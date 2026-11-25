package mx.detrasdelboton.aura;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
public final class TestAudioAdversarial {
    static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static void reject(Runnable r){boolean yes=false;try{r.run();}catch(IllegalArgumentException expected){yes=true;}check(yes,"invalid accepted");}
    private static ByteBuffer constStereo(int frames,int left,int right){
        ByteBuffer b=ByteBuffer.allocate(4*frames).order(ByteOrder.nativeOrder());
        for(int i=0;i<frames;i++)b.putShort((short)left).putShort((short)right);
        b.flip();return b;
    }
    public static void main(String[] args){
        int hz=10000;
        AudioSignalDetector oneSide=new AudioSignalDetector(hz,2);
        oneSide.ingest(constStereo(hz,0,2000),0);
        check(oneSide.finish(1).silences.isEmpty(),"no falso silencio cuando canal derecho audible");
        AudioSignalDetector quiet=new AudioSignalDetector(hz,2);
        quiet.ingest(constStereo(hz,100,100),0);
        AudioSignalDetector.Result q=quiet.finish(1);
        check(q.silences.size()==1,"señal inferior a -48 dBFS debe marcarse");
        check(q.rmsDbfs>-51 && q.rmsDbfs<-49,"RMS de nivel cercano al umbral");
        AudioSignalDetector justAbove=new AudioSignalDetector(hz,2);
        justAbove.ingest(constStereo(hz,150,150),0);
        check(justAbove.finish(1).silences.isEmpty(),"nivel sobre umbral no marcarse");
        AudioSignalDetector shortGap=new AudioSignalDetector(hz,2);
        shortGap.ingest(constStereo(hz/2,0,0),0);
        shortGap.ingest(constStereo(hz/2,150,150),500_000);
        check(shortGap.finish(1).silences.isEmpty(),"silencio inferior a 600 ms no es hallazgo");
        AudioSignalDetector exactGap=new AudioSignalDetector(hz,2);
        exactGap.ingest(constStereo(6*hz/10,0,0),0);
        exactGap.ingest(constStereo(4*hz/10,150,150),600_000);
        check(exactGap.finish(1).silences.size()==1,"600 ms exactos cuenta como candidato");
        reject(()->new AudioSignalDetector(8001,33));
        reject(()->new AudioSignalDetector(384001,1));
        reject(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.ingest(null,0);});
        reject(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.ingest(constStereo(hz,20,20),-1);});
        reject(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.finish(1);});
        reject(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.ingest(constStereo(hz,20,20),0);a.finish(Double.NaN);});
        AudioSignalDetector m=new AudioSignalDetector(hz,2);
        m.ingest(constStereo(hz,0,0),0);
        AudioSignalDetector.Result z=m.finish(1);
        boolean immutable=false;
        try{z.silences.add(new AudioSignalDetector.Finding(0,1));}
        catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"lista de intervalos no debe ser mutable");
        check(z.peakDbfs==-120.0,"pico silencio");
        // Regression: the old 85% threshold approved a truly incomplete stream.
        reject(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);
            a.ingest(constStereo(9*hz,1000,1000),0);a.finish(10);});
        // Regression: audio tracks that legitimately end before the video
        // should be measured against their own duration, not the video length.
        AudioSignalDetector shortTrack=new AudioSignalDetector(hz,2);
        shortTrack.ingest(constStereo(2*hz,1000,1000),0);
        check(Math.abs(shortTrack.finish(2).coveredSeconds-2)<.01,
            "la pista de audio más corta es válida con su propia duración");
        // Regression: a sequence of small undetected PTS gaps must not
        // collectively be classified as complete PCM coverage.
        reject(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);
            for(int i=0;i<10;i++)a.ingest(constStereo(1000,1000,1000),i*200_000L);
            a.finish(1);});
        System.out.println("PASS "+checks+" adversarial PCM level assertions");
    }
}
