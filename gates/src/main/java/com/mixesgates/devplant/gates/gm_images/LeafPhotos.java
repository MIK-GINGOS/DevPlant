package com.mixesgates.devplant.gates.gm_images;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Leaf photo files in app-private storage. They never leave the phone. */
public final class LeafPhotos {

    private final File dir;

    public LeafPhotos(Context context) {
        dir = new File(context.getFilesDir(), "leaves");
    }

    /** Copies a photo into permanent storage and returns the new file. */
    public File keep(File source) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        File target = new File(dir, "leaf_" + stamp + ".jpg");
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
        return target;
    }

    /** Deletes a kept photo. Paths outside the leaves folder are ignored. */
    public void delete(String path) {
        if (path == null) return;
        File file = new File(path);
        if (dir.equals(file.getParentFile())) //noinspection ResultOfMethodCallIgnored
            file.delete();
    }
}
