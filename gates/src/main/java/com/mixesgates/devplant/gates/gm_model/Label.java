package com.mixesgates.devplant.gates.gm_model;

/** One model output: a label and how sure the model is, 0..1. */
public final class Label {
    public final String name;
    public final float confidence;

    public Label(String name, float confidence) {
        this.name = name;
        this.confidence = confidence;
    }
}
