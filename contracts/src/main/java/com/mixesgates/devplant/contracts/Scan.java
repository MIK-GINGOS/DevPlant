package com.mixesgates.devplant.contracts;

/** One saved leaf photo. */
public final class Scan {
    public final long id;
    public final long takenAt;
    public final String photoPath;
    /** 0 when the scan is not tied to a field. */
    public final long fieldId;
    public final String fieldName;
    /** Null until diagnosed. */
    public final Diagnosis diagnosis;

    public Scan(long id, long takenAt, String photoPath, long fieldId, String fieldName,
                Diagnosis diagnosis) {
        this.id = id;
        this.takenAt = takenAt;
        this.photoPath = photoPath;
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.diagnosis = diagnosis;
    }
}
