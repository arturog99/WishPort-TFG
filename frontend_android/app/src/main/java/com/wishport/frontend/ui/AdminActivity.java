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

        // Verificar que es admin
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", "");
        if (!"ADMIN".equals(rol)) {
            Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar vistas
        recyclerViewReservas = findViewById(R.id.recyclerViewReservasHoy);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnEscanearQr = findViewById(R.id.btnEscanearQr);

        // Configurar RecyclerView
        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(new ArrayList<>());
        recyclerViewReservas.setAdapter(reservaAdapter);

        // Usar RetrofitClient
        apiService = RetrofitClient.getApiService();

        // Botón Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Botón Escanear QR
        btnEscanearQr.setOnClickListener(v -> iniciarEscaneoQR());

        // Cargar reservas del día
        cargarReservasDelDia();
    }

    private void iniciarEscaneoQR() {
        // Verificar permiso de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Iniciar escaneo QR
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escanea el código QR de la reserva");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarEscaneoQR();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
            } else {
                String qrCode = result.getContents();
                validarQR(qrCode);
            }
        }
    }

    private void validarQR(String qrCode) {
        // Buscar si el QR corresponde a una reserva de hoy
        Reserva reservaEncontrada = null;
        for (Reserva reserva : reservasHoy) {
            if (qrCode.equals(reserva.getCodigoQr())) {
                reservaEncontrada = reserva;
                break;
            }
        }

        if (reservaEncontrada != null) {
            String usuario = reservaEncontrada.getIdUsuario() != null ?
                    reservaEncontrada.getIdUsuario().getNombre() : "Usuario";
            String pista = reservaEncontrada.getIdPista() != null ?
                    reservaEncontrada.getIdPista().getDeporte() : "Pista";
            String hora = reservaEncontrada.getHoraInicio() != null ?
                    reservaEncontrada.getHoraInicio().toString() : "";

            Toast.makeText(this,
                    "✓ Reserva válida: " + usuario + " - " + pista + " " + hora,
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    "✗ QR inválido o no corresponde a una reserva de hoy",
                    Toast.LENGTH_LONG).show();
        }
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

                    // Filtrar reservas de hoy
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
                    Toast.makeText(AdminActivity.this,
                            "Error al cargar reservas: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Reserva>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("idUsuario");
        editor.remove("nombreUsuario");
        editor.remove("emailUsuario");
        editor.remove("rolUsuario");
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
