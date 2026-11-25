package mx.detrasdelboton.aura;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import android.content.ContentResolver;
import android.net.Uri;

/** Bounded Android bitmap adapter. Real API/device validation is required. */
final class AndroidFrameSampler {
    private AndroidFrameSampler(){}
    static VideoFrameDetector.Result inspect(ContentResolver resolver,Uri uri,double duration) throws Exception {
        if(duration<3 || duration>VideoFrameDetector.MAX_COVERAGE_SECONDS)
            return VideoFrameDetector.notRun("El muestreo visual se limita a clips de 3 a 3600 s");
        try(ParcelFileDescriptor pfd=resolver.openFileDescriptor(uri,"r")){
            if(pfd==null)return VideoFrameDetector.failed("El proveedor no permite leer fotogramas",0);
            MediaMetadataRetriever reader=new MediaMetadataRetriever();
            try {
                reader.setDataSource(pfd.getFileDescriptor());
                return VideoFrameDetector.inspect(duration, timeUs -> {
                    Bitmap bmp=reader.getScaledFrameAtTime(timeUs,MediaMetadataRetriever.OPTION_CLOSEST,80,45);
                    if(bmp==null)return null;
                    try {
                        int width=bmp.getWidth(),height=bmp.getHeight();
                        if(width<8||height<8||((long)width)*height>20000)return null;
                        int[] pixels=new int[width*height];
                        bmp.getPixels(pixels,0,width,0,0,width,height);
                        return pixels;
                    } finally {bmp.recycle();}
                });
            } finally {reader.release();}
        }
    }
}
