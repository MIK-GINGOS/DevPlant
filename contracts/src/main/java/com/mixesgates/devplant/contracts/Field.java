package com.mixesgates.devplant.contracts;

/** A plot of land growing one crop. */
public final class Field {
    public final long id;
    public final String name;
    public final long cropId;
    public final String cropName;
    public final double areaHectares;
    public final String notes;
    public final long createdAt;

    public Field(long id, String name, long cropId, String cropName,
                 double areaHectares, String notes, long createdAt) {
        this.id = id;
        this.name = name;
        this.cropId = cropId;
        this.cropName = cropName;
        this.areaHectares = areaHectares;
        this.notes = notes;
        this.createdAt = createdAt;
    }
}
