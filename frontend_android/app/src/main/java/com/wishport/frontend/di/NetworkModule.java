package com.wishport.frontend.di;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.AuthInterceptor;
import com.wishport.frontend.api.LocalDateTimeAdapter;
import com.wishport.frontend.api.RetrofitClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MÓDULO HILT DE RED: La "fábrica" que crea y configura todos los objetos de conexión.
 *
 * Hilt (el sistema de inyección de dependencias) necesita saber cómo construir
 * ciertos objetos complejos. Este módulo le dice cómo hacerlo.
 *
 * @Module           = Esta clase contiene instrucciones de construcción para Hilt.
 * @InstallIn(...)   = Los objetos creados aquí duran lo mismo que la aplicación
 *                     (SingletonComponent = desde que arranca hasta que se cierra).
 *
 * Orden de construcción que sigue Hilt:
 *   1. Crea el Gson (con adaptadores de fecha).
 *   2. Crea el OkHttpClient (con AuthInterceptor, logs y timeouts).
 *   3. Usa Gson + OkHttpClient para crear el ApiService listo para usar.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * Crea el cliente HTTP de bajo nivel (OkHttp) con toda su configuración.
     *
     * Lleva dos interceptores añadidos en orden de ejecución:
     *   1. HttpLoggingInterceptor: imprime en el log de Android todas las
     *      peticiones y respuestas HTTP. Útil para depuración durante el desarrollo.
     *   2. AuthInterceptor: añade el token JWT al header "Authorization" de cada
     *      petición que no sea pública, y fuerza logout si el servidor responde 401.
     *
     * @param context Contexto de la aplicación, necesario para que AuthInterceptor
     *                pueda acceder a TokenManager (EncryptedSharedPreferences).
     * @return OkHttpClient configurado con interceptores y timeouts de 30 segundos.
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(@ApplicationContext Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new AuthInterceptor(context))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Crea el convertidor JSON (Gson) con soporte para fechas y horas de Java 8.
     *
     * Por defecto, Gson no sabe serializar/deserializar LocalDate y LocalTime.
     * Registramos los adaptadores de LocalDateTimeAdapter para que sepa
     * convertir entre el formato ISO 8601 del JSON ("2026-05-11") y los
     * objetos Java correspondientes.
     *
     * @return Gson configurado con adaptadores para LocalDate y LocalTime.
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                .create();
    }

    /**
     * Crea el ApiService: la interfaz que usa toda la app para llamar al backend.
     *
     * Combina el OkHttpClient (con todos los interceptores) y el Gson (con los
     * adaptadores de fecha) para construir un Retrofit completo y de él extrae
     * la implementación de ApiService.
     *
     * Este ApiService es el que reciben todos los Repositorios y Activities con @Inject.
     *
     * @param client  OkHttpClient con AuthInterceptor y logs (del método anterior).
     * @param gson    Gson con adaptadores de fecha (del método anterior).
     * @return ApiService listo para hacer peticiones autenticadas al backend.
     */
    @Provides
    @Singleton
    public ApiService provideApiService(OkHttpClient client, Gson gson) {
        return new RetrofitClient().getRetrofitInstance(client, gson).create(ApiService.class);
    }
}
