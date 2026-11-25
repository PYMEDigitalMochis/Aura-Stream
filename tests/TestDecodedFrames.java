package mx.detrasdelboton.aura;
import java.nio.file.Files;
import java.nio.file.Path;
public final class TestDecodedFrames {
    public static void main(String[] args) throws Exception {
        byte[] data=Files.readAllBytes(Path.of(args[0]));
        int frames=data.length/(80*45*3);
        if(frames!=15 || data.length!=frames*80*45*3)throw new AssertionError("Fixture size frames="+frames);
        VideoFrameDetector.Result actual=VideoFrameDetector.inspect(frames,t->{
            int frame=(int)(t/1000000);
            int[] result=new int[80*45]; int base=frame*80*45*3;
            for(int i=0;i<result.length;i++){
                int j=base+i*3;
                result[i]=0xff000000 | ((data[j]&255)<<16) | ((data[j+1]&255)<<8) | (data[j+2]&255);
            }
            return result;
        });
        if(!"completed".equals(actual.status))throw new AssertionError("Status="+actual.status);
        boolean black=false,freeze=false;
        for(VideoFrameDetector.Finding f:actual.findings){
            System.out.println(f.kind+" "+f.start+"-"+f.end);
            if(f.kind.equals("black") && f.start>=2 && f.start<=4 && f.end>=6 && f.end<=8)black=true;
            if(f.kind.equals("freeze") && f.start>=6 && f.start<=9 && f.end>=11 && f.end<=14)freeze=true;
        }
        if(!black||!freeze)throw new AssertionError("Muestreo no localizó ambos candidatos reales");
        System.out.println("PASS: FFmpeg decoded real synthetic MP4; detector found sustained black & frozen candidates. Not Android decoder QA.");
    }
}
