package com.wishport.frontend;

import android.app.Application;
import android.content.Context;
import dagger.hilt.android.HiltAndroidApp;

/**
 * CONFIGURACIÓN GLOBAL: Activamos Hilt para la inyección de dependencias.
 */
@HiltAndroidApp
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
