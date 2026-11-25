package mx.detrasdelboton.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.io.InputStream;
import java.io.IOException;

/** Platform-independent aura.audit/1 PRELIMINARY report serialization.
 *  Native visual/codec sampling is candidate-only; editorial review is mandatory.
 */
public final class ReportContract {
    private ReportContract() {}
    public static final class Track {
        public final int index;
        public final String mime;
        public final int width, height, channels, sampleRate;
        public Track(int index, String mime, int width, int height, int channels, int sampleRate) {
            this.index=index; this.mime=mime; this.width=width; this.height=height;
            this.channels=channels; this.sampleRate=sampleRate;
        }
    }
    /** Verify a newly written JSON export byte for byte after closing the stream.
     * A cloud/content provider may ignore truncation or return an incomplete file.
     */
    public static void verifyExactBytes(InputStream in, byte[] expected) throws IOException {
        if(in==null || expected==null)throw new IllegalArgumentException("Verificación de exportación sin datos");
        byte[] chunk=new byte[4096];int n,pos=0;
        while((n=in.read(chunk))!=-1){
            if(n==0)throw new IOException("Lectura de verificación sin progreso");
            for(int i=0;i<n;i++){
                if(pos>=expected.length || chunk[i]!=expected[pos++])
                    throw new IOException("El contenido exportado no coincide con el informe");
            }
        }
        if(pos!=expected.length)throw new IOException("Exportación incompleta");
    }
    private static String quote(String s) {
        if (s==null) s="";
        StringBuilder out=new StringBuilder("\"");
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i+1>=s.length() || !Character.isLowSurrogate(s.charAt(i+1)))
                    throw new IllegalArgumentException("Texto Unicode inválido");
                out.append(c).append(s.charAt(++i));
                continue;
            }
            if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Texto Unicode inválido");
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if(c<32) out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));
                    else out.append(c);
            }
        }
        return out.append('"').toString();
    }
    public static String build(String name, String sha, long bytes, double duration,
                               List<Track> tracks, boolean unchanged) {
        return build(name,sha,bytes,duration,tracks,unchanged,null,null);
    }
    public static String build(String name, String sha, long bytes, double duration,
                               List<Track> tracks, boolean unchanged, VideoFrameDetector.Result videoResult) {
        return build(name,sha,bytes,duration,tracks,unchanged,videoResult,null);
    }
    public static String build(String name, String sha, long bytes, double duration,
                               List<Track> tracks, boolean unchanged,
                               VideoFrameDetector.Result videoResult,CodecAuditResult codecs) {
        if (name==null || name.isEmpty() || name.length()>255) throw new IllegalArgumentException("Nombre inválido");
        if (sha==null || !sha.matches("(?i)[0-9a-f]{64}")) throw new IllegalArgumentException("SHA-256 inválido");
        if (bytes<=0 || duration<=0 || !Double.isFinite(duration) || duration>43200)
            throw new IllegalArgumentException("Tamaño/duración fuera de rango");
        if (tracks==null || tracks.isEmpty() || tracks.size()>32) throw new IllegalArgumentException("Pistas no válidas");
        if (!unchanged) throw new IllegalArgumentException("Origen no comprobado");
        boolean video=false, audio=false;
        Set<Integer> trackIds=new HashSet<>();
        StringBuilder out=new StringBuilder(2300);
        out.append("{\"schema\":\"aura.audit/1\",\"engine\":\"android-native-audit-poc/0.1.8\",\"source\":{");
        out.append("\"name\":").append(quote(name)).append(",\"sha256\":").append(quote(sha.toLowerCase(Locale.ROOT)))
           .append(",\"bytes\":").append(bytes).append(",\"duration_s\":").append(Double.toString(duration)).append(",\"streams\":[");
        for (int i=0;i<tracks.size();i++) {
            Track t=tracks.get(i);
            if (t.index<0 || t.index>255 || !trackIds.add(t.index))
                throw new IllegalArgumentException("Índice de pista duplicado o fuera de rango");
            if (t.mime==null || !t.mime.matches("(?:video|audio)/[a-zA-Z0-9][a-zA-Z0-9.+_-]{0,63}"))
                throw new IllegalArgumentException("MIME de pista inválido");
            if (t.width<0 || t.width>16384 || t.height<0 || t.height>16384 ||
                t.channels<0 || t.channels>32 || t.sampleRate<0 || t.sampleRate>384000)
                throw new IllegalArgumentException("Metadatos de pista fuera de rango");
            String type=t.mime.startsWith("video/")?"video":t.mime.startsWith("audio/")?"audio":null;
            if(type==null)throw new IllegalArgumentException("Tipo de pista desconocido");
            if (type.equals("video")) video=true; else audio=true;
            if(i>0)out.append(',');
            out.append("{\"index\":").append(t.index).append(",\"codec_type\":").append(quote(type))
               .append(",\"codec_name\":").append(quote(t.mime));
            if(type.equals("video")) out.append(",\"width\":").append(Math.max(0,t.width))
                 .append(",\"height\":").append(Math.max(0,t.height));
            else out.append(",\"channels\":").append(Math.max(0,t.channels))
                .append(",\"sample_rate\":").append(quote(Integer.toString(Math.max(0,t.sampleRate))));
            out.append('}');
        }
        if(!video) throw new IllegalArgumentException("No hay pista de video");
        out.append("]},\"checks\":{");
        String[] kinds={"decode","black","freeze","silence","loudness"};
        for (int i=0;i<kinds.length;i++) {
            String kind=kinds[i];
            boolean applicable=i==0?(video||audio):(i<3?video:audio);
            if (i>0)out.append(',');
            boolean visual=videoResult!=null && (kind.equals("black") || kind.equals("freeze"));
            boolean codec=codecs!=null && (kind.equals("decode") || kind.equals("silence") || kind.equals("loudness"));
            String status=!applicable?"not_applicable":visual?videoResult.status:codec?
                    (kind.equals("decode")?codecs.decodeStatus:codecs.audioStatus):"not_run";
            String reason=!applicable?"Pista no presente":visual?videoResult.reason:codec?
                    (kind.equals("decode")?codecs.reason:
                        "PCM 16-bit: silencio RMS < -48 dBFS, sonoridad RMS/peak por muestra (NO LUFS/true peak); "+codecs.reason):
                    "Detector no ejecutado en este prototipo";
            out.append(quote(kind)).append(":{\"status\":").append(quote(status))
               .append(",\"reason\":").append(quote(reason)).append('}');
        }
        out.append("},\"findings\":[");
        if (videoResult!=null && "completed".equals(videoResult.status)) {
            int id=0;
            for(VideoFrameDetector.Finding f:videoResult.findings) {
                if (!((f.kind.equals("black")||f.kind.equals("freeze")) &&
                      Double.isFinite(f.start)&&Double.isFinite(f.end) &&
                      f.start>=0 && f.end>=f.start && f.end<=duration))
                    throw new IllegalArgumentException("Hallazgo visual inválido");
                if (id>=2000)throw new IllegalArgumentException("Demasiados hallazgos visuales");
                if (id>0)out.append(',');
                out.append("{\"id\":\"frame_").append(++id)
                   .append("\",\"kind\":").append(quote(f.kind))
                   .append(",\"start_s\":").append(f.start)
                   .append(",\"end_s\":").append(f.end)
                   .append(",\"severity\":\"review\",\"evidence\":\"Muestreo visual limitado hasta 120 muestras; confirmar visualmente\"}");
            }
        }
        int id=videoResult!=null && "completed".equals(videoResult.status)?videoResult.findings.size():0;
        if(codecs!=null){
            for(CodecAuditResult.TrackResult track:codecs.tracks){
                if(!track.audio || !"completed".equals(track.status) || track.levels==null)continue;
                for(AudioSignalDetector.Finding f:track.levels.silences){
                    if(f.start>=duration)continue;
                    if(id>=2000)throw new IllegalArgumentException("Demasiados hallazgos nativos");
                    if(id>0)out.append(',');
                    out.append("{\"id\":\"audio_").append(track.index).append('_').append(++id)
                       .append("\",\"kind\":\"silence\",\"start_s\":").append(f.start)
                       .append(",\"end_s\":").append(Math.min(duration,f.end))
                       .append(",\"severity\":\"review\",\"evidence\":\"PCM16 RMS < -48 dBFS; comprobar con escucha\"}");
                }
            }
        }
        out.append("],\"metrics\":{\"decoder_tracks\":[");
        if(codecs!=null){
            int n=0;
            for(CodecAuditResult.TrackResult track:codecs.tracks){
                if(n++>0)out.append(',');
                out.append("{\"index\":").append(track.index)
                   .append(",\"codec_status\":").append(quote(track.status))
                   .append(",\"decoded_outputs\":").append(track.outputs).append('}');
            }
        }
        out.append("],\"audio_tracks\":[");
        int metrics=0;
        if(codecs!=null)for(CodecAuditResult.TrackResult track:codecs.tracks){
            if(!track.audio || track.levels==null || !"completed".equals(track.status))continue;
            if(metrics++>0)out.append(',');
            out.append("{\"index\":").append(track.index).append(",\"rms_dbfs\":")
                .append(track.levels.rmsDbfs).append(",\"sample_peak_dbfs\":")
                .append(track.levels.peakDbfs).append(",\"coverage_s\":")
                .append(track.levels.coveredSeconds)
                .append(",\"method\":\"PCM16 unweighted RMS; not LUFS or true peak\"}");
        }
        out.append("]},\"not_evaluated\":[");
        StringBuilder pending=new StringBuilder();
        if(codecs==null || !"completed".equals(codecs.decodeStatus))pending.append("\"decode\",");
        if(videoResult==null || !"completed".equals(videoResult.status))pending.append("\"black\",\"freeze\",");
        if(audio && (codecs==null || !"completed".equals(codecs.audioStatus)))pending.append("\"silence\",\"loudness\",");
        out.append(pending).append("\"speech_transcription\",\"script_alignment\",\"narrative\",\"seo\",\"rights\"],");
        boolean visualOK=videoResult!=null && "completed".equals(videoResult.status);
        boolean decodeOK=codecs!=null && "completed".equals(codecs.decodeStatus);
        boolean audioOK=!audio || (codecs!=null && "completed".equals(codecs.audioStatus));
        out.append("\"technical_status\":").append(quote(visualOK && decodeOK && audioOK?"completed":"incomplete"))
          .append(",\"editorial_status\":\"PENDING_HUMAN_REVIEW\",\"source_unchanged\":true}");
        return out.toString();
    }
}
