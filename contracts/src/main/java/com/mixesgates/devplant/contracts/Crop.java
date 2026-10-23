package com.mixesgates.devplant.contracts;

public final class Crop {
    public final long id;
    public final String name;
    public final String scientificName;

    public Crop(long id, String name, String scientificName) {
        this.id = id;
        this.name = name;
        this.scientificName = scientificName;
    }
}
