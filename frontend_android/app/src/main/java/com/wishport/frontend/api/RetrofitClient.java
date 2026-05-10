package com.wishport.frontend.api;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MOTOR DE CONEXIÓN (Retrofit): Simplificado para trabajar con Hilt.
 */
public class RetrofitClient {

    public static final String BASE_URL = "https://wishport-tfg.onrender.com/";
    private static final long TIMEOUT_SECONDS = 30;

    /**
     * Devuelve una instancia configurada de Retrofit.
     * Ahora recibe el cliente y el conversor por parámetros (inyectados por Hilt).
     */
    public Retrofit getRetrofitInstance(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /**
     * Crea un OkHttpClient con timeouts razonables para evitar bloqueos indefinidos.
     * Útil contra cold starts de servidores en la nube (ej. Render).
     */
    public static OkHttpClient createDefaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Mantenemos este método estático por compatibilidad temporal
     * mientras migramos todas las actividades a Hilt.
     */
    public static ApiService getApiService() {
        // En una app 100% Hilt, este método desaparecería.
        return new RetrofitClient().getRetrofitInstance(
            createDefaultClient(),
            new Gson()
        ).create(ApiService.class);
    }
}
