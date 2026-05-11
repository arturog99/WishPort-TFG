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
 *
 * Cada método aquí se corresponde con una ruta concreta del backend.
 * Retrofit lee las anotaciones (@GET, @POST, etc.) y genera automáticamente
 * el código HTTP necesario para hacer cada petición.
 *
 * Las anotaciones más usadas:
 *   @GET    -> Petición para leer datos (no modifica nada).
 *   @POST   -> Petición para crear un nuevo recurso.
 *   @PUT    -> Petición para actualizar un recurso existente.
 *   @DELETE -> Petición para eliminar un recurso.
 *   @Body   -> El objeto Java que se envía convertido a JSON en el cuerpo.
 *   @Path   -> Sustituye una variable en la URL, ej: {idUsuario}.
 *   @Query  -> Añade parámetros a la URL, ej: ?idPista=1&fecha=2026-05-11.
 */
public interface ApiService {

    // =========================================================================
    // SECCIÓN: PISTAS
    // =========================================================================

    /**
     * Obtiene la lista completa de pistas deportivas del servidor.
     * Endpoint púBlico: no necesita token JWT.
     * Usado en: PistasActivity al arrancar la pantalla principal.
     */
    @GET("api/pistas")
    Call<List<Pista>> obtenerPistas();

    // =========================================================================
    // SECCIÓN: RESERVAS
    // =========================================================================

    /**
     * Obtiene TODAS las reservas del sistema.
     * Solo accesible para administradores (el backend lo valida por rol).
     */
    @GET("api/reservas")
    Call<List<Reserva>> obtenerReservas();

    /**
     * Obtiene solo las reservas del día de hoy, filtradas en el servidor.
     * Solo accesible para administradores (el backend lo valida por rol).
     * Usado en: AdminActivity para mostrar las reservas del día y validar QRs.
     */
    @GET("api/reservas/hoy")
    Call<List<Reserva>> obtenerReservasHoy();

    /**
     * Obtiene solo las reservas de un usuario concreto.
     * Requiere token JWT. El backend verifica que el usuario solo vea las suyas.
     * Usado en: ReservasActivity (pantalla "Mis Reservas").
     * @param idUsuario Identificador del usuario del que se quieren las reservas.
     */
    @GET("api/reservas/usuario/{idUsuario}")
    Call<List<Reserva>> obtenerReservasPorUsuario(@Path("idUsuario") int idUsuario);

    /**
     * Comprueba si una franja horaria está disponible antes de confirmar la reserva.
     * Endpoint púBlico: no requiere token (cualquiera puede ver disponibilidad).
     * Usado en: DetallePistaActivity justo antes de ir al Checkout.
     * @param idPista    ID de la pista a verificar.
     * @param fecha      Fecha en formato yyyy-MM-dd.
     * @param horaInicio Hora de inicio en formato HH:mm.
     * @param horaFin    Hora de fin en formato HH:mm.
     */
    @GET("api/reservas/disponibilidad")
    Call<Map<String, Object>> verificarDisponibilidad(
            @Query("idPista") int idPista,
            @Query("fecha") String fecha,
            @Query("horaInicio") String horaInicio,
            @Query("horaFin") String horaFin
    );

    /**
     * Obtiene las reservas de una pista en una fecha concreta.
     * Usado en: DetallePistaActivity para pintar los botones horarios en gris (ocupados).
     * @param idPista ID de la pista.
     * @param fecha   Fecha en formato yyyy-MM-dd.
     */
    @GET("api/reservas/pista/{idPista}/fecha/{fecha}")
    Call<List<Reserva>> obtenerReservasPorPistaYFecha(
            @Path("idPista") int idPista,
            @Path("fecha") String fecha
    );

    /**
     * Crea una nueva reserva en el servidor.
     * Requiere token JWT. El backend ignora el idUsuario del body y usa el del token.
     * Usado en: CheckoutActivity cuando el usuario confirma el pago.
     * @param reserva Objeto con fecha, hora, pista y usuario.
     */
    @POST("api/reservas")
    Call<Reserva> crearReserva(@Body Reserva reserva);

    /**
     * Cancela (elimina) una reserva por su ID.
     * Requiere token JWT. Solo puede cancelarla su dueño o un admin.
     * Devuelve Void porque el servidor no devuelve cuerpo, solo código 204.
     * Usado en: DetalleReservaActivity cuando el usuario pulsa "Cancelar".
     * @param idReserva Identificador de la reserva a cancelar.
     */
    @DELETE("api/reservas/{idReserva}")
    Call<Void> cancelarReserva(@Path("idReserva") int idReserva);

    // =========================================================================
    // SECCIÓN: USUARIOS
    // =========================================================================

    /**
     * Obtiene los datos de un usuario por su ID.
     * Requiere token JWT.
     * Usado en: PerfilActivity para cargar los datos actuales del perfil.
     * @param id Identificador del usuario.
     */
    @GET("api/usuarios/{id}")
    Call<Usuario> obtenerUsuarioPorId(@Path("id") int id);

    /**
     * Registra un nuevo usuario en el sistema.
     * Usa RegistroRequest para no enviar campos innecesarios (como idUsuario o rol).
     * Endpoint púBlico: no requiere token.
     * Usado en: RegistroActivity al pulsar el botón de registrarse.
     * @param registroRequest DTO con nombre, email, password y teléfono.
     */
    @POST("api/usuarios")
    Call<Usuario> registrarUsuario(@Body RegistroRequest registroRequest);

    /**
     * Actualiza los datos del perfil de un usuario existente.
     * Requiere token JWT.
     * Usado en: PerfilActivity al pulsar "Guardar cambios".
     * @param id      Identificador del usuario a actualizar.
     * @param usuario Objeto con los nuevos datos (nombre y teléfono).
     */
    @PUT("api/usuarios/{id}")
    Call<Usuario> actualizarUsuario(@Path("id") int id, @Body Usuario usuario);

    /**
     * Inicia sesión en el sistema.
     * Usa LoginRequest para enviar solo email y password (más seguro y limpio).
     * Endpoint púBlico: no requiere token (aún no lo tiene).
     * Devuelve un Map porque el servidor responde con campos variados: token, nombre, rol, etc.
     * Usado en: LoginActivity al pulsar el botón de entrar.
     * @param loginRequest DTO con email y password.
     */
    @POST("api/usuarios/login")
    Call<Map<String, Object>> login(@Body LoginRequest loginRequest);
}
