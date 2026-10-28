package com.mixesgates.devplant.contracts;

/** Reports sit beside {@link Services}, so that contract stays as it is. */
public interface ReportServices {

    ReportStore reports();

    interface Provider {
        ReportServices reportServices();
    }
}
