package com.mixesgates.devplant.contracts;

import java.util.List;

/**
 * The plant catalogue: every plant a scan can name. It comes from the cloud and a copy is kept
 * on the phone, so it keeps working offline after the first download.
 */
public interface PlantCatalog {

    /** Every plant in catalogue order. Downloads the catalogue the first time. */
    List<Plant> plants() throws Exception;

    /** Category names in catalogue order. */
    List<String> categories() throws Exception;

    /** Null when the key is unknown. */
    Plant plant(String key) throws Exception;

    /** Plants whose name, scientific name, category or problems contain every word. */
    List<Plant> search(String query, String category) throws Exception;

    /** A file on the phone holding the plant's photo, downloaded on first use. Null if unavailable. */
    String photoFile(Plant plant);

    /** The photo file if it is already on the phone, else null. Never uses the network. */
    String savedPhotoFile(Plant plant);

    /** Where the catalogue comes from, e.g. "https://devplant.vercel.app". */
    String source();
}
