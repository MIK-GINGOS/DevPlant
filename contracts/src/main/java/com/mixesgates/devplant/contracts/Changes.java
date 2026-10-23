package com.mixesgates.devplant.contracts;

/** Tells screens that stored data changed so they can refresh. */
public interface Changes {

    enum Topic { SCANS, CROPS, ALERTS }

    interface Listener {
        /** May be called on any thread. */
        void onChanged(Topic topic);
    }

    void listen(Listener listener);

    void unlisten(Listener listener);

    void post(Topic topic);
}
