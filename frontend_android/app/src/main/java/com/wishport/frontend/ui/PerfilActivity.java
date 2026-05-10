package com.wishport.frontend.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.data.repository.AuthRepository;
import com.wishport.frontend.models.Usuario;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE PERFIL: Seleccionada para Hilt.
 * Gestiona la edición de datos del usuario inyectando el repositorio de autenticación.
 */
@AndroidEntryPoint
public class PerfilActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etTelefono;
    private Button btnActualizar;
    private ProgressBar progressBar;
    
    @Inject AuthRepository authRepository;
    private int idUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        vincularComponentes();

        // Carga inicial desde datos locales (UX rápida)
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        idUsuario = prefs.getInt("idUsuario", -1);
        etNombre.setText(prefs.getString("nombreUsuario", ""));
        etEmail.setText(prefs.getString("emailUsuario", ""));

        if (idUsuario != -1) {
            obtenerDatosServidor();
        }

        btnActualizar.setOnClickListener(v -> intentarActualizar());
    }

    private void vincularComponentes() {
        etNombre = findViewById(R.id.etNombrePerfil);
        etEmail = findViewById(R.id.etEmailPerfil);
        etTelefono = findViewById(R.id.etTelefonoPerfil);
        btnActualizar = findViewById(R.id.btnActualizarPerfil);
        progressBar = findViewById(R.id.progressBarPerfil);
    }

    private void obtenerDatosServidor() {
        progressBar.setVisibility(View.VISIBLE);
        authRepository.obtenerPerfil(idUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Usuario u = response.body();
                    etNombre.setText(u.getNombre());
                    etTelefono.setText(u.getTelefono());
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void intentarActualizar() {
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombre.setError("Nombre obligatorio");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnActualizar.setEnabled(false);

        Usuario update = new Usuario();
        update.setNombre(nombre);
        update.setTelefono(etTelefono.getText().toString().trim());

        authRepository.actualizarPerfil(idUsuario, update).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(PerfilActivity.this, "Perfil guardado", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit().putString("nombreUsuario", nombre).apply();
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
            }
        });
    }
}
