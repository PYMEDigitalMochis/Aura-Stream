package mx.detrasdelboton.aura;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
/** Repeatable property test: buffer chunking must not alter RMS/peak or timeline. */
public final class TestAudioProperty {
    static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
    public static void main(String[] argv){
        final int hz=10000, channels=2,frames=50_000;
        short[] source=new short[frames*channels];
        Random rnd=new Random(0xA0175EEDL);double pow=0,peak=0;
        for(int i=0;i<source.length;i++){
            short s=(short)(rnd.nextInt(40001)-20000);source[i]=s;
            double v=s/32768.0;pow+=v*v;peak=Math.max(peak,Math.abs(v));
        }
        double expectRms=20*Math.log10(Math.sqrt(pow/source.length));
        double expectPeak=20*Math.log10(peak);
        for(int seed=0;seed<20;seed++){
            AudioSignalDetector meter=new AudioSignalDetector(hz,channels);
            Random splits=new Random(seed+187);
            for(int k=0;k<frames;){
                int take=Math.min(frames-k,1+splits.nextInt(1279));
                ByteBuffer b=ByteBuffer.allocate(take*2*channels).order(ByteOrder.nativeOrder());
                for(int j=0;j<take*channels;j++)b.putShort(source[k*channels+j]);
                b.flip();meter.ingest(b,k*1_000_000L/hz);k+=take;
            }
            AudioSignalDetector.Result r=meter.finish(frames/(double)hz);
            check(Math.abs(r.rmsDbfs-expectRms)<.0000001,"RMS cambiado por fragmentación");
            check(Math.abs(r.peakDbfs-expectPeak)<.0000001,"pico cambiado por fragmentación");
            check(r.silences.isEmpty(),"falso silencio en audio aleatorio fuerte");
            check(r.frames==frames,"contador de frames mutado");
        }
        System.out.println("PASS 20 randomized streaming segmentations, RMS/peak/PTS/silence invariants");
    }
}
