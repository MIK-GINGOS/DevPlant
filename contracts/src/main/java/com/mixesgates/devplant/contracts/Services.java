package com.mixesgates.devplant.contracts;

/** Everything the screens may use. The app decides what sits behind each store. */
public interface Services {

    ScanStore scans();

    DiagnosisStore diagnoses();

    CropStore crops();

    AlertStore alerts();

    Changes changes();

    /** Implemented by the Application so screens can find the services. */
    interface Provider {
        Services services();
    }
}
