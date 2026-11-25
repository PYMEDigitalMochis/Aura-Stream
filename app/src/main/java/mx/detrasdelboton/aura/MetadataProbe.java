package mx.detrasdelboton.aura;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Read-only SAF metadata survey. Native codec decoding is attempted, but no device QA is claimed. */
final class MetadataProbe {
    private static final long MAX_BYTES=512L*1024*1024;
    private MetadataProbe() {}
    static final class Result {
        final String report, sha, name;
        final long bytes;
        final double duration;
        final int trackCount;
        final String visualStatus, visualReason;
        final int visualCandidateCount;
        final String decodeStatus,audioStatus,audioSummary;
        Result(String r,String s,String n,long b,double d,int t,String v,String why,int candidates,String decode,String audio,String summary){
            report=r;sha=s;name=n;bytes=b;duration=d;trackCount=t;
            visualStatus=v;visualReason=why;visualCandidateCount=candidates;
            decodeStatus=decode;audioStatus=audio;audioSummary=summary;
        }
    }
    static Result inspect(Context context,Uri uri) throws Exception {
        if(uri==null || !"content".equals(uri.getScheme()))
            throw new IllegalArgumentException("Se requiere documento de solo lectura del selector Android");
        ContentResolver resolver=context.getContentResolver();
        String name="video.mp4";
        long declared=-1;
        try(Cursor cursor=resolver.query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){
                int ni=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME), si=cursor.getColumnIndex(OpenableColumns.SIZE);
                if(ni>=0&&!cursor.isNull(ni)) name=cursor.getString(ni);
                if(si>=0&&!cursor.isNull(si)) declared=cursor.getLong(si);
            }
        }
        if(name==null || name.isEmpty() || name.length()>255)throw new IllegalArgumentException("Nombre de archivo no válido");
        if(declared>MAX_BYTES)throw new IllegalArgumentException("POC limitado a videos de 512 MiB");
        Digested before=digest(resolver,uri);
        if(before.bytes==0)throw new IllegalArgumentException("El archivo está vacío");
        if(declared>=0 && declared!=before.bytes)throw new IllegalArgumentException("El tamaño declarado no coincide con bytes leídos");
        List<ReportContract.Track> tracks=new ArrayList<>();
        long durationMs;
        try(ParcelFileDescriptor pfd=resolver.openFileDescriptor(uri,"r")) {
            if(pfd==null)throw new IllegalArgumentException("No hay descriptor del video");
            MediaExtractor extractor=new MediaExtractor();
            try {
                extractor.setDataSource(pfd.getFileDescriptor());
                int total=extractor.getTrackCount();
                if(total>32)throw new IllegalArgumentException("Demasiadas pistas para el POC");
                for (int i=0;i<total;i++) {
                    MediaFormat f=extractor.getTrackFormat(i);
                    String mime=f.getString(MediaFormat.KEY_MIME);
                    if(mime==null || !(mime.startsWith("video/")||mime.startsWith("audio/")))continue;
                    tracks.add(new ReportContract.Track(i,mime, num(f,MediaFormat.KEY_WIDTH),num(f,MediaFormat.KEY_HEIGHT),
                                                       num(f,MediaFormat.KEY_CHANNEL_COUNT),num(f,MediaFormat.KEY_SAMPLE_RATE)));
                }
            } finally { extractor.release(); }
        }
        if(tracks.stream().noneMatch(t->t.mime.startsWith("video/")))
            throw new IllegalArgumentException("No se detectó una pista de video; informe no generado");
        try (ParcelFileDescriptor pfd=resolver.openFileDescriptor(uri,"r")) {
            if(pfd==null)throw new IllegalArgumentException("Descriptor no disponible");
            MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            try {
                retriever.setDataSource(pfd.getFileDescriptor());
                String raw=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if(raw==null)throw new IllegalArgumentException("Duración no disponible");
                durationMs=Long.parseLong(raw);
            } finally { retriever.release(); }
        }
        if(durationMs<=0)throw new IllegalArgumentException("Duración desconocida o inválida");
        VideoFrameDetector.Result visual;
        try { visual=AndroidFrameSampler.inspect(resolver,uri,durationMs/1000.0); }
        catch(InterruptedException e){throw e;}
        catch(Exception | LinkageError e){visual=VideoFrameDetector.failed("Decodificación visual no disponible en este proveedor",0);}
        CodecAuditResult codec=AndroidCodecAuditor.inspect(resolver,uri,tracks,durationMs/1000.0);
        Digested after=digest(resolver,uri);
        if(Thread.currentThread().isInterrupted())throw new InterruptedException("Análisis interrumpido");
        if(after.bytes!=before.bytes || !MessageDigest.isEqual(before.hash,after.hash))
            throw new IllegalArgumentException("La fuente cambió durante el análisis, no se generó reporte");
        String sha=hex(before.hash),report=ReportContract.build(name,sha,before.bytes,durationMs/1000.0,tracks,true,visual,codec);
        StringBuilder summary=new StringBuilder();
        for(CodecAuditResult.TrackResult track:codec.tracks){
            if(track.audio){
                if(track.levels!=null){
                    summary.append("\nAudio pista ").append(track.index)
                           .append(": RMS ").append(String.format(java.util.Locale.ROOT,"%.1f",track.levels.rmsDbfs))
                           .append(" dBFS; pico muestra ").append(String.format(java.util.Locale.ROOT,"%.1f",track.levels.peakDbfs))
                           .append(" dBFS; silencios ").append(track.levels.silences.size());
                }else summary.append("\nAudio pista ").append(track.index).append(": ").append(track.status);
            }
        }
        return new Result(report,sha,name,before.bytes,durationMs/1000.0,tracks.size(),visual.status,visual.reason,visual.findings.size(),codec.decodeStatus,codec.audioStatus,summary.toString());
    }
    private static int num(MediaFormat f,String k){return f.containsKey(k)?f.getInteger(k):0;}
    private static final class Digested {final byte[] hash; final long bytes;Digested(byte[] h,long b){hash=h;bytes=b;}}
    private static Digested digest(ContentResolver resolver,Uri uri) throws Exception {
        MessageDigest md=MessageDigest.getInstance("SHA-256");long count=0;
        try(InputStream in=resolver.openInputStream(uri)){
            if(in==null)throw new IllegalArgumentException("No se puede leer el documento");
            byte[] buf=new byte[1024*1024];int n;
            while((n=in.read(buf))!=-1){
                if(Thread.currentThread().isInterrupted())throw new InterruptedException("Análisis interrumpido");
                if(n==0)throw new IllegalArgumentException("El proveedor de documentos no avanzó en la lectura");
                count+=n;
                if(count>MAX_BYTES)throw new IllegalArgumentException("Archivo mayor de 512 MiB");
                md.update(buf,0,n);
            }
        }
        return new Digested(md.digest(),count);
    }
    private static String hex(byte[] h){StringBuilder s=new StringBuilder(h.length*2);for(byte b:h)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
