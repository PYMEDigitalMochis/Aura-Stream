package mx.detrasdelboton.aura;
import java.util.*;
public final class TestVideoFrames {
    static int assertions=0;
    static void ok(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
    static int[] fill(int level){int[] a=new int[80*45];Arrays.fill(a,0xff000000 | (level<<16) | (level<<8) | level);return a;}
    static int[] sample(long timeUs){
        int n=(int)(timeUs/1000000);int l;
        if(n>=3&&n<=5)l=0;
        else if(n>=8&&n<=13)l=145;
        else l=50+(n*13)%180;
        return fill(l);
    }
    public static void main(String[] args) throws Exception {
        VideoFrameDetector.Result r=VideoFrameDetector.inspect(16,TestVideoFrames::sample);
        ok("completed".equals(r.status),"status");
        ok(r.sampledFrames==16,"exact samples");
        ok(r.findings.size()==2,"expected 2 candidates, got "+r.findings.size());
        ok("black".equals(r.findings.get(0).kind),"black kind");
        ok(r.findings.get(0).start==3 && r.findings.get(0).end==6,"black time");
        ok("freeze".equals(r.findings.get(1).kind),"freeze kind");
        ok(r.findings.get(1).start==8 && r.findings.get(1).end==14,"freeze time");
        VideoFrameDetector.Result clean=VideoFrameDetector.inspect(16,t->fill(50+((int)(t/1000000)*13)%180));
        ok(clean.findings.size()==0,"moving video false alert");
        VideoFrameDetector.Result longer=VideoFrameDetector.inspect(600,TestVideoFrames::sample);
        ok("completed".equals(longer.status)&&longer.sampledFrames<=120,"adaptive long videos must remain bounded");
        VideoFrameDetector.Result tooLong=VideoFrameDetector.inspect(3601,TestVideoFrames::sample);
        ok("not_run".equals(tooLong.status)&&tooLong.sampledFrames==0,"outside coverage must remain not_run");
        VideoFrameDetector.Result tooShort=VideoFrameDetector.inspect(2,TestVideoFrames::sample);
        ok("not_run".equals(tooShort.status),"shorter than coverage must be not_run");
        VideoFrameDetector.Result invalid=VideoFrameDetector.inspect(16,t->t>=4_500_000L?null:sample(t));
        ok("failed".equals(invalid.status)&&invalid.findings.isEmpty(),"missing frame must fail and discard partial findings");
        VideoFrameDetector.Result throwsError=VideoFrameDetector.inspect(10,t->{throw new IllegalArgumentException("corrupt frame");});
        ok("failed".equals(throwsError.status)&&throwsError.findings.isEmpty(),"provider failure must not complete");
        VideoFrameDetector.Result pureBlack=VideoFrameDetector.inspect(6,t->fill(0));
        ok(pureBlack.findings.size()==1&&"black".equals(pureBlack.findings.get(0).kind),"black should not also be freeze");
        VideoFrameDetector.Result shortFreeze=VideoFrameDetector.inspect(4,t->fill(200));
        ok(shortFreeze.findings.isEmpty(),"freeze below 5 samples is not reported");
        VideoFrameDetector.Result dark=VideoFrameDetector.inspect(6,t->fill(28));
        ok(dark.findings.stream().noneMatch(f->f.kind.equals("black")),"dark gray not black");
        Thread.currentThread().interrupt();
        try{VideoFrameDetector.inspect(10,TestVideoFrames::sample);throw new AssertionError("interruption ignored");}
        catch(InterruptedException expected){assertions++;}
        finally{Thread.interrupted();}
        List<ReportContract.Track> streams=Arrays.asList(new ReportContract.Track(0,"video/avc",1280,720,0,0));
        String report=ReportContract.build("prueba.mp4","a".repeat(64),1024,16,streams,true,r);
        ok(report.contains("\"status\":\"completed\""),"visual completed status serialized");
        ok(report.contains("\"kind\":\"black\""),"black finding serialized");
        ok(report.contains("\"kind\":\"freeze\""),"freeze finding serialized");
        ok(report.contains("\"technical_status\":\"incomplete\""),"no fake audit complete");
        ok(report.contains("\"status\":\"not_applicable\""),"no audio not applicable");
        String bad=ReportContract.build("prueba.mp4","a".repeat(64),1024,16,streams,true,invalid);
        ok(bad.contains("\"status\":\"failed\""),"failed status reported");
        ok(bad.contains("\"findings\":[]"),"no findings when visual detector failed");
        System.out.println(report);
        System.err.println("PASS: "+assertions+" adversarial assertions; visual detection is HEURISTIC ONLY / no Android SDK validation");
    }
}
