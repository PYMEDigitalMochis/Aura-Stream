package mx.detrasdelboton.aura;

import android.content.ContentResolver;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** Android MediaCodec decoding using read-only SAF descriptors. No FFmpeg required.
 * A completed check means this selected track reached the decoder's EOS; it does
 * not certify editorial quality, rights, individual visual defects or every codec.
 */
final class AndroidCodecAuditor {
    static final long MAX_DECODE_NANOS=240_000_000_000L;
    static final long MAX_BUFFERS=500_000;
    static final long MAX_TOTAL_NANOS=300_000_000_000L;
    static final long MAX_PIXELS=1920L*1080;
    private AndroidCodecAuditor(){}
    static CodecAuditResult inspect(ContentResolver resolver,Uri uri,List<ReportContract.Track> tracks,double duration) throws InterruptedException {
        ArrayList<CodecAuditResult.TrackResult> list=new ArrayList<>();
        boolean decodeOK=true,audioOK=true,hasAudio=false,hadTimeout=false;
        StringBuilder reasons=new StringBuilder();
        final long overallStart=System.nanoTime();
        for(ReportContract.Track track:tracks){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Decodificación cancelada");
            boolean audio=track.mime.startsWith("audio/");
            if(System.nanoTime()-overallStart>MAX_TOTAL_NANOS){
                decodeOK=false;hadTimeout=true;
                if(reasons.length()>0)reasons.append("; ");
                reasons.append("pista ").append(track.index).append(" omitida: tiempo total agotado");
                if(audio){hasAudio=true;audioOK=false;}
                list.add(new CodecAuditResult.TrackResult(track.index,audio,"timeout","Tiempo total agotado; pista no ejecutada",0,null));
                continue;
            }
            if(audio)hasAudio=true;
            CodecAuditResult.TrackResult t;
            try{t=decodeOne(resolver,uri,track,duration,overallStart);}
            catch(InterruptedException e){throw e;}
            catch(Exception | LinkageError e){
                t=new CodecAuditResult.TrackResult(track.index,audio,"failed","Error del códec/pista " +track.index+ ": "+e.getClass().getSimpleName(),0,null);
            }
            list.add(t);
            if(!"completed".equals(t.status)){
                decodeOK=false;
                if("timeout".equals(t.status))hadTimeout=true;
                if(audio)audioOK=false;
                if(reasons.length()>0)reasons.append("; ");
                reasons.append("pista ").append(t.index).append(' ').append(t.reason);
            }
        }
        return new CodecAuditResult(decodeOK?"completed":hadTimeout?"timeout":"failed",!hasAudio?"not_applicable":audioOK?"completed":hadTimeout?"timeout":"failed",
                decodeOK?"Decodificación hasta EOS de cada pista; no garantiza ausencia de defectos visuales":reasons.toString(),list);
    }
    private static CodecAuditResult.TrackResult decodeOne(ContentResolver resolver,Uri uri,ReportContract.Track track,double declaredDuration,long overallStart) throws Exception {
        boolean audio=track.mime.startsWith("audio/");
        // Avoid raw 4K/8K MediaCodec buffers overwhelming low-RAM Android
        // devices. Unsupported coverage is explicit rather than an OOM crash.
        if(!audio && (track.width<=0 || track.height<=0 ||
                (long)track.width*track.height>MAX_PIXELS))
            return new CodecAuditResult.TrackResult(track.index,false,"not_run",
                    "Pista visual fuera del presupuesto de memoria <=1920x1080",0,null);
        try(ParcelFileDescriptor pfd=resolver.openFileDescriptor(uri,"r")){
            if(pfd==null)throw new IllegalStateException("Proveedor SAF sin descriptor de lectura");
            MediaExtractor extractor=new MediaExtractor();
            try{
                extractor.setDataSource(pfd.getFileDescriptor());
                if(track.index<0||track.index>=extractor.getTrackCount())throw new IllegalArgumentException("Índice de pista inválido");
                MediaFormat inputFormat=extractor.getTrackFormat(track.index);
                if(!track.mime.equals(inputFormat.getString(MediaFormat.KEY_MIME)))
                    throw new IllegalArgumentException("MIME cambió entre lecturas del mismo archivo");
                extractor.selectTrack(track.index);
                // KEY_DURATION is per track on supported extractors. The video
                // may legitimately be longer than its audio track; compare
                // PCM sample coverage with the AUDIO duration when available.
                double expectedAudioSeconds=declaredDuration;
                if(audio && inputFormat.containsKey(MediaFormat.KEY_DURATION)){
                    long trackUs=inputFormat.getLong(MediaFormat.KEY_DURATION);
                    if(trackUs>0 && trackUs<=((long)(declaredDuration*1_000_000)+1_000_000L))
                        expectedAudioSeconds=trackUs/1_000_000.0;
                }
                MediaCodec codec=MediaCodec.createDecoderByType(track.mime);
                boolean started=false;
                try{
                    codec.configure(inputFormat,null,null,0);
                    codec.start();started=true;
                    AudioSignalDetector meter=null;
                    if(audio)meter=makeMeter(codec.getOutputFormat(),track);
                    long count=0,steps=0;
                    boolean inputEOS=false,outputEOS=false;
                    final long start=System.nanoTime();
                    MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
                    while(!outputEOS){
                        if(Thread.currentThread().isInterrupted())throw new InterruptedException("Decodificación cancelada");
                        if(System.nanoTime()-start>MAX_DECODE_NANOS||System.nanoTime()-overallStart>MAX_TOTAL_NANOS||steps++>MAX_BUFFERS)
                            return new CodecAuditResult.TrackResult(track.index,audio,"timeout","Límite de tiempo o buffers",count,null);
                        if(!inputEOS){
                            int ix=codec.dequeueInputBuffer(10_000);
                            if(ix>=0){
                                ByteBuffer in=codec.getInputBuffer(ix);
                                if(in==null)throw new IllegalStateException("Buffer de entrada no disponible");
                                in.clear();
                                int size=extractor.readSampleData(in,0);
                                if(size<0){
                                    codec.queueInputBuffer(ix,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputEOS=true;
                                } else {
                                    if((extractor.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)
                                        throw new IllegalArgumentException("Pista DRM cifrada no analizable");
                                    long pts=extractor.getSampleTime();
                                    if(pts<0)throw new IllegalArgumentException("PTS negativo");
                                    int sampleFlags=extractor.getSampleFlags();
                                    int inputFlags=(sampleFlags&MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME)!=0
                                        ?MediaCodec.BUFFER_FLAG_PARTIAL_FRAME:0;
                                    codec.queueInputBuffer(ix,0,size,pts,inputFlags);
                                    extractor.advance();
                                }
                            }
                        }
                        int ix=codec.dequeueOutputBuffer(info,10_000);
                        if(ix>=0){
                            try{
                                if(info.size>0){
                                    count++;
                                    if(audio){
                                        ByteBuffer out=codec.getOutputBuffer(ix);
                                        if(out==null)throw new IllegalStateException("PCM no disponible");
                                        if(info.offset<0||info.size<0||info.offset>out.capacity()-info.size)
                                            throw new IllegalStateException("Límites PCM inválidos");
                                        out.position(info.offset);
                                        out.limit(info.offset+info.size);
                                        meter.ingest(out.slice(),info.presentationTimeUs);
                                    }
                                }
                                if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)outputEOS=true;
                            }finally{codec.releaseOutputBuffer(ix,false);}
                        }else if(ix==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                            if(audio){
                                if(meter!=null && count>0)throw new IllegalStateException("Formato PCM cambió durante el audio");
                                meter=makeMeter(codec.getOutputFormat(),track);
                            }
                        }else if(ix!=MediaCodec.INFO_TRY_AGAIN_LATER && ix!=MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                            throw new IllegalStateException("Resultado MediaCodec desconocido: "+ix);
                        }
                    }
                    if(!inputEOS)throw new IllegalStateException("Códec emitió EOS antes de terminar de introducir el archivo");
                    if(count==0)throw new IllegalStateException("EOS sin fotogramas PCM/video decodificados");
                    AudioSignalDetector.Result levels=audio?meter.finish(expectedAudioSeconds):null;
                    return new CodecAuditResult.TrackResult(track.index,audio,"completed","Salida hasta EOS",count,levels);
                }finally{
                    try{if(started)codec.stop();}finally{codec.release();}
                }
            }finally{extractor.release();}
        }
    }
    private static AudioSignalDetector makeMeter(MediaFormat fmt,ReportContract.Track track){
        int encoding=fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)?fmt.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;
        if(encoding!=AudioFormat.ENCODING_PCM_16BIT)
            throw new IllegalArgumentException("PCM no 16 bit; no asumir sonoridad");
        int rate=fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)?fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE):track.sampleRate;
        int channels=fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)?fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT):track.channels;
        return new AudioSignalDetector(rate,channels);
    }
}
