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
 * MOTOR DE CONEXIÓN (Retrofit): Configura y provee el cliente HTTP de la app.
 *
 * Retrofit es la librería que permite a Android comunicarse con el backend
 * de forma sencilla. En lugar de construir peticiones HTTP manualmente,
 * defines métodos Java (en ApiService) y Retrofit hace el resto.
 *
 * Esta clase tiene dos formas de funcionar:
 * 1. A través de Hilt (nueva arquitectura): getRetrofitInstance() recibe el
 *    cliente OkHttp y Gson inyectados por NetworkModule.
 * 2. Estática (compatibilidad): getApiService() crea el cliente directamente,
 *    sin Hilt. Se usa en clases que todavía no han migrado a inyección.
 */
public class RetrofitClient {

    /** URL base del servidor backend. Todas las rutas del ApiService se construyen sobre ella. */
    public static final String BASE_URL = "https://wishport-tfg.onrender.com/";
    /** Segundos máximos de espera para cada tipo de operación de red. */
    private static final long TIMEOUT_SECONDS = 30;

    /**
     * Crea y configura una instancia de Retrofit lista para usarse.
     * Recibe el cliente HTTP (OkHttp) y el convertidor JSON (Gson) desde fuera,
     * lo que permite que Hilt los inyecte con la configuración completa (AuthInterceptor incluido).
     *
     * @param client Cliente OkHttp configurado con AuthInterceptor y timeouts.
     * @param gson   Convertidor JSON con adaptadores para LocalDate y LocalTime.
     * @return Instancia de Retrofit lista para generar implementaciones de ApiService.
     */
    public Retrofit getRetrofitInstance(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /**
     * Crea un Gson con adaptadores personalizados para fechas y horas.
     * Necesario porque el JSON del servidor usa formatos ISO 8601 (ej: "2026-05-11")
     * pero Java usa objetos LocalDate/LocalTime que no entienden ese formato por defecto.
     *
     * @return Gson configurado con adaptadores para LocalDate y LocalTime.
     */
    public static Gson createDefaultGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                .create();
    }

    /**
     * Crea un cliente OkHttp simple con timeouts pero SIN AuthInterceptor.
     * Se usa en el método estático de compatibilidad (getApiService).
     * El timeout de 30s es importante porque el servidor está en Render (cloud gratuito)
     * y puede tardar en arrancar si lleva tiempo inactivo.
     *
     * @return OkHttpClient con timeouts de 30 segundos.
     */
    public static OkHttpClient createDefaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * MÉTODO DE COMPATIBILIDAD: Crea un ApiService completo sin necesidad de Hilt.
     * Usado en clases como CheckoutActivity o DetalleReservaActivity que aún
     * no usan @Inject para obtener el ApiService.
     * NOTA: Este cliente NO incluye AuthInterceptor, por lo que el token debe
     * añadirse manualmente si se usa en rutas protegidas.
     *
     * @return ApiService listo para hacer peticiones al backend.
     */
    public static ApiService getApiService() {
        return new RetrofitClient().getRetrofitInstance(
            createDefaultClient(),
            createDefaultGson()
        ).create(ApiService.class);
    }
}
