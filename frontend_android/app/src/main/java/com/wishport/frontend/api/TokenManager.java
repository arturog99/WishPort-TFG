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
    private static final String KEY_LOGIN_TIME = "login_timestamp";
    private static final long SESSION_DURATION_MS = 3600000; // 1 hora en milisegundos
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
            long now = System.currentTimeMillis();
            getPrefs(context).edit()
                    .putString(KEY_TOKEN, jwt)
                    .putLong(KEY_LOGIN_TIME, now)
                    .apply();
            Log.d("TokenManager", "Token guardado. Sesión iniciada a las: " + now);
        } catch (Exception ignored) {}
    }

    public static String getToken(Context context) {
        // Verificar si la sesión ha expirado (1 hora)
        if (isSessionExpired(context)) {
            Log.w("TokenManager", "Sesión expirada (más de 1 hora). Cerrando sesión...");
            clear(context);
            return null;
        }

        if (cachedToken == null && context != null) {
            try {
                cachedToken = getPrefs(context).getString(KEY_TOKEN, null);
            } catch (Exception e) {
                return null;
            }
        }
        return cachedToken;
    }

    /**
     * Verifica si la sesión ha expirado (más de 1 hora desde el login).
     */
    public static boolean isSessionExpired(Context context) {
        try {
            long loginTime = getPrefs(context).getLong(KEY_LOGIN_TIME, 0);
            if (loginTime == 0) {
                return true; // No hay registro de login = expirado
            }
            long elapsed = System.currentTimeMillis() - loginTime;
            return elapsed > SESSION_DURATION_MS;
        } catch (Exception e) {
            return true; // Si hay error, consideramos expirado
        }
    }

    /**
     * Devuelve los milisegundos restantes de sesión (0 si expiró).
     */
    public static long getRemainingTimeMs(Context context) {
        try {
            long loginTime = getPrefs(context).getLong(KEY_LOGIN_TIME, 0);
            if (loginTime == 0) return 0;
            long elapsed = System.currentTimeMillis() - loginTime;
            long remaining = SESSION_DURATION_MS - elapsed;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getToken() {
        return cachedToken;
    }

    public static void clear(Context context) {
        cachedToken = null;
        try {
            getPrefs(context).edit()
                    .remove(KEY_TOKEN)
                    .remove(KEY_LOGIN_TIME)
                    .apply();
            Log.d("TokenManager", "Sesión cerrada. Token y timestamp eliminados.");
        } catch (Exception ignored) {}
    }
}
