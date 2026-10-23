package com.mixesgates.devplant.gates.gm_model;

import android.graphics.Bitmap;

import java.util.List;

/** An on-device leaf classifier. */
public interface LeafModel {

    String name();

    /** False for the stand-in used when no trained model is bundled. */
    boolean isTrained();

    /** Side of the square bitmap {@link #classify} expects, in pixels. */
    int inputSize();

    /** Top labels, most confident first. Blocks; call off the main thread. */
    List<Label> classify(Bitmap square);

    void close();
}
