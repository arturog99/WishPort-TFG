package com.wishport.frontend.data.repository;

import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.models.Pista;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;

/**
 * REPOSITORIO DE PISTAS: Se encarga de gestionar la obtención de datos de las pistas.
 * Actúa como intermediario entre el ViewModel y la API.
 */
@Singleton
public class PistasRepository {

    private final ApiService apiService;

    @Inject
    public PistasRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Devuelve la llamada para obtener todas las pistas.
     */
    public Call<List<Pista>> getPistas() {
        return apiService.obtenerPistas();
    }
}
