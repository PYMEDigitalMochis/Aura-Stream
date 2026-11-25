package mx.detrasdelboton.aura;
import java.nio.*;
public class TestBoundaryExtended {
 static ByteBuffer pcm(int n,int val){ByteBuffer b=ByteBuffer.allocate(n*2).order(ByteOrder.nativeOrder());for(int i=0;i<n;i++)b.putShort((short)val);b.flip();return b;}
 public static void main(String[] x){int checks=0; for(int rate:new int[]{8000,10000,16000,48000})for(long t:new long[]{0,100000,900000,1200000,9900000,600000000,3599000000L})for(int ms:new int[]{500,600,700}){AudioSignalDetector a=new AudioSignalDetector(rate,1);int n=rate*ms/1000;a.ingest(pcm(n,0),t);a.ingest(pcm(rate-n,30000),t+ms*1000);AudioSignalDetector.Result r=a.finish(1);int expected=ms>=600?1:0;if(r.silences.size()!=expected)throw new AssertionError(rate+" "+t+" "+ms+" "+r.silences.size());checks++;} System.out.println("PASS "+checks+" offset/rate/duration checks");}
}
