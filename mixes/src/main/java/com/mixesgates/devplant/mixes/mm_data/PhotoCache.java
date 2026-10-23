package com.mixesgates.devplant.mixes.mm_data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Decodes leaf photos at screen size, upright, and keeps recent ones in memory. */
public final class PhotoCache {

    private final LruCache<String, Bitmap> cache =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount() / 1024;
                }
            };
    private final ExecutorService decoder = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    PhotoCache() {
    }

    public void load(ImageView view, String path, int sizePx) {
        String key = path + "@" + sizePx;
        view.setTag(key);
        Bitmap hit = cache.get(key);
        if (hit != null) {
            view.setImageBitmap(hit);
            return;
        }
        view.setImageDrawable(null);
        decoder.execute(() -> {
            Bitmap bitmap = decode(path, sizePx);
            if (bitmap != null) cache.put(key, bitmap);
            main.post(() -> {
                if (key.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap decode(String path, int sizePx) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int shortSide = Math.min(bounds.outWidth, bounds.outHeight);
        options.inSampleSize = 1;
        while (shortSide / (options.inSampleSize * 2) >= sizePx) options.inSampleSize *= 2;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) return null;
        int degrees = rotation(path);
        if (degrees == 0) return bitmap;
        Matrix m = new Matrix();
        m.setRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private static int rotation(String path) {
        try {
            switch (new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
