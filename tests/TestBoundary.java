package mx.detrasdelboton.aura;
import java.nio.*;
public class TestBoundary {
 static ByteBuffer pcm(int n,int val){ByteBuffer b=ByteBuffer.allocate(n*2).order(ByteOrder.nativeOrder());for(int i=0;i<n;i++)b.putShort((short)val);b.flip();return b;}
 public static void main(String[] x){int fail=0; for(int t=0;t<100;t++){long us=t*100000L; AudioSignalDetector a=new AudioSignalDetector(10000,1);a.ingest(pcm(6000,0),us);a.ingest(pcm(4000,2000),us+600000);int count=a.finish(1).silences.size();if(count!=1){fail++;System.out.println("FAIL offset="+us+" exact 600ms count="+count);}}System.out.println("Failures "+fail+"/100");if(fail>0)System.exit(1);}
}
