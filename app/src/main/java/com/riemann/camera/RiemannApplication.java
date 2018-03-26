package com.riemann.camera;

import android.app.Application;

import com.riemann.camera.util.Util;

public class RiemannApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Util.initialize(getApplicationContext());
    }
}