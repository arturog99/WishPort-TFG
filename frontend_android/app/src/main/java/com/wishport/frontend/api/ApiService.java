package com.wishport.frontend.api;

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
 * Cada método representa una acción que la app puede pedir al Backend.
 */
public interface ApiService {

    // --- SECCIÓN: PISTAS ---

    /** Obtiene el catálogo completo de pistas deportivas */
    @GET("api/pistas")
    Call<List<Pista>> obtenerPistas();

    // --- SECCIÓN: RESERVAS ---

    /** Lista todas las reservas del sistema (usado por Admin) */
    @GET("api/reservas")
    Call<List<Reserva>> obtenerReservas();

    /** Obtiene las reservas de un usuario específico */
    @GET("api/reservas/usuario/{idUsuario}")
    Call<List<Reserva>> obtenerReservasPorUsuario(@Path("idUsuario") int idUsuario);

    /** Comprueba si un hueco horario está libre antes de permitir pagar */
    @GET("api/reservas/disponibilidad")
    Call<Map<String, Object>> verificarDisponibilidad(
            @Query("idPista") int idPista,
            @Query("fecha") String fecha,
            @Query("horaInicio") String horaInicio,
            @Query("horaFin") String horaFin
    );

    /** Busca reservas para una pista y día concreto (rellena el cuadrante de horarios) */
    @GET("api/reservas/pista/{idPista}/fecha/{fecha}")
    Call<List<Reserva>> obtenerReservasPorPistaYFecha(
            @Path("idPista") int idPista,
            @Path("fecha") String fecha
    );

    /** Envía una nueva reserva al servidor */
    @POST("api/reservas")
    Call<Reserva> crearReserva(@Body Reserva reserva);

    /** Elimina una reserva existente */
    @DELETE("api/reservas/{idReserva}")
    Call<Void> cancelarReserva(@Path("idReserva") int idReserva);

    // --- SECCIÓN: USUARIOS ---

    /** Obtiene el perfil completo de un usuario por su ID */
    @GET("api/usuarios/{id}")
    Call<Usuario> obtenerUsuarioPorId(@Path("id") int id);

    /** Crea una cuenta nueva */
    @POST("api/usuarios")
    Call<Usuario> registrarUsuario(@Body Usuario usuario);

    /** Actualiza nombre o teléfono del usuario */
    @PUT("api/usuarios/{id}")
    Call<Usuario> actualizarUsuario(@Path("id") int id, @Body Usuario usuario);

    /** Valida credenciales y devuelve el Token JWT */
    @POST("api/usuarios/login")
    Call<Map<String, Object>> login(@Body Usuario credenciales);
}
