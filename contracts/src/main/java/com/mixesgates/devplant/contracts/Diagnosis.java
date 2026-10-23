package com.mixesgates.devplant.contracts;

/** What the model and rules decided about one scan. */
public final class Diagnosis {
    public final long id;
    public final long scanId;
    public final Kind kind;
    /** Guide key such as "late_blight"; "healthy" or the raw model label otherwise. */
    public final String key;
    public final String name;
    /** 0..1, from the model. */
    public final float confidence;
    public final Severity severity;
    /** 0..1, share of the leaf that looks damaged. */
    public final float damage;
    /** Runner-up labels, e.g. "Early blight 21%, Leaf spot 9%". */
    public final String alternatives;
    public final String advice;

    public Diagnosis(long id, long scanId, Kind kind, String key, String name, float confidence,
                     Severity severity, float damage, String alternatives, String advice) {
        this.id = id;
        this.scanId = scanId;
        this.kind = kind;
        this.key = key;
        this.name = name;
        this.confidence = confidence;
        this.severity = severity;
        this.damage = damage;
        this.alternatives = alternatives;
        this.advice = advice;
    }
}
