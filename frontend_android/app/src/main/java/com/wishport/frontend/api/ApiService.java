package com.wishport.frontend.api;

import com.wishport.frontend.data.dto.LoginRequest;
import com.wishport.frontend.data.dto.RegistroRequest;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * INTERFAZ API: Define todos los "caminos" (endpoints) para hablar con el servidor.
 * Actualizada para usar DTOs en Login y Registro para mayor seguridad.
 */
public interface ApiService {

    // --- SECCIÓN: PISTAS ---
    @GET("api/pistas")
    Call<List<Pista>> obtenerPistas();

    // --- SECCIÓN: RESERVAS ---
    @GET("api/reservas")
    Call<List<Reserva>> obtenerReservas();

    @GET("api/reservas/usuario/{idUsuario}")
    Call<List<Reserva>> obtenerReservasPorUsuario(@Path("idUsuario") int idUsuario);

    @GET("api/reservas/disponibilidad")
    Call<Map<String, Object>> verificarDisponibilidad(
            @Query("idPista") int idPista,
            @Query("fecha") String fecha,
            @Query("horaInicio") String horaInicio,
            @Query("horaFin") String horaFin
    );

    @GET("api/reservas/pista/{idPista}/fecha/{fecha}")
    Call<List<Reserva>> obtenerReservasPorPistaYFecha(
            @Path("idPista") int idPista,
            @Path("fecha") String fecha
    );

    @POST("api/reservas")
    Call<Reserva> crearReserva(@Body Reserva reserva);

    @DELETE("api/reservas/{idReserva}")
    Call<Void> cancelarReserva(@Path("idReserva") int idReserva);

    // --- SECCIÓN: USUARIOS ---
    @GET("api/usuarios/{id}")
    Call<Usuario> obtenerUsuarioPorId(@Path("id") int id);

    /** Usa RegistroRequest para no enviar campos innecesarios */
    @POST("api/usuarios")
    Call<Usuario> registrarUsuario(@Body RegistroRequest registroRequest);

    @PUT("api/usuarios/{id}")
    Call<Usuario> actualizarUsuario(@Path("id") int id, @Body Usuario usuario);

    /** Usa LoginRequest para enviar solo email y password */
    @POST("api/usuarios/login")
    Call<Map<String, Object>> login(@Body LoginRequest loginRequest);
}
