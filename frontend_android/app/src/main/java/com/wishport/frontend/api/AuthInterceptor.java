package com.wishport.frontend.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wishport.frontend.ui.LoginActivity;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * EL GUARDIÁN DE LA API (Interceptor): Su trabajo es interceptar cada mensaje que sale de la app
 * hacia el servidor para pegarle la "etiqueta" de seguridad (Token JWT).
 * También vigila si el servidor nos echa (Error 401) para cerrar la sesión.
 */
public class AuthInterceptor implements Interceptor {

    private Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();
        String method = original.method();

        // 1. EXCEPCIONES: No añadimos token en endpoints públicos (Login, Registro, Disponibilidad)
        boolean esPublico = path.contains("/api/usuarios/login")
                || (path.contains("/api/usuarios") && method.equals("POST"))
                || path.contains("/api/reservas/disponibilidad");

        if (esPublico) {
            return chain.proceed(original);
        }

        // 2. AÑADIR TOKEN: Buscamos si tenemos la "llave" guardada y la ponemos en la cabecera.
        String token = TokenManager.getToken(context);
        Request request = original;
        if (token != null) {
            request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }

        // 3. ENVIAR Y REVISAR RESPUESTA
        Response response = chain.proceed(request);

        // 4. CONTROL DE EXPIRACIÓN: Si el servidor devuelve 401, la sesión ya no vale.
        if (response.code() == 401) {
            hacerLogoutForzoso();
        }

        return response;
    }

    /**
     * Borra los datos locales y manda al usuario a la pantalla de Login.
     */
    private void hacerLogoutForzoso() {
        TokenManager.clear(context);
        
        // Usamos un Handler porque los cambios visuales deben hacerse en el "hilo principal"
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Tu sesión ha caducado. Entra de nuevo.", Toast.LENGTH_LONG).show();
            
            Intent intent = new Intent(context, LoginActivity.class);
            // Estas banderas borran todas las pantallas abiertas y dejan solo el Login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }
}
