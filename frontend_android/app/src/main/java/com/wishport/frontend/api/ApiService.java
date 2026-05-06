package com.wishport.frontend.api;

/**
 * Interfaz para definir los puntos de enlace (endpoints) de la API con Retrofit.
 */
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
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- ENDPOINTS PISTAS ---
    @GET("api/pistas")
    Call<List<Pista>> obtenerPistas();

    // --- ENDPOINTS RESERVAS ---
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

    // --- ENDPOINTS USUARIOS ---
    @GET("api/usuarios")
    Call<List<Usuario>> obtenerUsuarios();

    @POST("api/usuarios")
    Call<Usuario> registrarUsuario(@Body Usuario usuario);

    @POST("api/usuarios/login")
    Call<Usuario> login(@Body Usuario credenciales);
}
