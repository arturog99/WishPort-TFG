package com.wishport.frontend.api;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * GESTOR DE TOKENS: Se encarga de guardar la "llave" (JWT) que nos da el servidor.
 * Usamos cifrado para que nadie pueda robar el token desde los archivos de la app.
 */
public class TokenManager {
    private static final String PREF_NAME = "EncryptedWishPortPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static String cachedToken = null; // Guardamos en memoria para acceso rápido

    /**
     * Crea o recupera el archivo de preferencias cifrado.
     */
    private static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            // Generamos una llave maestra de seguridad de Android
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            
            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Si el cifrado falla por algo raro, volvemos a las preferencias normales
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    /** Guarda el token cuando el usuario hace login */
    public static void setToken(Context context, String jwt) {
        cachedToken = jwt;
        getEncryptedPrefs(context).edit().putString(KEY_TOKEN, jwt).apply();
    }

    /** Recupera el token guardado (si no está en memoria, lo busca en el archivo) */
    public static String getToken(Context context) {
        if (cachedToken == null && context != null) {
            cachedToken = getEncryptedPrefs(context).getString(KEY_TOKEN, null);
        }
        return cachedToken;
    }

    /** Retorna el token que ya tenemos cargado en memoria */
    public static String getToken() {
        return cachedToken;
    }

    /** Borra el token (usado al cerrar sesión) */
    public static void clear(Context context) {
        cachedToken = null;
        getEncryptedPrefs(context).edit().remove(KEY_TOKEN).apply();
    }
}
