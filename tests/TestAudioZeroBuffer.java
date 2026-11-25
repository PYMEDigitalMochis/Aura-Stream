package mx.detrasdelboton.aura;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
public final class TestAudioZeroBuffer {
    private static ByteBuffer silence(int frames){return ByteBuffer.allocate(frames*4).order(ByteOrder.nativeOrder());}
    private static void check(boolean yes, String label){if(!yes)throw new AssertionError(label);}
    public static void main(String[] args){
        AudioSignalDetector a=new AudioSignalDetector(48000,2);
        a.ingest(silence(48000),0);
        a.ingest(ByteBuffer.allocate(0),10_000_000);
        a.ingest(silence(48000),1_000_000);
        AudioSignalDetector.Result r=a.finish(2);
        check(r.frames==96000,"zero-buffer counted as audio");
        check(r.silences.size()==1,"silent interval broken by empty sample");
        check(r.silences.get(0).start==0,"bad silence start");
        check(Math.abs(r.silences.get(0).end-2)<.001,"empty timestamp changed coverage");
        AudioSignalDetector b=new AudioSignalDetector(48000,2);
        b.ingest(ByteBuffer.allocate(0),50_000_000);
        b.ingest(silence(48000),0);
        check(Math.abs(b.finish(1).coveredSeconds-1)<.00001,"empty first sample changed coverage");
        System.out.println("PASS 5 empty PCM/PTS assertions");
    }
}
