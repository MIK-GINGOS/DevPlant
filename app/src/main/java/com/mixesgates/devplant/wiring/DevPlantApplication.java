package com.mixesgates.devplant.wiring;

import android.app.Application;

import com.mixesgates.devplant.contracts.Services;

/** Builds the services once and hands them to the screens. */
public class DevPlantApplication extends Application implements Services.Provider {

    private AppServices services;

    @Override
    public void onCreate() {
        super.onCreate();
        services = new AppServices(this);
    }

    @Override
    public Services services() {
        return services;
    }
}
