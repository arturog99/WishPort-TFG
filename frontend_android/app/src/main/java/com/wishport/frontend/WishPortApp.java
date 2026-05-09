package com.wishport.frontend;

import android.app.Application;
import android.content.Context;

/**
 * CONFIGURACIÓN GLOBAL DE LA APP (Application):
 * Esta clase se ejecuta antes que cualquier pantalla. 
 * Sirve para guardar un "Contexto" global que permite a clases que no son pantallas
 * (como el Interceptor o el TokenManager) acceder a recursos del sistema.
 */
public class WishPortApp extends Application {
    
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        // Guardamos el contexto de la aplicación al arrancar
        context = getApplicationContext();
    }

    /**
     * Permite obtener el contexto desde cualquier parte del código.
     */
    public static Context getContext() {
        return context;
    }
}
