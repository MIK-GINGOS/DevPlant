package com.mixesgates.devplant.contracts;

import java.util.List;

/** Crops and the fields they grow in. */
public interface CropStore {

    List<Crop> crops();

    List<Crop> searchCrops(String query);

    List<Field> fields();

    long addField(String name, long cropId, double areaHectares, String notes);

    void deleteField(long fieldId);
}
