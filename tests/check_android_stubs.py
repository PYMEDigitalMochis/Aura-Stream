"""Compile all authored Java against minimal signature stubs. NOT Android SDK/Gradle validation.

The stubs test source syntax/type flow only, not actual Android API binary compatibility.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
root=Path(__file__).resolve().parents[1]
JAVA={
'android/content/Context.java': '''package android.content;
import android.content.res.AssetManager;
public class Context { public ContentResolver getContentResolver(){return null;} public AssetManager getAssets(){return null;} }''',
'android/content/res/AssetManager.java': '''package android.content.res;
public class AssetManager { public java.io.InputStream open(String n) throws java.io.IOException{return null;} }''',
'android/app/Activity.java': '''package android.app;
import android.content.*;import android.os.*;import android.view.*;
public class Activity extends Context {public static final int RESULT_OK=-1;
protected void onCreate(Bundle b){} protected void onSaveInstanceState(Bundle b){} protected void onActivityResult(int a,int b,Intent c){} protected void onDestroy(){};
public void setContentView(View v){} public Context getApplicationContext(){return this;} public void runOnUiThread(Runnable r){};
public void startActivityForResult(Intent i,int req){} public void onBackPressed(){} }''',
'android/content/Intent.java': '''package android.content;
import android.net.Uri;
public class Intent {public static final String ACTION_OPEN_DOCUMENT="",ACTION_CREATE_DOCUMENT="",CATEGORY_OPENABLE="",EXTRA_TITLE="";
public static final int FLAG_GRANT_READ_URI_PERMISSION=1;
public Intent(String s){} public Intent setType(String s){return this;} public Intent addCategory(String s){return this;} public Intent addFlags(int f){return this;}
public Intent putExtra(String s,String v){return this;} public Uri getData(){return null;} }''',
'android/content/ContentResolver.java': '''package android.content;
import android.net.Uri;import android.database.Cursor;import android.os.ParcelFileDescriptor;
public class ContentResolver {public Cursor query(Uri u,String[] v,String a,String[] b,String c){return null;}
public java.io.InputStream openInputStream(Uri u)throws java.io.IOException{return null;}
public java.io.OutputStream openOutputStream(Uri u,String m)throws java.io.IOException{return null;}
public ParcelFileDescriptor openFileDescriptor(Uri u,String m)throws java.io.IOException{return null;} }''',
'android/net/Uri.java': '''package android.net;
public class Uri {public String getScheme(){return null;}public String getHost(){return null;}public String getPath(){return null;}}''',
'android/os/Bundle.java': '''package android.os;
public class Bundle {public String getString(String s){return null;}public void putString(String s,String v){}public boolean getBoolean(String s,boolean v){return v;}public void putBoolean(String s,boolean v){}}''',
'android/os/Build.java': '''package android.os; public class Build {}''',
'android/os/ParcelFileDescriptor.java': '''package android.os;
public class ParcelFileDescriptor implements AutoCloseable {public java.io.FileDescriptor getFileDescriptor(){return null;}public void close(){} }''',
'android/graphics/Bitmap.java': '''package android.graphics;
public class Bitmap {public int getWidth(){return 80;}public int getHeight(){return 45;}
public void getPixels(int[] a,int off,int stride,int x,int y,int w,int h){}public void recycle(){} }''',
'android/graphics/Color.java': '''package android.graphics;
public class Color {public static final int WHITE=0;public static int rgb(int a,int b,int c){return 0;}}''',
'android/view/View.java': '''package android.view;
public class View {public void setFitsSystemWindows(boolean value){} public Object getParent(){return null;}public interface OnClickListener {void onClick(View v);} }''',
'android/view/ViewGroup.java': '''package android.view; public class ViewGroup extends View {public void removeView(View v){} }''',
'android/widget/TextView.java': '''package android.widget;
import android.content.*;import android.view.*;
public class TextView extends View {public TextView(Context c){}public void setText(String s){}public void setTextSize(float f){}public void setTextColor(int i){}public void append(String s){} }''',
'android/widget/Button.java': '''package android.widget;
import android.content.*;import android.view.*;
public class Button extends TextView {public Button(Context c){super(c);}public void setOnClickListener(View.OnClickListener l){}public void setEnabled(boolean b){} }''',
'android/widget/LinearLayout.java': '''package android.widget;
import android.content.*;import android.view.*;
public class LinearLayout extends ViewGroup {public static final int VERTICAL=1;
public LinearLayout(Context c){}public void setOrientation(int i){}public void setPadding(int a,int b,int c,int d){}public void setBackgroundColor(int c){}public void addView(View v){} }''',
'android/widget/ScrollView.java': '''package android.widget;
import android.content.*;import android.view.*;
public class ScrollView extends ViewGroup {public ScrollView(Context c){}public void addView(View v){} }''',
'android/webkit/ValueCallback.java': '''package android.webkit;
public interface ValueCallback<T> {void onReceiveValue(T t);}''',
'android/webkit/WebChromeClient.java': '''package android.webkit;
import android.net.Uri;import android.content.Intent;
public class WebChromeClient {
public static class FileChooserParams {public static Uri[] parseResult(int i,Intent v){return null;}public Intent createIntent(){return null;}}
public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> v,FileChooserParams f){return false;}}
''',
'android/webkit/WebResourceRequest.java': '''package android.webkit;
import android.net.Uri;
public interface WebResourceRequest {Uri getUrl();}''',
'android/webkit/WebResourceResponse.java': '''package android.webkit;
public class WebResourceResponse {public WebResourceResponse(String t,String e,java.io.InputStream in){} }''',
'android/webkit/WebView.java': '''package android.webkit;
import android.content.*;import android.view.*;
public class WebView extends View {public WebView(Context c){}public void destroy(){}public WebSettings getSettings(){return null;}
public void setWebViewClient(WebViewClient c){}public void setWebChromeClient(WebChromeClient c){}public void loadUrl(String s){}
public void evaluateJavascript(String js,ValueCallback<String> cb){} }''',
'android/webkit/WebSettings.java': '''package android.webkit;
public class WebSettings {public static final int MIXED_CONTENT_NEVER_ALLOW=1;public void setJavaScriptEnabled(boolean b){}public void setDomStorageEnabled(boolean b){}
public void setJavaScriptCanOpenWindowsAutomatically(boolean b){}public void setSupportMultipleWindows(boolean b){}
public void setGeolocationEnabled(boolean b){}
public void setAllowFileAccess(boolean b){}public void setAllowContentAccess(boolean b){}public void setAllowFileAccessFromFileURLs(boolean b){}
public void setAllowUniversalAccessFromFileURLs(boolean b){}public void setMixedContentMode(int v){} }''',
'android/webkit/WebViewClient.java': '''package android.webkit;
public class WebViewClient {public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest r){return null;}
public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return false;}
public boolean onRenderProcessGone(WebView w,RenderProcessGoneDetail d){return false;}
public void onPageFinished(WebView w,String s){} }''',
'android/webkit/RenderProcessGoneDetail.java': '''package android.webkit; public class RenderProcessGoneDetail {}''',
'androidx/webkit/WebViewAssetLoader.java': '''package androidx.webkit;
import android.net.Uri;import android.content.Context;import android.webkit.WebResourceResponse;
public class WebViewAssetLoader {public interface PathHandler {}public static class AssetsPathHandler implements PathHandler {public AssetsPathHandler(Context c){}}
public static class Builder {public Builder addPathHandler(String p,PathHandler h){return this;}public WebViewAssetLoader build(){return null;}}
public WebResourceResponse shouldInterceptRequest(Uri u){return null;}}
''',
'android/media/MediaExtractor.java': '''package android.media;
public class MediaExtractor {public void setDataSource(java.io.FileDescriptor fd)throws java.io.IOException{}public int getTrackCount(){return 0;}
public MediaFormat getTrackFormat(int index){return null;}public void selectTrack(int n){}public int readSampleData(java.nio.ByteBuffer b,int off){return -1;}
public long getSampleTime(){return 0;}public int getSampleFlags(){return 0;}public boolean advance(){return false;}
public static final int SAMPLE_FLAG_ENCRYPTED=2,SAMPLE_FLAG_PARTIAL_FRAME=4;public void release(){} }''',
'android/media/MediaFormat.java': '''package android.media;
public class MediaFormat {public static final String KEY_MIME="mime", KEY_WIDTH="width", KEY_HEIGHT="height", KEY_CHANNEL_COUNT="channels",KEY_SAMPLE_RATE="sample-rate",KEY_PCM_ENCODING="pcm-encoding",KEY_DURATION="durationUs";
public String getString(String k){return null;}public boolean containsKey(String k){return false;}public int getInteger(String k){return 0;}public long getLong(String k){return 0L;} }''',
'android/media/AudioFormat.java': '''package android.media; public class AudioFormat {public static final int ENCODING_PCM_16BIT=2;}''',
'android/media/MediaCodec.java': '''package android.media;
public class MediaCodec {public static final int BUFFER_FLAG_END_OF_STREAM=4,BUFFER_FLAG_PARTIAL_FRAME=8,INFO_TRY_AGAIN_LATER=-1,INFO_OUTPUT_BUFFERS_CHANGED=-3,INFO_OUTPUT_FORMAT_CHANGED=-2;
public static class BufferInfo {public int offset,size,flags; public long presentationTimeUs;}
public static MediaCodec createDecoderByType(String mime){return new MediaCodec();}
public void configure(MediaFormat format,android.view.View surface,Object drm,int flags){}public void start(){}public void stop(){}public void release(){}
public java.nio.ByteBuffer getInputBuffer(int n){return null;}public java.nio.ByteBuffer getOutputBuffer(int n){return null;}
public int dequeueInputBuffer(long timeoutUs){return -1;}public void queueInputBuffer(int i,int off,int size,long pts,int flags){}
public int dequeueOutputBuffer(BufferInfo info,long timeoutUs){return -1;}public void releaseOutputBuffer(int ix,boolean render){}
public MediaFormat getOutputFormat(){return null;} }''',
'android/media/MediaMetadataRetriever.java': '''package android.media;
public class MediaMetadataRetriever {public static final int METADATA_KEY_DURATION=1,OPTION_CLOSEST=3;
public android.graphics.Bitmap getScaledFrameAtTime(long a,int b,int c,int d){return null;}
public void setDataSource(java.io.FileDescriptor fd){}public String extractMetadata(int k){return null;}public void release(){} }''',
'android/database/Cursor.java': '''package android.database;
public interface Cursor extends AutoCloseable {boolean moveToFirst();int getColumnIndex(String s);boolean isNull(int index);String getString(int index);long getLong(int index);void close(); }''',
'android/provider/OpenableColumns.java': '''package android.provider;
public class OpenableColumns {public static final String DISPLAY_NAME="display_name",SIZE="size";}''',
'android/util/Base64.java': '''package android.util;
public class Base64 {public static final int NO_WRAP=2; public static String encodeToString(byte[] b,int flag){return "";} }''',
}
with TemporaryDirectory() as temp:
    stubs=Path(temp)/'stubs'; dst=Path(temp)/'classes'
    for name,content in JAVA.items():
        file=stubs/name; file.parent.mkdir(parents=True,exist_ok=True); file.write_text(content)
    app=list((root/'app/src/main/java').rglob('*.java'))
    cmd=['javac','--release','17','-Xlint:all','-d',str(dst)]+[str(f) for f in stubs.rglob('*.java')]+[str(f) for f in app]
    run=subprocess.run(cmd,capture_output=True,text=True)
    if run.returncode: raise AssertionError(run.stderr)
    assert len(app)==8
    print('OK: all 8 Java source files compile with minimal API signature stubs; NOT Android SDK/Gradle/APK')
