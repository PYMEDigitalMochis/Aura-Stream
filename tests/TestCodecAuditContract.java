package mx.detrasdelboton.aura;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
public final class TestCodecAuditContract {
    public static void main(String[] args){
        AudioSignalDetector a=new AudioSignalDetector(16000,1);
        ByteBuffer b=ByteBuffer.allocate(32000).order(ByteOrder.nativeOrder());
        a.ingest(b,0);
        AudioSignalDetector.Result measurements=a.finish(1);
        CodecAuditResult codecs=new CodecAuditResult("completed","completed","EOS comprobado",Arrays.asList(
            new CodecAuditResult.TrackResult(0,false,"completed","EOS",10,null),
            new CodecAuditResult.TrackResult(1,true,"completed","EOS",20,measurements)));
        VideoFrameDetector.Result visual=new VideoFrameDetector.Result("completed","1 fps",5,Collections.emptyList());
        String result=ReportContract.build("test.mp4","a".repeat(64),1234,1.0,Arrays.asList(
            new ReportContract.Track(0,"video/avc",640,360,0,0),
            new ReportContract.Track(1,"audio/mp4a-latm",0,0,1,16000)),true,visual,codecs);
        System.out.println(result);
    }
}
