package com.wishport.frontend.data.repository;

import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.data.dto.LoginRequest;
import com.wishport.frontend.data.dto.RegistroRequest;
import com.wishport.frontend.models.Usuario;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;

/**
 * REPOSITORIO DE AUTENTICACIÓN: Centraliza las llamadas a la API relacionadas con usuarios.
 */
@Singleton
public class AuthRepository {

    private final ApiService apiService;

    @Inject
    public AuthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public Call<Map<String, Object>> login(LoginRequest request) {
        return apiService.login(request);
    }

    public Call<Usuario> registrar(RegistroRequest request) {
        return apiService.registrarUsuario(request);
    }
    
    public Call<Usuario> obtenerPerfil(int id) {
        return apiService.obtenerUsuarioPorId(id);
    }
    
    public Call<Usuario> actualizarPerfil(int id, Usuario usuario) {
        return apiService.actualizarUsuario(id, usuario);
    }
}
