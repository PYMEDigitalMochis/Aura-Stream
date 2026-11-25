package mx.detrasdelboton.aura;
import java.util.concurrent.atomic.AtomicLong;
public final class TestVisualDeadline {
  private static int tests;
  static void ok(boolean ok, String msg){tests++; if(!ok)throw new AssertionError(msg);}
  static int[] frame(){int[] b=new int[80*45]; java.util.Arrays.fill(b,0xff505050); return b;}
  public static void main(String[] args) throws Exception {
    AtomicLong clock=new AtomicLong(1000);
    VideoFrameDetector.Result fine=VideoFrameDetector.inspect(6, t->frame(),clock::get);
    ok("completed".equals(fine.status),"control clock passes");
    clock.set(1000);
    VideoFrameDetector.Result late=VideoFrameDetector.inspect(6,t->{clock.set(1000+VideoFrameDetector.MAX_NANOS+1);return frame();},clock::get);
    ok("failed".equals(late.status),"late decoder not completed");
    ok(late.findings.isEmpty(),"late decoder discards findings");
    clock.set(1000);
    VideoFrameDetector.Result lateLast=VideoFrameDetector.inspect(6,t->{if(t>=5500000L)clock.set(1000+VideoFrameDetector.MAX_NANOS+1);return frame();},clock::get);
    ok("failed".equals(lateLast.status),"last sample timeout checked");
    Thread.currentThread().interrupt();
    try{VideoFrameDetector.inspect(6,t->frame(),clock::get);throw new AssertionError("interrupted thread accepted");}
    catch(InterruptedException good){ok(true,"interruption propagates");}
    finally{Thread.interrupted();}
    clock.set(1000);
    VideoFrameDetector.Result nullClock=VideoFrameDetector.inspect(6,t->frame(),null);
    ok("failed".equals(nullClock.status),"null clock fails");
    System.out.println("PASS "+tests+" deadline/cancellation assertions");
  }
}
