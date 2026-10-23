package com.mixesgates.devplant.app_rules;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;

/** Turns a leaf photo upright, crops it square and resizes it for the model. */
public final class LeafPictures {

    public Bitmap prepare(File photo, int size) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photo.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Not a readable image: " + photo.getName());
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        int shortSide = Math.min(bounds.outWidth, bounds.outHeight);
        options.inSampleSize = 1;
        while (shortSide / (options.inSampleSize * 2) >= size * 2) options.inSampleSize *= 2;
        Bitmap raw = BitmapFactory.decodeFile(photo.getPath(), options);
        if (raw == null) throw new IOException("Could not decode " + photo.getName());

        int side = Math.min(raw.getWidth(), raw.getHeight());
        Matrix matrix = orientation(photo);
        matrix.postScale((float) size / side, (float) size / side);
        Bitmap square = Bitmap.createBitmap(raw, (raw.getWidth() - side) / 2,
                (raw.getHeight() - side) / 2, side, side, matrix, true);
        if (square != raw) raw.recycle();
        return square;
    }

    /** Measures how much of the picture is leaf and how much of the leaf looks damaged. */
    public LeafStats measure(Bitmap square) {
        int w = square.getWidth(), h = square.getHeight();
        int[] pixels = new int[w * h];
        square.getPixels(pixels, 0, w, 0, 0, w, h);
        float[] hsv = new float[3];
        int green = 0, damaged = 0;
        for (int p : pixels) {
            Color.colorToHSV(p, hsv);
            float hue = hsv[0], s = hsv[1], v = hsv[2];
            if (s < 0.2f || v < 0.12f) continue;
            if (hue >= 70 && hue <= 170) green++;
            else if (hue >= 10 && hue < 70) damaged++;
        }
        int plant = green + damaged;
        float leaf = (float) plant / pixels.length;
        float damage = plant == 0 ? 0f : (float) damaged / plant;
        return new LeafStats(leaf, damage);
    }

    private static Matrix orientation(File photo) {
        Matrix m = new Matrix();
        int o;
        try {
            o = new ExifInterface(photo.getPath()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            return m;
        }
        switch (o) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: m.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180: m.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: m.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: m.setRotate(90); m.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90: m.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: m.setRotate(-90); m.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270: m.setRotate(-90); break;
            default: break;
        }
        return m;
    }
}
