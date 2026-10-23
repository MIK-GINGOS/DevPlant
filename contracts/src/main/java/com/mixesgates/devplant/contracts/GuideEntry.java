package com.mixesgates.devplant.contracts;

/** One disease or pest in the guide. */
public final class GuideEntry {
    public final String key;
    /** DISEASE or PEST. */
    public final Kind kind;
    public final String name;
    public final String scientificName;
    /** Comma separated, e.g. "Tomato, Potato". */
    public final String crops;
    public final String symptoms;
    public final String spread;

    public GuideEntry(String key, Kind kind, String name, String scientificName, String crops,
                      String symptoms, String spread) {
        this.key = key;
        this.kind = kind;
        this.name = name;
        this.scientificName = scientificName;
        this.crops = crops;
        this.symptoms = symptoms;
        this.spread = spread;
    }
}
