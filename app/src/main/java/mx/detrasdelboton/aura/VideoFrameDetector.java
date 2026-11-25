package mx.detrasdelboton.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

/** Candidate-only analysis of actual decoded samples, not a whole-file video decode.
 * On timeouts or missing frames, both detectors remain incomplete and findings
 * from partial coverage are discarded to prevent misleading completed statuses.
 */
final class VideoFrameDetector {
    static final double MAX_COVERAGE_SECONDS=3600.0;
    static final int MAX_SAMPLES=120;
    static final long MAX_NANOS=45_000_000_000L;
    interface FrameSource { int[] sample(long timeUs) throws Exception; }
    static final class Finding {
        final String kind;
        final double start, end;
        Finding(String kind,double start,double end){this.kind=kind;this.start=start;this.end=end;}
    }
    static final class Result {
        final String status, reason;
        final int sampledFrames;
        final List<Finding> findings;
        Result(String status,String reason,int count,List<Finding> findings){
            this.status=status;this.reason=reason;this.sampledFrames=count;
            this.findings=Collections.unmodifiableList(new ArrayList<>(findings));
        }
    }
    private VideoFrameDetector(){}
    static Result notRun(String reason){return new Result("not_run",reason,0,Collections.emptyList());}
    static Result failed(String reason,int count){return new Result("failed",reason,count,Collections.emptyList());}
    /** Runs only for clips where one-second sampling is acceptably bounded. Even
     * successful sampling is a HEURISTIC, never full-frame decodability proof.
     */
    static Result inspect(double duration,FrameSource source) throws InterruptedException {
        return inspect(duration,source,System::nanoTime);
    }
    /** Clock-injected overload tests both sides of a potentially slow Android decoder call.
     * It cannot forcibly interrupt a blocked platform codec: the real-device gate
     * must measure that independently. Never claim a completed result after timeout.
     */
    static Result inspect(double duration,FrameSource source,LongSupplier monotonicNanos) throws InterruptedException {
        if(monotonicNanos==null)return failed("Reloj monotónico no disponible",0);
        if(!Double.isFinite(duration)||duration<3 || duration>MAX_COVERAGE_SECONDS)
            return notRun("El muestreo visual de esta versión cubre videos entre 3 y 3600 s");
        if(source==null)return failed("Fuente de fotogramas no disponible",0);
        final long started=monotonicNanos.getAsLong();
        // Cap work on longer videos: the effective detection threshold also
        // grows with step. Missing short glitches must never be interpreted as clean.
        final double step=Math.max(1.0,Math.ceil(duration/MAX_SAMPLES));
        final List<Finding> found=new ArrayList<>();
        int[] previous=null; double previousTime=0;
        int blackRun=0, freezePairs=0; double blackStart=0, freezeStart=0;
        int count=0;
        for(double time=step/2;time<duration;time+=step){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Muestreo cancelado");
            if(monotonicNanos.getAsLong()-started>MAX_NANOS)
                return failed("Tiempo de muestreo visual excedido",count);
            int[] pixels;
            try {pixels=source.sample((long)(time*1_000_000));}
            catch(InterruptedException e){throw e;}
            catch(Exception | LinkageError e){return failed("Fallo del decodificador de fotogramas",count);}
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Muestreo cancelado");
            if(monotonicNanos.getAsLong()-started>MAX_NANOS)
                return failed("Tiempo de muestreo visual excedido",count);
            if(pixels==null || pixels.length<64 || pixels.length>20000)
                return failed("Fotograma no recuperable o de tamaño inválido",count);
            boolean black=isBlack(pixels);
            if(black){if(blackRun==0)blackStart=Math.max(0,time-step/2);blackRun++;}
            else {if(blackRun>=3)found.add(new Finding("black",blackStart,Math.min(duration,previousTime+step/2)));blackRun=0;}
            boolean same=previous!=null && !black && !isBlack(previous) && identical(previous,pixels);
            if(same){if(freezePairs==0)freezeStart=Math.max(0,previousTime-step/2);freezePairs++;}
            else {if(freezePairs>=4)found.add(new Finding("freeze",freezeStart,Math.min(duration,previousTime+step/2)));freezePairs=0;}
            previous=pixels;previousTime=time;count++;
        }
        if(Thread.currentThread().isInterrupted())throw new InterruptedException("Muestreo cancelado");
        if(monotonicNanos.getAsLong()-started>MAX_NANOS)
            return failed("Tiempo de muestreo visual excedido",count);
        if(blackRun>=3)found.add(new Finding("black",blackStart,Math.min(duration,previousTime+step/2)));
        if(freezePairs>=4)found.add(new Finding("freeze",freezeStart,Math.min(duration,previousTime+step/2)));
        return new Result("completed", "Muestreo visual a intervalos de "+step+" s, máximo 120 muestras; candidatos, no examen de cada cuadro",count,found);
    }
    private static int luma(int c){int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;return (77*r+150*g+29*b)>>8;}
    private static boolean isBlack(int[] pixels){
        long total=0;int dark=0;
        for(int p:pixels){int lum=luma(p);total+=lum;if(lum<=24)dark++;}
        return total<=12L*pixels.length && dark>=Math.ceil(pixels.length*.985);
    }
    private static boolean identical(int[] a,int[] b){
        if(a.length!=b.length)return false;
        long error=0;int significant=0;
        for(int i=0;i<a.length;i++){
            int p=a[i],q=b[i];
            int dr=Math.abs(((p>>>16)&255)-((q>>>16)&255));
            int dg=Math.abs(((p>>>8)&255)-((q>>>8)&255));
            int db=Math.abs((p&255)-(q&255));
            int max=Math.max(dr,Math.max(dg,db));
            if(max>4)significant++;
            error+=dr+dg+db;
        }
        return error<=3L*a.length && significant<=a.length/100;
    }
}
