package com.mixesgates.devplant.contracts;

/** One common problem of a catalogue plant, with what to look for and what to do. */
public final class PlantProblem {
    public final String key;
    public final String name;
    public final String signs;
    public final String treatment;

    public PlantProblem(String key, String name, String signs, String treatment) {
        this.key = key;
        this.name = name;
        this.signs = signs;
        this.treatment = treatment;
    }
}
