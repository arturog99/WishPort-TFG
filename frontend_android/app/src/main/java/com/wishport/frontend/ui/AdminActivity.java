package com.wishport.frontend.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wishport.frontend.R;
import com.wishport.frontend.adapters.ReservaAdapter;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.models.Reserva;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerViewReservas;
    private ReservaAdapter reservaAdapter;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private FloatingActionButton btnEscanearQr;

    private ApiService apiService;
    private List<Reserva> reservasHoy = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", "");
        if (!"ADMIN".equals(rol)) {
            Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerViewReservas = findViewById(R.id.recyclerViewReservasHoy);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnEscanearQr = findViewById(R.id.btnEscanearQr);

        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(new ArrayList<>());
        recyclerViewReservas.setAdapter(reservaAdapter);

        apiService = RetrofitClient.getApiService();

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
        btnEscanearQr.setOnClickListener(v -> iniciarEscaneoQR());

        cargarReservasDelDia();
    }

    private void cargarReservasDelDia() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);

        apiService.obtenerReservas().enqueue(new Callback<List<Reserva>>() {
            @Override
            public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Reserva> todasLasReservas = response.body();
                    LocalDate hoy = LocalDate.now();

                    reservasHoy = new ArrayList<>();
                    for (Reserva reserva : todasLasReservas) {
                        if (reserva.getFecha() != null && reserva.getFecha().equals(hoy)) {
                            reservasHoy.add(reserva);
                        }
                    }

                    if (reservasHoy.isEmpty()) {
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        recyclerViewReservas.setVisibility(View.GONE);
                    } else {
                        emptyStateLayout.setVisibility(View.GONE);
                        recyclerViewReservas.setVisibility(View.VISIBLE);
                        reservaAdapter.actualizarLista(reservasHoy);
                    }
                } else {
                    Toast.makeText(AdminActivity.this, "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Reserva>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminActivity.this, "Error de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void iniciarEscaneoQR() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escanea el código QR de la reserva");
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            validarQR(result.getContents());
        }
    }

    private void validarQR(String qrCode) {
        Reserva encontrada = null;
        for (Reserva r : reservasHoy) {
            if (qrCode.equals(r.getCodigoQr())) {
                encontrada = r;
                break;
            }
        }

        if (encontrada != null) {
            Toast.makeText(this, "✓ Reserva Válida: " + encontrada.getIdUsuario().getNombre(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "✗ QR Inválido o de otro día", Toast.LENGTH_LONG).show();
        }
    }

    private void logout() {
        // Limpiar SharedPreferences de usuario
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Limpiar Token JWT persistente
        TokenManager.clear(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
