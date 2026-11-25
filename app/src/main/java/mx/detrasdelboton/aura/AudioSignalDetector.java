package mx.detrasdelboton.aura;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Streaming PCM16 meter. RMS dBFS is NOT EBU-R128 LUFS or true peak.
 * All windows and silence intervals are candidates; no speech intelligence.
 * Pure-Java core can be tested independently of Android.
 */
final class AudioSignalDetector {
    static final double SILENCE_RMS_DBFS=-48.0;
    static final double MIN_SILENCE_SECONDS=0.60;
    static final int MAX_FINDINGS=900;
    static final class Finding {
        final double start, end;
        Finding(double start, double end){this.start=start;this.end=end;}
    }
    static final class Result {
        final List<Finding> silences;
        final double rmsDbfs, peakDbfs, coveredSeconds;
        final long frames;
        Result(List<Finding> found, double rms, double peak,long frames,double seconds){
            this.silences=Collections.unmodifiableList(new ArrayList<>(found));
            this.rmsDbfs=rms;this.peakDbfs=peak;this.frames=frames;this.coveredSeconds=seconds;
        }
    }
    private final int sampleRate, channels, windowFrames;
    private long frames, windowCount;
    private double power, windowPower, peak, coveredEnd=-1, coveredStart=-1, windowStart;
    private double runStart=-1, runEnd=-1;
    private final List<Finding> silences=new ArrayList<>();
    AudioSignalDetector(int sampleRate,int channels){
        if(sampleRate<8000||sampleRate>384000||channels<1||channels>32)throw new IllegalArgumentException("Formato PCM inválido");
        this.sampleRate=sampleRate;this.channels=channels;this.windowFrames=Math.max(1,sampleRate/10);
    }
    /** input is already decoded PCM16 in native endian; position/limit are not modified. */
    void ingest(ByteBuffer pcm,long presentationUs){
        if(pcm==null||presentationUs<0)throw new IllegalArgumentException("PCM inválido");
        ByteBuffer b=pcm.duplicate().order(ByteOrder.nativeOrder());
        int frameBytes=channels*2;
        if(b.remaining()%frameBytes!=0)throw new IllegalArgumentException("PCM truncado");
        // Empty codec output must not change the coverage timeline or bridge a gap.
        // Some decoders emit informational zero-byte buffers at arbitrary PTS.
        if(!b.hasRemaining())return;
        double start=presentationUs/1_000_000.0;
        if(coveredStart<0)coveredStart=start;
        if(coveredEnd>=0 && Math.abs(coveredEnd-start)>.25)
            throw new IllegalArgumentException("Discontinuidad de marcas PTS en audio PCM");
        int currentFrame=0;
        while(b.remaining()>0){
            if(windowCount==0)windowStart=start+(double)currentFrame/sampleRate;
            double framePower=0;
            for(int c=0;c<channels;c++){
                double s=b.getShort()/32768.0;
                framePower+=s*s;
                peak=Math.max(peak,Math.abs(s));
            }
            framePower/=channels;
            power+=framePower;windowPower+=framePower;frames++;windowCount++;currentFrame++;
            if(windowCount>=windowFrames){
                finishWindow(start+(double)currentFrame/sampleRate);
            }
        }
        coveredEnd=start+(double)currentFrame/sampleRate;
    }
    private void finishWindow(double end){
        if(windowCount==0)return;
        double db=toDb(Math.sqrt(windowPower/windowCount));
        if(db<SILENCE_RMS_DBFS){
            if(runStart<0)runStart=windowStart;
            runEnd=end;
        } else flushRun();
        windowCount=0;windowPower=0;
    }
    private void flushRun(){
        // PTS has microsecond resolution. Allow one microsecond of numerical
        // rounding error at the inclusive 600 ms threshold.
        if(runStart>=0&&runEnd-runStart+0.000001>=MIN_SILENCE_SECONDS){
            if(silences.size()>=MAX_FINDINGS)throw new IllegalArgumentException("Exceso de intervalos de silencio");
            silences.add(new Finding(Math.max(0,runStart),runEnd));
        }
        runStart=-1;runEnd=-1;
    }
    Result finish(double declaredDuration){
        if(frames==0||!Double.isFinite(declaredDuration)||declaredDuration<=0)
            throw new IllegalArgumentException("PCM vacío o duración inválida");
        if(windowCount>0)finishWindow(coveredEnd);
        flushRun();
        double seconds=(double)frames/sampleRate;
        // Decoder priming/trailing padding can shift the count slightly. Never call
        // an incomplete stream complete when the observed PCM omits most of it.
        // Compare against this audio track's expected duration, not the length
        // of the video. Legitimate videos can have shorter audio tracks.
        // At most one second of padding/timeline discrepancy is tolerated;
        // 85% coverage falsely labelled large, incomplete tracks completed.
        double tolerance=Math.max(.25,Math.min(1.0,declaredDuration*.002));
        if(seconds+tolerance<declaredDuration)
            throw new IllegalArgumentException("PCM insuficiente para cubrir la pista de audio");
        // Many individually small PTS gaps can accumulate: they are not
        // decoded PCM and must not become invisible in the total coverage.
        double timelineSeconds=coveredEnd-coveredStart;
        if(Math.abs(timelineSeconds-seconds)>Math.max(.25,seconds*.005))
            throw new IllegalArgumentException("Discontinuidades acumuladas en marcas PTS PCM");
        return new Result(silences,toDb(Math.sqrt(power/frames)),toDb(peak),frames,seconds);
    }
    private static double toDb(double linear){return linear<=0?-120.0:20*Math.log10(linear);}
}
