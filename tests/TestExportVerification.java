import java.io.*;
import java.nio.charset.StandardCharsets;
import mx.detrasdelboton.aura.ReportContract;
public class TestExportVerification {
 private static int checked;
 private static void verify(byte[] actual,byte[] expected, boolean ok){
  try {
   ReportContract.verifyExactBytes(new ByteArrayInputStream(actual),expected);
   if(!ok)throw new AssertionError("Bytes inválidos aceptados");
  }catch(IOException e){if(ok)throw new AssertionError("Bytes correctos rechazados",e);}
  checked++;
 }
 public static void main(String[] args) throws Exception {
  byte[] correct="{\"schema\":\"aura.audit/1\"}".getBytes(StandardCharsets.UTF_8);
  verify(correct,correct,true);
  verify((new String(correct,StandardCharsets.UTF_8)+"X").getBytes(StandardCharsets.UTF_8),correct,false);
  verify(java.util.Arrays.copyOf(correct,correct.length-1),correct,false);
  byte[] changed=correct.clone();changed[1]='z';verify(changed,correct,false);
  boolean zeroRejected=false;
  try {ReportContract.verifyExactBytes(new InputStream(){public int read(byte[] b){return 0;}public int read(){return 0;}},correct);}
  catch(IOException expected){zeroRejected=true;}
  if(!zeroRejected)throw new AssertionError("No-progress stream accepted");
  checked++;
  System.out.println("OK: export bytes correct, extra, truncated, tampered, zero-progress: "+checked+"/"+checked);
 }
}
