package com.mixesgates.devplant.contracts;

/** A leaf photo on this phone, and optionally the field it came from (0 = none). */
public final class ScanRequest {
    public final String photoPath;
    public final long fieldId;

    public ScanRequest(String photoPath, long fieldId) {
        this.photoPath = photoPath;
        this.fieldId = fieldId;
    }
}
