package com.wishport.frontend.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MOTOR DE CONEXIÓN (Retrofit): Simplificado para trabajar con Hilt.
 * Mantenemos soporte para llamadas estáticas mientras termina la migración.
 */
public class RetrofitClient {

    public static final String BASE_URL = "https://wishport-tfg.onrender.com/";
    private static final long TIMEOUT_SECONDS = 30;

    /**
     * Devuelve una instancia configurada de Retrofit.
     * Inyectada por Hilt en la nueva arquitectura.
     */
    public Retrofit getRetrofitInstance(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /**
     * Crea los adaptadores de fecha para que Java entienda el JSON del servidor.
     */
    public static Gson createDefaultGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                .create();
    }

    /**
     * Cliente OKHttp con timeouts para servidores en la nube.
     */
    public static OkHttpClient createDefaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * MÉTODO DE COMPATIBILIDAD: Permite usar la API en clases que aún no tienen Hilt.
     */
    public static ApiService getApiService() {
        return new RetrofitClient().getRetrofitInstance(
            createDefaultClient(),
            createDefaultGson()
        ).create(ApiService.class);
    }
}
