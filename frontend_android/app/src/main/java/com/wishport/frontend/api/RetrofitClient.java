package com.wishport.frontend.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wishport.frontend.WishPortApp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * MOTOR DE CONEXIÓN (Retrofit): Es el encargado de configurar cómo la app se comunica con internet.
 * Solo existe una instancia (Singleton) para ahorrar recursos del móvil.
 */
public class RetrofitClient {

    // URL base donde está alojado nuestro Backend
    public static final String BASE_URL = "https://wishport-tfg.onrender.com/";
    
    private static Retrofit retrofit = null;

    /**
     * Configura y devuelve la instancia de Retrofit.
     * Aquí se definen los timeouts, los interceptores de seguridad y los conversores de fecha.
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // 1. Monitor de tráfico: Muestra en la consola de Android qué enviamos y qué recibimos
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. Interceptor de seguridad: Pega el token JWT en cada llamada automáticamente
            AuthInterceptor authInterceptor = new AuthInterceptor(WishPortApp.getContext());

            // 3. Cliente OKHttp: El "navegador" interno que hace la petición real
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS) // Espera 30s para conectar (útil para servidores gratuitos lentos)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 4. Conversor JSON: Adapta los formatos de fecha de Java a los de la base de datos
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                    .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                    .create();

            // 5. Construcción final del motor
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    /**
     * Acceso rápido a las funciones definidas en ApiService
     */
    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
