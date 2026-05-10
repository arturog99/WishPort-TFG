package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE PAGO (Checkout): Simula el proceso de pago y confirma la reserva.
 * Incluye validaciones visuales para la tarjeta y envía los datos finales al servidor.
 */
@AndroidEntryPoint
public class CheckoutActivity extends AppCompatActivity {

    // Componentes de la interfaz
    private TextView tvResumenPista, tvResumenFechaHora;
    private EditText etTitular, etNumTarjeta, etCaducidad, etCVV;
    private Button btnPagar;
    private ProgressBar progressBarPago;

    // Datos de la reserva recibidos de la pantalla anterior
    private Pista pista;
    private String fechaSeleccionadaStr;
    private int horaInicio;

    @Inject ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // 1. Enlazamos los componentes del XML
        vincularVistas();

        // 2. Recuperamos los datos que nos pasó DetallePistaActivity
        pista = (Pista) getIntent().getSerializableExtra("EXTRA_PISTA");
        fechaSeleccionadaStr = getIntent().getStringExtra("EXTRA_FECHA");
        horaInicio = getIntent().getIntExtra("EXTRA_HORA_INICIO", -1);

        // 3. Configuramos máscaras para que los campos de tarjeta se formateen solos
        configurarFormateadores();

        // 4. Mostramos el resumen de lo que el usuario va a pagar
        mostrarResumen();

        // 5. Acción al pulsar el botón de pago
        btnPagar.setOnClickListener(v -> procesarPago());
        findViewById(R.id.btnCancelarCheckout).setOnClickListener(v -> finish());
    }

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

    private void mostrarResumen() {
        if (pista != null && fechaSeleccionadaStr != null) {
            tvResumenPista.setText(pista.getNombre() + " (" + pista.getDeporte() + ")");
            
            String hIni = String.format("%02d:00", horaInicio);
            String hFin = String.format("%02d:00", horaInicio + 1);
            tvResumenFechaHora.setText("Día: " + fechaSeleccionadaStr + "\nHora: " + hIni + " - " + hFin);
        }
    }

    /**
     * Añade "escuchadores" a los campos de texto para mejorar la experiencia (UX).
     * Por ejemplo, añade espacios en la tarjeta o la barra en la fecha automáticamente.
     */
    private void configurarFormateadores() {
        // Formatear número de tarjeta: 0000 0000 0000 0000
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

        // Formatear caducidad: MM/YY
        etCaducidad.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 2 && !s.toString().contains("/")) s.append("/");
            }
        });
    }

    /** Verifica que los campos de la tarjeta no estén vacíos y tengan el formato correcto */
    private boolean validarCampos() {
        String titular = etTitular.getText().toString().trim();
        String num = etNumTarjeta.getText().toString().replace(" ", "");
        String cad = etCaducidad.getText().toString().trim();
        String cvv = etCVV.getText().toString().trim();

        if (titular.isEmpty()) { etTitular.setError("Nombre obligatorio"); return false; }
        if (num.length() != 16) { etNumTarjeta.setError("Deben ser 16 números"); return false; }
        if (!cad.matches("(0[1-9]|1[0-2])/[0-9]{2}")) { etCaducidad.setError("Usa formato MM/YY"); return false; }
        if (cvv.length() < 3) { etCVV.setError("CVV inválido"); return false; }
        
        return true;
    }

    /** Realiza la petición final al servidor para guardar la reserva */
    private void procesarPago() {
        if (!validarCampos()) return;

        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) {
            Toast.makeText(this, "Tu sesión ha expirado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bloqueamos el botón para evitar que el usuario pulse dos veces
        btnPagar.setEnabled(false);
        progressBarPago.setVisibility(View.VISIBLE);

        // Preparamos el objeto reserva con toda la info recogida
        Usuario usuarioReserva = new Usuario();
        usuarioReserva.setIdUsuario(idUsuario);

        Reserva nuevaReserva = new Reserva();
        nuevaReserva.setFecha(LocalDate.parse(fechaSeleccionadaStr));
        nuevaReserva.setHoraInicio(LocalTime.of(horaInicio, 0));
        nuevaReserva.setHoraFin(LocalTime.of(horaInicio + 1, 0));
        nuevaReserva.setIdPista(pista);
        nuevaReserva.setIdUsuario(usuarioReserva);
        nuevaReserva.setEstadoReserva("activa");

        // Llamamos a la API (apiService inyectado por Hilt)
        apiService.crearReserva(nuevaReserva).enqueue(new Callback<Reserva>() {
            @Override
            public void onResponse(Call<Reserva> call, Response<Reserva> response) {
                progressBarPago.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CheckoutActivity.this, "¡Pago realizado con éxito!", Toast.LENGTH_SHORT).show();
                    
                    // Si todo va bien, vamos a la pantalla de detalle para mostrar el QR
                    Intent intent = new Intent(CheckoutActivity.this, DetalleReservaActivity.class);
                    intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, response.body());
                    startActivity(intent);
                    finish(); // Cerramos esta pantalla para que no pueda volver al pago
                } else {
                    btnPagar.setEnabled(true);
                    String msg = "Error del servidor (" + response.code() + ")";
                    if (response.code() == 409) {
                        msg = "El horario ya no está disponible";
                    } else if (response.code() == 401 || response.code() == 403) {
                        msg = "Sesión expirada. Vuelve a iniciar sesión.";
                    }
                    Toast.makeText(CheckoutActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Reserva> call, Throwable t) {
                progressBarPago.setVisibility(View.GONE);
                btnPagar.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
