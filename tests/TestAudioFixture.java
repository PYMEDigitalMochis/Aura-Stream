package mx.detrasdelboton.aura;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
public final class TestAudioFixture {
    public static void main(String[] args) throws Exception {
        byte[] raw=Files.readAllBytes(Path.of(args[0]));
        int rate=Integer.parseInt(args[1]);
        if(raw.length%2!=0)throw new AssertionError("PCM inválido");
        AudioSignalDetector meter=new AudioSignalDetector(rate,1);
        // Vary frame batch sizes as Android hardware decoders do. Feed PTS of
        // first sample in each batch, not guessed wall-clock time.
        int pos=0,window=389;
        while(pos<raw.length){
            int count=Math.min(window,raw.length-pos);count-=count%2;
            ByteBuffer b=ByteBuffer.wrap(raw,pos,count).slice().order(ByteOrder.nativeOrder());
            meter.ingest(b, (long)pos/2*1_000_000/rate);
            pos+=count;
            window=window==389?2102:389;
        }
        AudioSignalDetector.Result result=meter.finish(3.0);
        if(result.silences.size()!=1)throw new AssertionError("Se esperaba 1 silencio: "+result.silences.size());
        double start=result.silences.get(0).start,end=result.silences.get(0).end;
        if(Math.abs(start-1)>0.17 || Math.abs(end-2)>0.17)
            throw new AssertionError("Intervalo fuera de margen AAC "+start+"-"+end);
        if(result.rmsDbfs<-30 || result.rmsDbfs>-10)throw new AssertionError("Nivel RMS inesperado");
        System.out.println("PASS AAC→PCM+RMS+silence "+start+"-"+end+" rms="+result.rmsDbfs);
    }
}
