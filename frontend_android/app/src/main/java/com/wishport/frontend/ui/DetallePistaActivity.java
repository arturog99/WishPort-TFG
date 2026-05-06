package com.wishport.frontend.ui;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetallePistaActivity extends AppCompatActivity {

    public static final String EXTRA_PISTA = "extra_pista";
    private static final int HORA_APERTURA = 8;
    private static final int HORA_CIERRE = 22;
    private static final ZoneId EUROPE_MADRID = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextView tvNombre, tvDeporte, tvEstado, tvId, tvFechaSeleccionada;
    private Button btnSeleccionarFecha, btnReservar;
    private GridLayout gridHorarios;
    private Pista pistaActual;
    private ApiService apiService;
    private LocalDate fechaSeleccionada;
    private int horaInicioSeleccionada = -1;
    private List<Reserva> reservasExistentes = new ArrayList<>();
    private List<Button> botonesHorarios = new ArrayList<>();
    private LinearLayout loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_pista);

        tvNombre = findViewById(R.id.tvDetalleNombre);
        tvDeporte = findViewById(R.id.tvDetalleDeporte);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvId = findViewById(R.id.tvDetalleId);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnReservar = findViewById(R.id.btnReservar);
        gridHorarios = findViewById(R.id.gridHorarios);
        loadingLayout = findViewById(R.id.loadingLayout);

        // Usar RetrofitClient centralizado con adapters java.time
        apiService = RetrofitClient.getApiService();

        pistaActual = (Pista) getIntent().getSerializableExtra(EXTRA_PISTA);
        fechaSeleccionada = LocalDate.now(EUROPE_MADRID);

        if (pistaActual != null) {
            tvNombre.setText(pistaActual.getNombre());
            tvDeporte.setText("Deporte: " + pistaActual.getDeporte());
            tvEstado.setText("Estado: " + pistaActual.getEstado());
            tvId.setText("ID: " + pistaActual.getIdPista());
        }

        btnSeleccionarFecha.setOnClickListener(v -> mostrarDatePicker());
        btnReservar.setOnClickListener(v -> crearReserva());

        // Inicializar grid de horarios
        inicializarGridHorarios();

        // Limpiar lista al iniciar (por si hay datos de sesión anterior)
        reservasExistentes.clear();

        // Cargar reservas de la fecha actual al iniciar
        tvFechaSeleccionada.setText("Fecha: " + fechaSeleccionada.format(DATE_FORMATTER));
        cargarReservasExistentes();
    }

    private void inicializarGridHorarios() {
        gridHorarios.removeAllViews();
        botonesHorarios.clear();

        for (int hora = HORA_APERTURA; hora < HORA_CIERRE; hora++) {
            Button btnHora = new Button(this);
            btnHora.setText(String.format("%02d:00", hora));
            btnHora.setTag(hora);
            btnHora.setEnabled(false);
            btnHora.setAlpha(0.5f);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            btnHora.setLayoutParams(params);

            final int horaFinal = hora;
            btnHora.setOnClickListener(v -> seleccionarHora(horaFinal));

            botonesHorarios.add(btnHora);
            gridHorarios.addView(btnHora);
        }
    }

    private void mostrarDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    fechaSeleccionada = LocalDate.of(year, month + 1, dayOfMonth);
                    tvFechaSeleccionada.setText("Fecha: " + fechaSeleccionada.format(DATE_FORMATTER));

                    // LIMPIAR BOTONES INMEDIATAMENTE - antes de cargar datos
                    resetearBotonesHorarios();

                    // Ahora cargar reservas del servidor
                    cargarReservasExistentes();
                },
                fechaSeleccionada.getYear(),
                fechaSeleccionada.getMonthValue() - 1,
                fechaSeleccionada.getDayOfMonth()
        );

        // No permitir fechas pasadas
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private long loadingStartTime = 0;
    private static final long MIN_LOADING_TIME = 500; // Mínimo 500ms visible

    private void cargarReservasExistentes() {
        if (pistaActual == null) return;

        // Mostrar pantalla de carga
        loadingStartTime = System.currentTimeMillis();
        mostrarLoading("Cargando horarios disponibles...");

        String fechaStr = fechaSeleccionada.format(DATE_FORMATTER);

        apiService.obtenerReservasPorPistaYFecha(pistaActual.getIdPista(), fechaStr)
                .enqueue(new Callback<List<Reserva>>() {
                    @Override
                    public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            reservasExistentes.clear();
                            reservasExistentes.addAll(response.body());
                            actualizarHorariosDisponibles();
                        } else {
                            reservasExistentes.clear();
                            actualizarHorariosDisponibles();
                        }
                        ocultarLoadingConDelay();
                    }

                    @Override
                    public void onFailure(Call<List<Reserva>> call, Throwable t) {
                        reservasExistentes.clear();
                        actualizarHorariosDisponibles();
                        ocultarLoadingConDelay();
                    }
                });
    }

    private void ocultarLoadingConDelay() {
        long elapsed = System.currentTimeMillis() - loadingStartTime;
        long remaining = MIN_LOADING_TIME - elapsed;

        if (remaining > 0) {
            // Esperar el tiempo restante para que se vea el loading
            loadingLayout.postDelayed(() -> ocultarLoading(), remaining);
        } else {
            ocultarLoading();
        }
    }

    private TextView tvLoadingMensaje;

    private void mostrarLoading(String mensaje) {
        if (loadingLayout == null) {
            return;
        }
        runOnUiThread(() -> {
            if (tvLoadingMensaje == null) {
                tvLoadingMensaje = findViewById(R.id.tvLoadingMensaje);
            }
            if (tvLoadingMensaje != null && mensaje != null) {
                tvLoadingMensaje.setText(mensaje);
            }
            loadingLayout.setVisibility(LinearLayout.VISIBLE);
        });
    }

    private void ocultarLoading() {
        runOnUiThread(() -> {
            loadingLayout.setVisibility(LinearLayout.GONE);
        });
    }

    private void resetearBotonesHorarios() {
        horaInicioSeleccionada = -1;
        reservasExistentes.clear(); // Limpiar reservas antiguas inmediatamente

        for (Button btn : botonesHorarios) {
            btn.setEnabled(false); // Deshabilitar hasta saber si están disponibles
            btn.setAlpha(0.5f);
            btn.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void actualizarHorariosDisponibles() {
        horaInicioSeleccionada = -1;

        ZonedDateTime ahora = ZonedDateTime.now(EUROPE_MADRID);
        int horaActual = ahora.getHour();

        LocalDate hoy = LocalDate.now(EUROPE_MADRID);
        boolean esHoy = fechaSeleccionada.equals(hoy);

        for (Button btn : botonesHorarios) {
            int hora = (int) btn.getTag();
            boolean ocupado = isHoraOcupada(hora);
            boolean horaPasada = esHoy && hora <= horaActual;

            if (ocupado || horaPasada) {
                btn.setEnabled(false);
                btn.setAlpha(0.3f);
                btn.setBackgroundColor(Color.GRAY);
            } else {
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
                btn.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    private boolean isHoraOcupada(int hora) {
        for (Reserva reserva : reservasExistentes) {
            if (reserva.getHoraInicio() != null && reserva.getHoraFin() != null) {
                int reservaHoraInicio = reserva.getHoraInicio().getHour();
                int reservaHoraFin = reserva.getHoraFin().getHour();

                if (hora >= reservaHoraInicio && hora < reservaHoraFin) {
                    return true;
                }
            }
        }
        return false;
    }

    private void seleccionarHora(int hora) {
        horaInicioSeleccionada = hora;
        
        // Actualizar visual de botones
        for (Button btn : botonesHorarios) {
            int btnHora = (int) btn.getTag();
            if (btnHora == hora) {
                btn.setBackgroundColor(Color.GREEN);
            } else if (!isHoraOcupada(btnHora)) {
                btn.setBackgroundColor(Color.LTGRAY);
            }
        }
        
        Toast.makeText(this, "Horario seleccionado: " + String.format("%02d:00 - %02d:00", hora, hora + 1), Toast.LENGTH_SHORT).show();
    }

    private void crearReserva() {
        if (pistaActual == null) {
            Toast.makeText(this, "Error: No hay pista seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horaInicioSeleccionada == -1) {
            Toast.makeText(this, "Selecciona un horario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener usuario de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) {
            Toast.makeText(this, "Error: Usuario no logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el horario no esté ocupado
        if (isHoraOcupada(horaInicioSeleccionada)) {
            Toast.makeText(this, "Este horario ya está ocupado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objetos Usuario y Pista con IDs
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);

        // Crear reserva con java.time
        Reserva reserva = new Reserva();
        reserva.setFecha(fechaSeleccionada);  // LocalDate
        reserva.setHoraInicio(LocalTime.of(horaInicioSeleccionada, 0));  // LocalTime
        reserva.setHoraFin(LocalTime.of(horaInicioSeleccionada + 1, 0));  // LocalTime
        reserva.setIdPista(pistaActual);
        reserva.setIdUsuario(usuario);
        reserva.setEstadoReserva("activa");

        // Mostrar pantalla de carga
        mostrarLoading("Procesando reserva...");

        apiService.crearReserva(reserva).enqueue(new Callback<Reserva>() {
            @Override
            public void onResponse(Call<Reserva> call, Response<Reserva> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cargarReservasExistentes();
                    Toast.makeText(DetallePistaActivity.this, "¡Reserva creada!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 409) {
                    // El backend dice que el horario está ocupado
                    Toast.makeText(DetallePistaActivity.this,
                            "Este horario ya fue reservado por otro usuario",
                            Toast.LENGTH_LONG).show();
                    // Recargar reservas para actualizar la UI
                    cargarReservasExistentes();

                } else if (response.code() == 403) {
                        Toast.makeText(DetallePistaActivity.this,
                                "¡Límite alcanzado! No puedes tener más de 2 reservas activas.",
                                Toast.LENGTH_LONG).show();


                } else {
                    Toast.makeText(DetallePistaActivity.this,
                            "Error al crear reserva: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
                // Ocultar pantalla de carga
                ocultarLoading();
            }

            @Override
            public void onFailure(Call<Reserva> call, Throwable t) {
                Toast.makeText(DetallePistaActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                // Ocultar pantalla de carga
                ocultarLoading();
            }
        });


    }
}
