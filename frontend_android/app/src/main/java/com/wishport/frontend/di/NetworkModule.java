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
 * MÓDULO HILT DE RED: Centraliza la creación de objetos relacionados con la conexión.
 * Hilt se encargará de inyectar estos objetos automáticamente donde se necesiten.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

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

    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, LocalDateTimeAdapter.LOCAL_DATE)
                .registerTypeAdapter(LocalTime.class, LocalDateTimeAdapter.LOCAL_TIME)
                .create();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(OkHttpClient client, Gson gson) {
        return new RetrofitClient().getRetrofitInstance(client, gson).create(ApiService.class);
    }
}
