package mx.detrasdelboton.aura;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.RenderProcessGoneDetail;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Experimental no-Termux UI: metadata-only report + existing local review UI. */
public final class MainActivity extends Activity {
    private static final int PICK_VIDEO=30, SAVE_REPORT=31;
    private static final String HOME="https://appassets.androidplatform.net/assets/viewer.html";
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private TextView status;
    private Button export;
    private Button cancel;
    private WebView web;
    private String report;
    private volatile boolean destroyed;
    private int requestVersion;
    private Future<?> currentProbe;
    private boolean processing;
    private boolean interruptedByRecreation;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(state!=null){
            report=state.getString("aura_partial_report");
            interruptedByRecreation=state.getBoolean("aura_pending_probe",false);
        }
        showHome();
    }
    @Override protected void onSaveInstanceState(Bundle state){
        if(report!=null)state.putString("aura_partial_report",report);
        state.putBoolean("aura_pending_probe",processing);
        super.onSaveInstanceState(state);
    }
    private Button button(String text,LinearLayout box,View.OnClickListener onClick){
        Button b=new Button(this);b.setText(text);b.setOnClickListener(onClick);box.addView(b);return b;
    }
    /** Release a WebView only after detaching it from its parent, including
     * renderer crashes and screen recreation. Never reuse a destroyed view.
     */
    private void releaseViewer(){
        if(web!=null){
            WebView previous=web;
            web=null;
            if(previous.getParent() instanceof ViewGroup){
                ((ViewGroup)previous.getParent()).removeView(previous);
            }
            previous.destroy();
        }
    }
    private void showHome(){
        releaseViewer();
        ScrollView scroll=new ScrollView(this);scroll.setFitsSystemWindows(true);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,36,30,30);box.setBackgroundColor(Color.rgb(15,23,35));
        scroll.addView(box);setContentView(scroll);
        TextView title=new TextView(this);title.setText("AURA · Android POC v0.1.8");title.setTextSize(24);title.setTextColor(Color.WHITE);box.addView(title);
        status=new TextView(this);status.setTextSize(16);status.setTextColor(Color.rgb(221,235,248));
        status.setText(interruptedByRecreation
            ? "Análisis interrumpido al recrear la pantalla. Vuelve a seleccionar el video; no existe informe nuevo ni se modificó el original."
            : report==null
            ? "Metadatos, doble SHA-256, decodificación MediaCodec y RMS/silencios PCM16; muestreo visual limitado a 3–3600 s. Resultados candidatos, sin Termux."
            : "Informe parcial recuperado. Expórtalo como JSON para conservarlo; este informe no certifica la calidad del video.");box.addView(status);
        button("1. Seleccionar video (solo lectura)",box,v->pickVideo());
        export=button("2. Guardar informe técnico parcial",box,v->saveReport());export.setEnabled(report!=null);
        cancel=button("Cancelar análisis en curso",box,v->cancelProbe());cancel.setEnabled(processing);
        button("3. Revisar informe en visor offline (sin exportaciones)",box,v->showViewer());
        TextView caution=new TextView(this);caution.setTextColor(Color.rgb(255,216,160));caution.setText("NO PUBLICAR. Muestreo visual adaptativo máximo 120 fotogramas por video: posibles cuadros negros/inmovilidad; no revisa cada cuadro. RMS/peak no equivalen a LUFS/true peak; decodificación sujeta al códec Android y decisiones WebView no validan publicación.");box.addView(caution);
    }
    private void cancelProbe(){
        if(!processing)return;
        ++requestVersion;
        if(currentProbe!=null)currentProbe.cancel(true);
        currentProbe=null;processing=false;interruptedByRecreation=false;
        if(cancel!=null)cancel.setEnabled(false);
        if(export!=null)export.setEnabled(report!=null);
        if(status!=null)status.setText("Análisis cancelado. " +
            (report!=null?"Se conserva el informe anterior; puedes exportarlo.":"No se generó informe.")+
            " Original intacto. La cancelación de un códec nativo requiere validación en Android.");
    }
    private void pickVideo(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,PICK_VIDEO);}
    private void saveReport(){if(report==null)return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"AURA_informe_parcial.json");startActivityForResult(i,SAVE_REPORT);}
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(req==PICK_VIDEO){
            final int request=++requestVersion;
            if(currentProbe!=null)currentProbe.cancel(true);
            processing=true;interruptedByRecreation=false;
            if(cancel!=null)cancel.setEnabled(true);
            // Preserve the last verified report until the new source passes all checks.
            // A failed SAF provider/video must never silently erase a usable report.
            if(export!=null)export.setEnabled(false);
            status.setText("Analizando nuevo archivo; se conserva temporalmente el informe anterior hasta finalizar la comprobación…");
            currentProbe=io.submit(()->{
                try{
                    MetadataProbe.Result r=MetadataProbe.inspect(getApplicationContext(),uri);
                    runOnUiThread(()->{
                        if(destroyed || request!=requestVersion)return;
                        processing=false;currentProbe=null;report=r.report;
                        if(cancel!=null)cancel.setEnabled(false);
                        if(export!=null)export.setEnabled(true);
                        if(status!=null)status.setText("Video: "+r.name+"\nDuración: "+r.duration+" s · Pistas: "+r.trackCount+"\nSHA-256: "+r.sha+
                            "\nVisual adaptativo: "+r.visualStatus+" ("+r.visualCandidateCount+" candidatos). "+r.visualReason+
                            "\nCódec: "+r.decodeStatus+" · Audio PCM: "+r.audioStatus+r.audioSummary+
                            "\nInforme técnico candidato, SIN AUTORIZACIÓN DE PUBLICACIÓN. Guarda el JSON.");
                    });
                }catch(Exception e){runOnUiThread(()->{
                    if(!destroyed && request==requestVersion && status!=null){
                        processing=false;currentProbe=null;
                        if(cancel!=null)cancel.setEnabled(false);
                        if(export!=null)export.setEnabled(report!=null);
                        status.setText("Nuevo archivo rechazado: "+e.getMessage()+". "+
                            (report!=null?"Informe anterior conservado.":"Sin informe previo.")+" Original intacto.");
                    }
                });}
            });
        } else if(req==SAVE_REPORT){
            final String snapshot=report;
            if(snapshot==null){status.setText("No hay informe válido que guardar.");return;}
            io.execute(()->{
                final byte[] expected=snapshot.getBytes(StandardCharsets.UTF_8);
                try{
                    try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                        if(out==null)throw new IOException("Salida no disponible");
                        out.write(expected);out.flush();
                    }
                    // Read back the bytes: a provider can disregard truncate semantics.
                    // Never call the export verified when extra or truncated bytes remain.
                    try(InputStream in=getContentResolver().openInputStream(uri)){
                        if(in==null)throw new IOException("No se pudo releer el informe exportado");
                        ReportContract.verifyExactBytes(in,expected);
                    }
                    runOnUiThread(()->{if(!destroyed && status!=null)
                        status.append("\nExportación JSON cotejada por lectura de retorno; no autoriza publicación.");});
                }catch(Exception e){runOnUiThread(()->{if(!destroyed && status!=null)
                    status.append("\nExportación no verificada: "+e.getMessage());});}
            });
        }
    }
    private void showViewer(){
        releaseViewer();
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web=new WebView(this);web.setFitsSystemWindows(true);setContentView(web);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);s.setGeolocationEnabled(false);
        s.setAllowFileAccess(false);s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                super.onPageFinished(view,url);
                if(!destroyed && view==web && HOME.equals(url))prepareSafeViewer(view);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();
                if("https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&u.getPath()!=null&&u.getPath().startsWith("/assets/"))return loader.shouldInterceptRequest(u);
                return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !HOME.equals(request.getUrl().toString());}
            @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail){
                // WebView is still attached here: showHome() detaches it before destroy.
                if(view==web){showHome();status.setText("El visor se reinició. Los originales no se han modificado. Restaura tus exportaciones previamente guardadas.");}
                return true;
            }
        });
        // No WebChromeClient file chooser: only the native SAF picker is trusted.
        // The bundled viewer reads the injected native JSON, not arbitrary URIs.
        web.loadUrl(HOME);
    }
    private void prepareSafeViewer(WebView view){
        // One-way injection into bundled page; no JavaScript interface or external origins.
        String payload=report==null?"null":("'"+Base64.encodeToString(
            report.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP)+"'");
        try(java.io.InputStream in=getAssets().open("android_poc_ui.js")){
            java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();
            byte[] chunk=new byte[2048];int n;
            while((n=in.read(chunk))!=-1){if(n==0)throw new IOException("Asset sin avance");if(b.size()>32768)throw new IllegalStateException("Asset JS demasiado grande");b.write(chunk,0,n);}
            String js="window.__auraAndroidReportBase64="+payload+";\n"+b.toString("UTF-8");
            view.evaluateJavascript(js,null);
        }catch(Exception e){
            // A disabled adapter leaves the original HTML unchanged; no false success.
            view.evaluateJavascript("document.getElementById('notice').textContent='Adaptador Android no disponible; conserva el informe JSON nativo.';",null);
        }
    }
    @Override public void onBackPressed(){if(web!=null)showHome();else super.onBackPressed();}
    @Override protected void onDestroy(){
        destroyed=true;requestVersion++;
        if(currentProbe!=null)currentProbe.cancel(true);
        releaseViewer();
        io.shutdownNow();super.onDestroy();
    }
}
