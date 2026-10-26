package com.mixesgates.devplant.contracts;

import java.util.Collections;
import java.util.List;

/** One plant in the catalogue: its photo and its common problems. */
public final class Plant {
    /** Matches the label the scan model outputs, e.g. "tomato". */
    public final String key;
    public final String name;
    public final String scientificName;
    public final String category;
    /** Where the photo is downloaded from. */
    public final String imageUrl;
    /** Licence and author, e.g. "CC BY-SA 3.0, Softeis". */
    public final String imageCredit;
    /** Web page of the photo, for the credit line. */
    public final String imageSource;
    public final List<PlantProblem> problems;

    public Plant(String key, String name, String scientificName, String category, String imageUrl,
                 String imageCredit, String imageSource, List<PlantProblem> problems) {
        this.key = key;
        this.name = name;
        this.scientificName = scientificName;
        this.category = category;
        this.imageUrl = imageUrl;
        this.imageCredit = imageCredit;
        this.imageSource = imageSource;
        this.problems = Collections.unmodifiableList(problems);
    }
}
