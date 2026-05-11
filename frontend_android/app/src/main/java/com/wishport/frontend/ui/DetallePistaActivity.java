package com.wishport.frontend.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DETALLE PISTA: Muestra información de una pista y su disponibilidad horaria.
 *
 * Recibe la pista seleccionada desde PistasActivity mediante Intent (Serializable).
 * Genera dinámicamente una cuadrícula de botones por hora (08:00 a 21:00).
 * Cada botón puede estar:
 *   - GRIS/BLOQUEADO: la hora ya está reservada o ya pasó (si es hoy).
 *   - GRIS CLARO: disponible para reservar.
 *   - VERDE: seleccionado por el usuario.
 *
 * Antes de ir al pago, hace una verificación final de disponibilidad contra
 * el servidor para evitar colisiones de reserva simultánea.
 */
@AndroidEntryPoint
public class DetallePistaActivity extends AppCompatActivity {

    /** Clave del Intent para recibir la Pista desde PistasActivity */
    public static final String EXTRA_PISTA = "extra_pista";
    /** Primer slot disponible del día (8:00h) */
    private static final int HORA_APERTURA = 8;
    /** Último slot disponible del día, exclusivo (cierra a las 22:00h) */
    private static final int HORA_CIERRE = 22;
    /** Zona horaria de España para calcular correctamente si una hora ya pasó */
    private static final ZoneId EUROPE_MADRID = ZoneId.of("Europe/Madrid");
    /** Formato de fecha para enviar al backend: 2026-05-11 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Textos que muestran la info de la pista (nombre, deporte, estado, id, fecha) */
    private TextView tvNombre, tvDeporte, tvEstado, tvId, tvFechaSeleccionada;
    /** Botón para abrir el calendario y botón para proceder a reservar */
    private Button btnSeleccionarFecha, btnReservar;
    /** Cuadrícula donde se generan los botones de cada hora del día */
    private GridLayout gridHorarios;
    /** Contenedor del gesto pull-to-refresh */
    private SwipeRefreshLayout swipeRefreshLayout;
    /** Capa de carga semitransparente que bloquea la UI durante una petición */
    private LinearLayout loadingLayout;

    /** Pista recibida desde PistasActivity */
    private Pista pistaActual;
    /** Fecha actualmente seleccionada (por defecto = hoy) */
    private LocalDate fechaSeleccionada;
    /** Hora de inicio seleccionada por el usuario (-1 = ninguna) */
    private int horaInicioSeleccionada = -1;
    /** Lista de reservas existentes para la fecha y pista seleccionadas */
    private List<Reserva> reservasExistentes = new ArrayList<>();
    /** Referencia a todos los botones hora para poder colorearlos */
    private List<Button> botonesHorarios = new ArrayList<>();

    /** ApiService inyectado por Hilt (con AuthInterceptor y timeouts) */
    @Inject ApiService apiService;

    /**
     * Inicializa la pantalla:
     * 1. Vincula las vistas del XML.
     * 2. Recoge la Pista del Intent y establece la fecha de hoy por defecto.
     * 3. Muestra la info de la pista en pantalla.
     * 4. Genera los botones de horario en el GridLayout.
     * 5. Carga del servidor las horas ya ocupadas para colorear los botones.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_pista);

        vincularVistas();

        pistaActual = (Pista) getIntent().getSerializableExtra(EXTRA_PISTA);
        fechaSeleccionada = LocalDate.now(EUROPE_MADRID); // Fecha por defecto: hoy

        if (pistaActual != null) {
            mostrarInfoPista();
        }

        btnSeleccionarFecha.setOnClickListener(v -> mostrarCalendario());
        btnReservar.setOnClickListener(v -> verificarDisponibilidadYProceder());
        swipeRefreshLayout.setOnRefreshListener(this::cargarHorariosOcupados);

        prepararGridHorarios();
        cargarHorariosOcupados();
    }

    /** Vincula todas las variables con los elementos del XML activity_detalle_pista */
    private void vincularVistas() {
        tvNombre = findViewById(R.id.tvDetalleNombre);
        tvDeporte = findViewById(R.id.tvDetalleDeporte);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvId = findViewById(R.id.tvDetalleId);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnReservar = findViewById(R.id.btnReservar);
        gridHorarios = findViewById(R.id.gridHorarios);
        loadingLayout = findViewById(R.id.loadingLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshDetalle);
    }

    /** Rellena los TextViews con los datos de la pista recibida */
    private void mostrarInfoPista() {
        tvNombre.setText(pistaActual.getNombre());
        tvDeporte.setText("Deporte: " + pistaActual.getDeporte());
        tvEstado.setText("Estado: " + pistaActual.getEstado());
        tvId.setText("ID: " + pistaActual.getIdPista());
        tvFechaSeleccionada.setText("Fecha: " + fechaSeleccionada.format(DATE_FORMATTER));
    }

    /**
     * Genera dinámicamente los botones de hora en el GridLayout.
     * Crea un botón por cada hora entre HORA_APERTURA (8) y HORA_CIERRE (22).
     * Los botones empiezan deshabilitados y se habilitan en refrescarEstadoBotones()
     * una vez que se conocen las horas ocupadas del servidor.
     * Cada botón lleva su hora guardada en setTag() para identificarla al hacer clic.
     */
    private void prepararGridHorarios() {
        gridHorarios.removeAllViews();
        botonesHorarios.clear();

        for (int hora = HORA_APERTURA; hora < HORA_CIERRE; hora++) {
            Button btnHora = new Button(this);
            btnHora.setText(String.format("%02d:00", hora));
            btnHora.setTag(hora); // Guardamos la hora como etiqueta para recuperarla en el clic
            btnHora.setEnabled(false); // Deshabilitado hasta conocer la disponibilidad

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            btnHora.setLayoutParams(params);

            final int horaFinal = hora;
            btnHora.setOnClickListener(v -> marcarHoraSeleccionada(horaFinal));

            botonesHorarios.add(btnHora);
            gridHorarios.addView(btnHora);
        }
    }

    /**
     * Abre el selector de fecha (DatePickerDialog) con la fecha mínima = hoy.
     * Al confirmar una fecha:
     * 1. Actualiza fechaSeleccionada y el texto en pantalla.
     * 2. Resetea la hora seleccionada (ya no es válida para el nuevo día).
     * 3. Recarga las horas ocupadas para la nueva fecha.
     */
    private void mostrarCalendario() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            fechaSeleccionada = LocalDate.of(year, month + 1, day); // month es 0-indexado
            tvFechaSeleccionada.setText("Fecha: " + fechaSeleccionada.format(DATE_FORMATTER));
            resetearSeleccion();
            cargarHorariosOcupados();
        }, fechaSeleccionada.getYear(), fechaSeleccionada.getMonthValue() - 1, fechaSeleccionada.getDayOfMonth());
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // No permite fechas pasadas
        dialog.show();
    }

    /**
     * Descarga del servidor las reservas existentes para esta pista y fecha.
     * GET /api/reservas/pista/{idPista}/fecha/{fecha}
     * Al recibir la respuesta, actualiza reservasExistentes y repinta los botones.
     */
    private void cargarHorariosOcupados() {
        if (pistaActual == null) return;
        swipeRefreshLayout.setRefreshing(true);
        String fechaStr = fechaSeleccionada.format(DATE_FORMATTER);

        apiService.obtenerReservasPorPistaYFecha(pistaActual.getIdPista(), fechaStr)
                .enqueue(new Callback<List<Reserva>>() {
                    @Override
                    public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            reservasExistentes = response.body();
                            refrescarEstadoBotones(); // Repintamos con la nueva información
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Reserva>> call, Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(DetallePistaActivity.this, "Sin conexión con la API", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Actualiza el color y estado de cada botón de hora según:
     * - Si la hora ya está en reservasExistentes -> GRIS/BLOQUEADO (ocupada).
     * - Si es hoy y la hora ya pasó                -> GRIS/BLOQUEADO (pasada).
     * - En caso contrario                           -> GRIS CLARO/HABILITADO (libre).
     */
    private void refrescarEstadoBotones() {
        ZonedDateTime ahora = ZonedDateTime.now(EUROPE_MADRID);
        int horaActual = ahora.getHour();
        boolean esHoy = fechaSeleccionada.equals(LocalDate.now(EUROPE_MADRID));

        for (Button btn : botonesHorarios) {
            int horaBtn = (int) btn.getTag();
            boolean ocupada = false;
            for (Reserva r : reservasExistentes) {
                if (r.getHoraInicio() != null && r.getHoraInicio().getHour() == horaBtn) {
                    ocupada = true;
                    break;
                }
            }
            boolean pasada = esHoy && horaBtn <= horaActual;
            if (ocupada || pasada) {
                btn.setEnabled(false);
                btn.setBackgroundColor(Color.GRAY);
                btn.setAlpha(0.3f); // Semitransparente para mostrar que no está disponible
            } else {
                btn.setEnabled(true);
                btn.setBackgroundColor(Color.LTGRAY);
                btn.setAlpha(1.0f);
            }
        }
    }

    /**
     * Marca visualmente la hora seleccionada (verde) y resetea las demás (gris).
     * Guarda la hora en horaInicioSeleccionada para usarla al reservar.
     * @param hora Número de hora seleccionada (ej: 18 para 18:00).
     */
    private void marcarHoraSeleccionada(int hora) {
        horaInicioSeleccionada = hora;
        for (Button btn : botonesHorarios) {
            int h = (int) btn.getTag();
            if (h == hora) btn.setBackgroundColor(Color.GREEN);
            else if (btn.isEnabled()) btn.setBackgroundColor(Color.LTGRAY);
        }
    }

    /** Limpia la hora seleccionada. Se llama al cambiar de fecha. */
    private void resetearSeleccion() {
        horaInicioSeleccionada = -1;
    }

    /**
     * Verificación final antes de ir al pago.
     * Aunque los botones ya muestran disponibilidad, hacemos una última comprobación
     * al servidor por si otro usuario reservó el mismo hueco mientras el usuario
     * estaba mirando la pantalla (condición de carrera).
     *
     * Si está disponible: navega a CheckoutActivity con la pista, fecha y hora.
     * Si ya no está: muestra aviso y recarga los horarios actualizados.
     */
    private void verificarDisponibilidadYProceder() {
        if (horaInicioSeleccionada == -1) {
            Toast.makeText(this, "Elige un horario primero", Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarLoading("Verificando...");
        String fechaStr = fechaSeleccionada.format(DATE_FORMATTER);
        String hIni = String.format("%02d:00", horaInicioSeleccionada);
        String hFin = String.format("%02d:00", horaInicioSeleccionada + 1); // Siempre 1 hora

        apiService.verificarDisponibilidad(pistaActual.getIdPista(), fechaStr, hIni, hFin)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        ocultarLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            if ((boolean) response.body().get("disponible")) {
                                // Todavía libre: pasamos los datos al Checkout
                                Intent intent = new Intent(DetallePistaActivity.this, CheckoutActivity.class);
                                intent.putExtra("EXTRA_PISTA", pistaActual);
                                intent.putExtra("EXTRA_FECHA", fechaSeleccionada.toString());
                                intent.putExtra("EXTRA_HORA_INICIO", horaInicioSeleccionada);
                                startActivity(intent);
                            } else {
                                // Se ocupó mientras tanto: recargamos los horarios
                                Toast.makeText(DetallePistaActivity.this, "Este hueco se acaba de ocupar", Toast.LENGTH_SHORT).show();
                                cargarHorariosOcupados();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) { ocultarLoading(); }
                });
    }

    /**
     * Muestra la capa de carga con un mensaje (ej: "Verificando...").
     * Bloquea la UI para evitar que el usuario pulse algo mientras se espera respuesta.
     * @param msg Texto a mostrar en la capa de carga.
     */
    private void mostrarLoading(String msg) {
        loadingLayout.setVisibility(LinearLayout.VISIBLE);
        ((TextView)findViewById(R.id.tvLoadingMensaje)).setText(msg);
    }

    /** Oculta la capa de carga y devuelve el control al usuario */
    private void ocultarLoading() {
        loadingLayout.setVisibility(LinearLayout.GONE);
    }
}
