package com.mixesgates.devplant.gates.gm_model;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in used until a trained model is bundled. It only looks at leaf colour, so it can tell
 * "mostly green", "yellowing" and "brown spotting" apart and nothing finer. Confidence is capped
 * so the rules treat its answers as tentative.
 */
public final class ColourHeuristicModel implements LeafModel {

    private static final int SIZE = 128;
    private static final float CAP = 0.75f;

    @Override
    public String name() {
        return "Colour heuristic (no trained model bundled)";
    }

    @Override
    public boolean isTrained() {
        return false;
    }

    @Override
    public int inputSize() {
        return SIZE;
    }

    @Override
    public List<Label> classify(Bitmap square) {
        Bitmap bitmap = square.getWidth() == SIZE && square.getHeight() == SIZE
                ? square : Bitmap.createScaledBitmap(square, SIZE, SIZE, true);
        int[] pixels = new int[SIZE * SIZE];
        bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE);

        float[] hsv = new float[3];
        int green = 0, yellow = 0, brown = 0;
        for (int p : pixels) {
            Color.colorToHSV(p, hsv);
            float h = hsv[0], s = hsv[1], v = hsv[2];
            if (s < 0.2f || v < 0.12f) continue;
            if (h >= 70 && h <= 170) green++;
            else if (h >= 45 && h < 70 && v > 0.35f) yellow++;
            else if (h >= 10 && h < 45 && v < 0.75f) brown++;
        }
        int plant = green + yellow + brown;
        List<Label> labels = new ArrayList<>();
        if (plant < pixels.length / 10) {
            // Not enough leaf in view; low confidence makes the rules ask for a retake.
            labels.add(new Label("healthy", 0.2f));
            return labels;
        }
        float y = (float) yellow / plant;
        float b = (float) brown / plant;
        float healthy = Math.max(0f, 1f - 4f * (y + b));
        float spot = Math.min(1f, 2.5f * b);
        float chlorosis = Math.min(1f, 2.5f * y);
        float sum = healthy + spot + chlorosis;
        if (sum == 0f) sum = 1f;
        labels.add(new Label("healthy", CAP * healthy / sum));
        labels.add(new Label("leaf_spot", CAP * spot / sum));
        labels.add(new Label("chlorosis", CAP * chlorosis / sum));
        labels.sort((l1, l2) -> Float.compare(l2.confidence, l1.confidence));
        return labels;
    }

    @Override
    public void close() {
    }
}
