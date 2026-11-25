import java.util.Arrays;
import java.util.Collections;
import mx.detrasdelboton.aura.ReportContract;
public class TestReportContract {
    private static int rejected;
    private static void shouldFail(Runnable r){
        try{r.run();throw new AssertionError("Invalid input accepted");}
        catch(IllegalArgumentException expected){rejected++;}
    }
    public static void main(String[] args){
        final String sha="a".repeat(64);
        final ReportContract.Track v=new ReportContract.Track(0,"video/avc",1280,720,0,0);
        final ReportContract.Track a=new ReportContract.Track(1,"audio/mp4a-latm",0,0,2,48000);
        shouldFail(()->ReportContract.build("x","bad",5,2,Arrays.asList(v,a),true));
        shouldFail(()->ReportContract.build("x",sha,5,2,Arrays.asList(v,a),false));
        shouldFail(()->ReportContract.build("x",sha,5,-1,Arrays.asList(v,a),true));
        shouldFail(()->ReportContract.build("x",sha,0,1,Arrays.asList(v,a),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Collections.emptyList(),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(v,v),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(a),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(new ReportContract.Track(-1,"video/avc",720,1280,0,0)),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(new ReportContract.Track(0,"video/avc\nattack",720,1280,0,0)),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(new ReportContract.Track(0,"video/avc",99999,720,0,0)),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(new ReportContract.Track(0,"video/avc",720,-1,0,0)),true));
        shouldFail(()->ReportContract.build("x\uD800",sha,5,1,Arrays.asList(v,a),true));
        shouldFail(()->ReportContract.build("x",sha,5,Double.NaN,Arrays.asList(v,a),true));
        shouldFail(()->ReportContract.build("x",sha,5,43201,Arrays.asList(v,a),true));
        shouldFail(()->ReportContract.build("x",sha,5,1,Arrays.asList(new ReportContract.Track(0,"video/avc",1280,720,0,0),new ReportContract.Track(1,"audio/mp4a-latm",0,0,2,999999)),true));
        if(rejected!=15)throw new AssertionError("Unexpected invalid count: "+rejected);
        System.err.println(rejected+" entradas adversariales rechazadas");
        System.out.println(ReportContract.build("Video \"prueba\" \\ñ.mp4",sha,1000000,10.5,Arrays.asList(v,a),true));
    }
}
