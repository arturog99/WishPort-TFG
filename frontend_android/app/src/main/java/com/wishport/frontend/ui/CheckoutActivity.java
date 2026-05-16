package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.TokenManager;
import javax.inject.Inject;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE CHECKOUT (Pago): Último paso del flujo de reserva.
 *
 * Recibe desde DetallePistaActivity: la Pista, la fecha y la hora de inicio.
 * Muestra un resumen de lo que se va a reservar y un formulario de pago.
 *
 * NOTA: El pago es simulado (no hay pasarela real). Solo se valida que el
 * número de tarjeta tenga 16 dígitos antes de enviar la reserva al servidor.
 * El objetivo es ilustrar el flujo completo, no implementar un TPV real.
 *
 * Al confirmar, construye el objeto Reserva y lo manda al backend via
 * POST /api/reservas. Si el servidor lo acepta, navega a DetalleReservaActivity.
 */
@AndroidEntryPoint
public class CheckoutActivity extends AppCompatActivity {

    /** Textos que muestran el resumen de la reserva (pista, fecha y hora) */
    private TextView tvResumenPista, tvResumenFechaHora;
    /** Campos del formulario de pago simulado */
    private EditText etTitular, etNumTarjeta, etCaducidad, etCVV;
    /** Botón que confirma y envía la reserva al servidor */
    private Button btnPagar;
    /** Indicador de carga mientras se procesa la petición */
    private ProgressBar progressBarPago;

    /** Cliente API inyectado por Hilt (incluye AuthInterceptor con JWT) */
    @Inject
    ApiService apiService;

    /** Pista recibida desde DetallePistaActivity */
    private Pista pista;
    /** Fecha en formato yyyy-MM-dd, recibida como String desde DetallePistaActivity */
    private String fechaSeleccionadaStr;
    /** Hora de inicio (ej: 18 para 18:00), recibida desde DetallePistaActivity */
    private int horaInicio;

    /**
     * Inicializa la pantalla de checkout:
     * 1. Vincula las vistas del XML.
     * 2. Recoge la pista, fecha y hora del Intent.
     * 3. Configura los formateadores automáticos del formulario.
     * 4. Muestra el resumen de la reserva.
     * 5. Configura el botón de pagar.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        vincularVistas();

        pista = (Pista) getIntent().getSerializableExtra("EXTRA_PISTA");
        fechaSeleccionadaStr = getIntent().getStringExtra("EXTRA_FECHA");
        horaInicio = getIntent().getIntExtra("EXTRA_HORA_INICIO", -1);

        configurarFormateadores();

        if (pista != null && fechaSeleccionadaStr != null) {
            tvResumenPista.setText(pista.getNombre() + " (" + pista.getDeporte() + ")");
            String hIni = String.format("%02d:00", horaInicio);
            String hFin = String.format("%02d:00", horaInicio + 1);
            tvResumenFechaHora.setText("Día: " + fechaSeleccionadaStr + "\nHora: " + hIni + " - " + hFin);
        }

        btnPagar.setOnClickListener(v -> procesarPago());
    }

    /** Vincula las variables con los elementos del XML activity_checkout */
    private void vincularVistas() {
        tvResumenPista = findViewById(R.id.tvResumenPista);
        tvResumenFechaHora = findViewById(R.id.tvResumenFechaHora);
        etTitular = findViewById(R.id.etTitular);
        etNumTarjeta = findViewById(R.id.etNumTarjeta);
        etCaducidad = findViewById(R.id.etCaducidad);
        etCVV = findViewById(R.id.etCVV);
        btnPagar = findViewById(R.id.btnPagar);
        progressBarPago = findViewById(R.id.progressBarPago);
    }

    /**
     * Añade formateadores automáticos a los campos de tarjeta:
     *
     * Número de tarjeta: añade un espacio automáticamente cada 4 dígitos
     * para mostrar el formato típico: "1234 5678 9012 3456".
     *
     * Caducidad: inserta la barra "/" automáticamente tras los 2 primeros
     * dígitos para mostrar el formato: "05/28".
     *
     * Estos TextWatcher escuchan cada cambio en el texto y aplican el formato.
     */
    private void configurarFormateadores() {
        etNumTarjeta.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String original = s.toString().replace(" ", "");
                if (original.length() > 0 && original.length() % 4 == 0 && original.length() < 16) {
                    if (s.charAt(s.length() - 1) != ' ') s.append(" ");
                }
            }
        });

        etCaducidad.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 2 && !s.toString().contains("/")) s.append("/");
            }
        });
    }

    /**
     * Gestiona la confirmación de la reserva:
     * 1. Valida que el número de tarjeta tenga 16 dígitos (sin espacios).
     * 2. Obtiene el idUsuario de SharedPreferences.
     * 3. Construye el objeto Reserva con: fecha, horaInicio, horaFin, pista y usuario.
     * 4. Lo envía al backend via POST /api/reservas.
     * 5. Si el servidor responde 201: navega a DetalleReservaActivity con la reserva creada.
     * 6. Si hay error: muestra mensaje y rehabilita el botón.
     *
     * NOTA: El codigoQr y el id son generados por el servidor, no por el cliente.
     */
    private void procesarPago() {
        if (etNumTarjeta.getText().toString().replace(" ", "").length() != 16) {
            etNumTarjeta.setError("16 dígitos");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Log.e("CheckoutActivity", "ERROR: No hay sesión activa (idUsuario = -1)");
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_LONG).show();
            return;
        }

        // DEBUG: Verificar token JWT
        String token = TokenManager.getToken(this);
        Log.d("CheckoutActivity", "Token JWT: " + (token != null ? "PRESENTE (" + token.substring(0, Math.min(20, token.length())) + "...)" : "AUSENTE"));

        btnPagar.setEnabled(false);
        progressBarPago.setVisibility(View.VISIBLE);

        // Creamos un Usuario solo con el id para que el backend sepa a quién asignar la reserva
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);

        // Construimos la Reserva con todos los datos necesarios
        Reserva reserva = new Reserva();
        reserva.setFecha(LocalDate.parse(fechaSeleccionadaStr)); // String -> LocalDate
        reserva.setHoraInicio(LocalTime.of(horaInicio, 0));      // int -> LocalTime
        reserva.setHoraFin(LocalTime.of(horaInicio + 1, 0));     // Siempre 1 hora
        reserva.setIdPista(pista);
        reserva.setIdUsuario(usuario);
        reserva.setEstadoReserva("activa");

        // Enviamos la reserva al servidor usando el ApiService inyectado (con token JWT)
        apiService.crearReserva(reserva).enqueue(new Callback<Reserva>() {
            @Override
            public void onResponse(Call<Reserva> call, Response<Reserva> response) {
                progressBarPago.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    // Reserva creada: navegamos a ver el detalle con el QR
                    Intent intent = new Intent(CheckoutActivity.this, DetalleReservaActivity.class);
                    intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, response.body());
                    startActivity(intent);
                    finish(); // No permitir volver a esta pantalla (ya reservado)
                } else {
                    btnPagar.setEnabled(true);
                    String msg = "Error " + response.code() + " en la reserva";
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("CheckoutActivity", "Error body: " + errorBody);
                        JSONObject json = new JSONObject(errorBody);
                        if (json.has("mensaje")) {
                            msg = json.getString("mensaje");
                        } else if (json.has("error")) {
                            msg = json.getString("error") + ": " + msg;
                        }
                    } catch (Exception e) {
                        Log.e("CheckoutActivity", "Error parseando respuesta: " + e.getMessage());
                    }
                    Toast.makeText(CheckoutActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Reserva> call, Throwable t) {
                progressBarPago.setVisibility(View.GONE);
                btnPagar.setEnabled(true);
                Log.e("CheckoutActivity", "Error de conexión: " + t.getMessage(), t);
                Toast.makeText(CheckoutActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
