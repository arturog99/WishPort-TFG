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
 * REPOSITORIO DE AUTENTICACIÓN: Centraliza todas las llamadas a la API
 * relacionadas con usuarios (login, registro y gestión de perfil).
 *
 * Actua como intermediario entre el ViewModel y el ApiService.
 * El ViewModel nunca llama directamente a la API: siempre pasa por aqui.
 * Esto facilita cambiar la fuente de datos (ej: base de datos local, otro servidor)
 * sin tocar el ViewModel ni la pantalla.
 *
 * @Singleton garantiza que Hilt crea UNA SOLA instancia de esta clase en toda la app.
 */
@Singleton
public class AuthRepository {

    /** Interfaz de Retrofit con todos los endpoints del backend */
    private final ApiService apiService;

    /**
     * Constructor inyectado por Hilt.
     * @Inject indica a Hilt que debe proporcionar el ApiService automáticamente.
     * @param apiService Interfaz Retrofit proporcionada por NetworkModule.
     */
    @Inject
    public AuthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Prepara la llamada HTTP de login.
     * @param request DTO con email y password del usuario.
     * @return Un Call que al ejecutarse manda POST /api/usuarios/login al backend.
     */
    public Call<Map<String, Object>> login(LoginRequest request) {
        return apiService.login(request);
    }

    /**
     * Prepara la llamada HTTP de registro.
     * @param request DTO con nombre, email, password y telefono.
     * @return Un Call que al ejecutarse manda POST /api/usuarios al backend.
     */
    public Call<Usuario> registrar(RegistroRequest request) {
        return apiService.registrarUsuario(request);
    }

    /**
     * Prepara la llamada para obtener los datos del perfil de un usuario.
     * @param id Identificador del usuario a consultar.
     * @return Un Call que al ejecutarse manda GET /api/usuarios/{id} al backend.
     */
    public Call<Usuario> obtenerPerfil(int id) {
        return apiService.obtenerUsuarioPorId(id);
    }

    /**
     * Prepara la llamada para actualizar los datos del perfil.
     * @param id      Identificador del usuario a actualizar.
     * @param usuario Objeto Usuario con los nuevos datos (nombre y teléfono).
     * @return Un Call que al ejecutarse manda PUT /api/usuarios/{id} al backend.
     */
    public Call<Usuario> actualizarPerfil(int id, Usuario usuario) {
        return apiService.actualizarUsuario(id, usuario);
    }
}
