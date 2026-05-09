package com.wishport.frontend;

import android.app.Application;
import android.content.Context;

/**
 * Clase Application para mantener un contexto global accesible.
 */
public class WishPortApp extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
