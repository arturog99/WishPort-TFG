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
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE PAGO: Gestiona la reserva final.
 */
@AndroidEntryPoint
public class CheckoutActivity extends AppCompatActivity {

    private TextView tvResumenPista, tvResumenFechaHora;
    private EditText etTitular, etNumTarjeta, etCaducidad, etCVV;
    private Button btnPagar;
    private ProgressBar progressBarPago;

    private Pista pista;
    private String fechaSeleccionadaStr;
    private int horaInicio;

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

    private void procesarPago() {
        // Validación básica
        if (etNumTarjeta.getText().toString().replace(" ", "").length() != 16) {
            etNumTarjeta.setError("16 dígitos");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) return;

        btnPagar.setEnabled(false);
        progressBarPago.setVisibility(View.VISIBLE);

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);

        Reserva reserva = new Reserva();
        reserva.setFecha(LocalDate.parse(fechaSeleccionadaStr));
        reserva.setHoraInicio(LocalTime.of(horaInicio, 0));
        reserva.setHoraFin(LocalTime.of(horaInicio + 1, 0));
        reserva.setIdPista(pista);
        reserva.setIdUsuario(usuario);
        reserva.setEstadoReserva("activa");

        RetrofitClient.getApiService().crearReserva(reserva).enqueue(new Callback<Reserva>() {
            @Override
            public void onResponse(Call<Reserva> call, Response<Reserva> response) {
                progressBarPago.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(CheckoutActivity.this, DetalleReservaActivity.class);
                    intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, response.body());
                    startActivity(intent);
                    finish();
                } else {
                    btnPagar.setEnabled(true);
                    Toast.makeText(CheckoutActivity.this, "Error en la reserva", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Reserva> call, Throwable t) {
                progressBarPago.setVisibility(View.GONE);
                btnPagar.setEnabled(true);
            }
        });
    }
}
