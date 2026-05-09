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
import com.wishport.frontend.api.RetrofitClient;
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

public class DetallePistaActivity extends AppCompatActivity {

    public static final String EXTRA_PISTA = "extra_pista";
    private static final int HORA_APERTURA = 8;
    private static final int HORA_CIERRE = 22;
    private static final ZoneId EUROPE_MADRID = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextView tvNombre, tvDeporte, tvEstado, tvId, tvFechaSeleccionada;
    private Button btnSeleccionarFecha, btnReservar;
    private GridLayout gridHorarios;
    private SwipeRefreshLayout swipeRefreshLayout;
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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshDetalle);

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
        btnReservar.setOnClickListener(v -> verificarYReservar());

        swipeRefreshLayout.setOnRefreshListener(this::cargarReservasExistentes);

        inicializarGridHorarios();
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
                    resetearBotonesHorarios();
                    cargarReservasExistentes();
                },
                fechaSeleccionada.getYear(),
                fechaSeleccionada.getMonthValue() - 1,
                fechaSeleccionada.getDayOfMonth()
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void cargarReservasExistentes() {
        if (pistaActual == null) return;

        swipeRefreshLayout.setRefreshing(true);
        String fechaStr = fechaSeleccionada.format(DATE_FORMATTER);

        apiService.obtenerReservasPorPistaYFecha(pistaActual.getIdPista(), fechaStr)
                .enqueue(new Callback<List<Reserva>>() {
                    @Override
                    public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            reservasExistentes.clear();
                            reservasExistentes.addAll(response.body());
                            actualizarHorariosDisponibles();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Reserva>> call, Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(DetallePistaActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetearBotonesHorarios() {
        horaInicioSeleccionada = -1;
        for (Button btn : botonesHorarios) {
            btn.setEnabled(false);
            btn.setAlpha(0.5f);
            btn.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void actualizarHorariosDisponibles() {
        ZonedDateTime ahora = ZonedDateTime.now(EUROPE_MADRID);
        int horaActual = ahora.getHour();
        boolean esHoy = fechaSeleccionada.equals(LocalDate.now(EUROPE_MADRID));

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
            if (reserva.getHoraInicio() != null) {
                if (hora == reserva.getHoraInicio().getHour()) return true;
            }
        }
        return false;
    }

    private void seleccionarHora(int hora) {
        horaInicioSeleccionada = hora;
        for (Button btn : botonesHorarios) {
            int btnHora = (int) btn.getTag();
            if (btnHora == hora) {
                btn.setBackgroundColor(Color.GREEN);
            } else if (!isHoraOcupada(btnHora)) {
                btn.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    private void verificarYReservar() {
        if (horaInicioSeleccionada == -1) {
            Toast.makeText(this, "Selecciona un horario", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarLoading("Verificando disponibilidad...");
        String fechaStr = fechaSeleccionada.format(DATE_FORMATTER);
        String hInicio = String.format("%02d:00", horaInicioSeleccionada);
        String hFin = String.format("%02d:00", horaInicioSeleccionada + 1);

        apiService.verificarDisponibilidad(pistaActual.getIdPista(), fechaStr, hInicio, hFin)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        ocultarLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            boolean disponible = (boolean) response.body().get("disponible");
                            if (disponible) {
                                irACheckout();
                            } else {
                                Toast.makeText(DetallePistaActivity.this, "Lo sentimos, este horario se acaba de ocupar", Toast.LENGTH_LONG).show();
                                cargarReservasExistentes();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        ocultarLoading();
                        Toast.makeText(DetallePistaActivity.this, "Error de verificación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void irACheckout() {
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("EXTRA_PISTA", pistaActual);
        intent.putExtra("EXTRA_FECHA", fechaSeleccionada.toString());
        intent.putExtra("EXTRA_HORA_INICIO", horaInicioSeleccionada);
        startActivity(intent);
    }

    private void mostrarLoading(String mensaje) {
        loadingLayout.setVisibility(LinearLayout.VISIBLE);
        ((TextView)findViewById(R.id.tvLoadingMensaje)).setText(mensaje);
    }

    private void ocultarLoading() {
        loadingLayout.setVisibility(LinearLayout.GONE);
    }
}
