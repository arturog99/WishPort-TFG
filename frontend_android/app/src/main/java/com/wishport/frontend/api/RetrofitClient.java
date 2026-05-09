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
 * Cliente singleton para Retrofit que gestiona la conexión con el backend.
 */
public class RetrofitClient {

    public static final String BASE_URL = "https://wishport-tfg.onrender.com/";
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Pasamos el contexto global al interceptor para gestionar el ciclo de vida del token
            AuthInterceptor authInterceptor = new AuthInterceptor(WishPortApp.getContext());

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                    .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
