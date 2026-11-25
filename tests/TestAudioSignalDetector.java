package mx.detrasdelboton.aura;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
public final class TestAudioSignalDetector {
    private static int assertions;
    static void check(boolean pass,String why){assertions++;if(!pass)throw new AssertionError(why);}
    static void rejects(Runnable r){boolean rejected=false;try{r.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,"debió rechazar entrada");}
    static ByteBuffer tone(int sampleRate,int frames,double amplitude){
        ByteBuffer b=ByteBuffer.allocate(frames*4).order(ByteOrder.nativeOrder());
        for(int i=0;i<frames;i++){
            short s=(short)Math.round(32767.0*amplitude*Math.sin(2*Math.PI*440*i/sampleRate));
            b.putShort(s).putShort(s);
        }
        b.flip();return b;
    }
    static ByteBuffer silence(int frames){return ByteBuffer.allocate(frames*4).order(ByteOrder.nativeOrder());}
    public static void main(String[] args){
        final int hz=48000;
        AudioSignalDetector m=new AudioSignalDetector(hz,2);
        m.ingest(tone(hz,hz,.5),0);
        m.ingest(silence(hz),1_000_000);
        m.ingest(tone(hz,hz,.5),2_000_000);
        AudioSignalDetector.Result r=m.finish(3);
        check(r.frames==3L*hz,"número de muestras");
        check(r.silences.size()==1,"silencio intermedio");
        check(Math.abs(r.silences.get(0).start-1)<.11,"inicio silencio");
        check(Math.abs(r.silences.get(0).end-2)<.11,"fin silencio");
        check(r.peakDbfs>-7 && r.peakDbfs<-5,"sample peak");
        check(r.rmsDbfs>-12 && r.rmsDbfs<-9,"RMS 3 s");
        check(Math.abs(r.coveredSeconds-3)<.000001,"duración cubierta");
        AudioSignalDetector allSilent=new AudioSignalDetector(hz,2);
        allSilent.ingest(silence(2*hz),0);
        AudioSignalDetector.Result z=allSilent.finish(2);
        check(z.silences.size()==1&&z.rmsDbfs==-120.0,"silencio total");
        check(Math.abs(z.silences.get(0).end-2)<.00001,"cierre del final silencioso");
        AudioSignalDetector split=new AudioSignalDetector(hz,2);
        split.ingest(silence(1123),0);
        split.ingest(silence(hz-1123),1123L*1_000_000/hz);
        check(split.finish(1).silences.size()==1,"fragmentación de buffers");
        rejects(()->new AudioSignalDetector(0,2));
        rejects(()->new AudioSignalDetector(hz,0));
        rejects(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.ingest(ByteBuffer.allocate(3),0);});
        rejects(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.ingest(silence(hz),0);a.ingest(silence(hz),4_000_000);});
        rejects(()->{AudioSignalDetector a=new AudioSignalDetector(hz,2);a.ingest(silence(hz),0);a.finish(10);});
        System.out.println("PASS "+assertions+" pure PCM assertions");
    }
}
