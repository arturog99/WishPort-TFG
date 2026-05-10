package com.wishport.frontend.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/**
 * GESTOR DE SEGURIDAD: Almacena el JWT de forma cifrada.
 * Usa la API estable 1.0.0 (MasterKeys) para evitar errores de arranque.
 */
public class TokenManager {
    private static final String PREF_NAME = "WishPortSecureStorage";
    private static final String KEY_TOKEN = "jwt_token";
    private static String cachedToken = null;

    private static SharedPreferences getPrefs(Context context) {
        try {
            // API ESTABLE 1.0.0
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Throwable t) {
            // FALLBACK: Si falla el sistema de llaves de Android, usamos el modo privado normal.
            // Esto es vital para que la app arranque en todos los dispositivos.
            Log.e("WishPort_Security", "Usando almacenamiento estándar por fallo en Keystore", t);
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static void setToken(Context context, String jwt) {
        cachedToken = jwt;
        try {
            getPrefs(context).edit().putString(KEY_TOKEN, jwt).apply();
        } catch (Exception ignored) {}
    }

    public static String getToken(Context context) {
        if (cachedToken == null && context != null) {
            try {
                cachedToken = getPrefs(context).getString(KEY_TOKEN, null);
            } catch (Exception e) {
                return null;
            }
        }
        return cachedToken;
    }

    public static String getToken() {
        return cachedToken;
    }

    public static void clear(Context context) {
        cachedToken = null;
        try {
            getPrefs(context).edit().remove(KEY_TOKEN).apply();
        } catch (Exception ignored) {}
    }
}
