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
import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DETALLE PISTA: Permite ver info de una pista y seleccionar horario para reservar.
 * Gestiona el cuadrante de horarios y verifica disponibilidad antes de ir al pago.
 */
@AndroidEntryPoint
public class DetallePistaActivity extends AppCompatActivity {

    public static final String EXTRA_PISTA = "extra_pista";
    
    // Configuración de horarios: de 8 de la mañana a 10 de la noche (22:00)
    private static final int HORA_APERTURA = 8;
    private static final int HORA_CIERRE = 22;
    private static final ZoneId EUROPE_MADRID = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextView tvNombre, tvDeporte, tvEstado, tvId, tvFechaSeleccionada;
    private Button btnSeleccionarFecha, btnReservar;
    private GridLayout gridHorarios;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout loadingLayout;

    private Pista pistaActual;
    @Inject ApiService apiService;
    private LocalDate fechaSeleccionada;
    private int horaInicioSeleccionada = -1;
    
    private List<Reserva> reservasExistentes = new ArrayList<>();
    private List<Button> botonesHorarios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_pista);

        // 1. Inicializar vistas
        vincularVistas();

        // apiService inyectado por Hilt (con AuthInterceptor + timeouts)

        // 2. Recuperar la pista que nos pasaron desde la lista
        pistaActual = (Pista) getIntent().getSerializableExtra(EXTRA_PISTA);
        fechaSeleccionada = LocalDate.now(EUROPE_MADRID);

        if (pistaActual != null) {
            mostrarInfoPista();
        }

        // 3. Configurar eventos
        btnSeleccionarFecha.setOnClickListener(v -> mostrarCalendario());
        btnReservar.setOnClickListener(v -> verificarDisponibilidadYProceder());
        swipeRefreshLayout.setOnRefreshListener(this::cargarHorariosOcupados);

        // 4. Preparar el cuadrante de horas (botones del 8 al 22)
        prepararGridHorarios();
        
        // 5. Cargar qué horas están ya reservadas para hoy
        cargarHorariosOcupados();
    }

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

    private void mostrarInfoPista() {
        tvNombre.setText(pistaActual.getNombre());
        tvDeporte.setText("Deporte: " + pistaActual.getDeporte());
        tvEstado.setText("Estado: " + pistaActual.getEstado());
        tvId.setText("ID Pista: " + pistaActual.getIdPista());
        tvFechaSeleccionada.setText("Fecha: " + fechaSeleccionada.format(DATE_FORMATTER));
    }

    /** Crea los botones dinámicamente en el GridLayout */
    private void prepararGridHorarios() {
        gridHorarios.removeAllViews();
        botonesHorarios.clear();

        for (int hora = HORA_APERTURA; hora < HORA_CIERRE; hora++) {
            Button btnHora = new Button(this);
            btnHora.setText(String.format("%02d:00", hora));
            btnHora.setTag(hora); // Guardamos la hora para saber cuál es al hacer clic
            btnHora.setEnabled(false); // Empiezan apagados hasta que cargue la API
            
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

    /** Abre el selector de fecha y recarga los horarios */
    private void mostrarCalendario() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            fechaSeleccionada = LocalDate.of(year, month + 1, day);
            tvFechaSeleccionada.setText("Fecha: " + fechaSeleccionada.format(DATE_FORMATTER));
            resetearSeleccion();
            cargarHorariosOcupados();
        }, fechaSeleccionada.getYear(), fechaSeleccionada.getMonthValue() - 1, fechaSeleccionada.getDayOfMonth());
        
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    /** Pide al servidor las reservas de esta pista para el día elegido */
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
                        } else {
                            reservasExistentes = new ArrayList<>();
                            Toast.makeText(DetallePistaActivity.this,
                                    "No se pudo cargar ocupación. Horarios mostrados sin garantía.", Toast.LENGTH_LONG).show();
                        }
                        refrescarEstadoBotones();
                    }

                    @Override
                    public void onFailure(Call<List<Reserva>> call, Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        reservasExistentes = new ArrayList<>();
                        refrescarEstadoBotones();
                        Toast.makeText(DetallePistaActivity.this,
                                "Error de red. Puedes intentar reservar; se verificará al pagar.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Activa o desactiva botones según si la hora está ocupada o ya ha pasado */
    private void refrescarEstadoBotones() {
        ZonedDateTime ahora = ZonedDateTime.now(EUROPE_MADRID);
        int horaActual = ahora.getHour();
        boolean esHoy = fechaSeleccionada.equals(LocalDate.now(EUROPE_MADRID));

        for (Button btn : botonesHorarios) {
            int horaBtn = (int) btn.getTag();
            boolean ocupada = false;

            // Revisar si esta hora está en la lista de reservas recibida
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
                btn.setAlpha(0.3f);
            } else {
                btn.setEnabled(true);
                btn.setBackgroundColor(Color.LTGRAY);
                btn.setAlpha(1.0f);
            }
        }
    }

    private void marcarHoraSeleccionada(int hora) {
        horaInicioSeleccionada = hora;
        for (Button btn : botonesHorarios) {
            int h = (int) btn.getTag();
            // Si el botón es el clicado, verde. Si está libre, gris claro.
            if (h == hora) btn.setBackgroundColor(Color.GREEN);
            else if (btn.isEnabled()) btn.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void resetearSeleccion() {
        horaInicioSeleccionada = -1;
        for (Button btn : botonesHorarios) {
            btn.setEnabled(false);
            btn.setBackgroundColor(Color.LTGRAY);
        }
    }

    /** Antes de ir al pago, preguntamos de nuevo al servidor por si alguien se adelantó */
    private void verificarDisponibilidadYProceder() {
        if (horaInicioSeleccionada == -1) {
            Toast.makeText(this, "Por favor, elige una hora", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarLoading("Verificando hueco...");
        String fechaStr = fechaSeleccionada.format(DATE_FORMATTER);
        String hIni = String.format("%02d:00", horaInicioSeleccionada);
        String hFin = String.format("%02d:00", horaInicioSeleccionada + 1);

        apiService.verificarDisponibilidad(pistaActual.getIdPista(), fechaStr, hIni, hFin)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        ocultarLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            Object disponible = response.body().get("disponible");
                            boolean libre = disponible instanceof Boolean ? (Boolean) disponible : false;
                            if (libre) {
                                irAPantallaDePago();
                            } else {
                                Toast.makeText(DetallePistaActivity.this, "¡Vaya! Alguien acaba de reservar esa hora.", Toast.LENGTH_LONG).show();
                                cargarHorariosOcupados(); // Refrescar para mostrar la realidad
                            }
                        } else {
                            Toast.makeText(DetallePistaActivity.this,
                                    "Error del servidor (" + response.code() + "). Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        ocultarLoading();
                        Toast.makeText(DetallePistaActivity.this,
                                "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void irAPantallaDePago() {
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("EXTRA_PISTA", pistaActual);
        intent.putExtra("EXTRA_FECHA", fechaSeleccionada.toString());
        intent.putExtra("EXTRA_HORA_INICIO", horaInicioSeleccionada);
        startActivity(intent);
    }

    private void mostrarLoading(String msg) {
        loadingLayout.setVisibility(LinearLayout.VISIBLE);
        ((TextView)findViewById(R.id.tvLoadingMensaje)).setText(msg);
    }

    private void ocultarLoading() {
        loadingLayout.setVisibility(LinearLayout.GONE);
    }
}
