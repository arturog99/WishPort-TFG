package com.wishport.frontend.api;

/**
 * Interfaz para definir los puntos de enlace (endpoints) de la API con Retrofit.
 */
import com.wishport.frontend.models.Pista;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/pistas")
    Call<List<Pista>> obtenerPistas();
}
