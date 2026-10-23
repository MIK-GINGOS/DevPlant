package com.mixesgates.devplant.contracts;

import java.util.List;

/** Results, treatments and the pest and disease guide. */
public interface DiagnosisStore {

    /** Null when the scan has no diagnosis. */
    Diagnosis forScan(long scanId);

    List<Treatment> treatmentsFor(String key);

    /** Every disease and pest the app knows about. */
    List<GuideEntry> guide();

    /** Null when the key is unknown. */
    GuideEntry guideEntry(String key);

    List<GuideEntry> searchGuide(String query);

    /** Human-readable name of the model doing the diagnosis. */
    String modelName();
}
