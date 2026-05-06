package com.wishport.frontend.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente singleton para Retrofit que gestiona la conexión con el backend.
 */
public class RetrofitClient {

    public static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // Crear Gson con adapters para java.time
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                    .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
