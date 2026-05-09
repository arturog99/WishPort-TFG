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
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Usuario;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etTelefono;
    private Button btnActualizar;
    private ProgressBar progressBar;
    private ApiService apiService;
    private int idUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        etNombre = findViewById(R.id.etNombrePerfil);
        etEmail = findViewById(R.id.etEmailPerfil);
        etTelefono = findViewById(R.id.etTelefonoPerfil);
        btnActualizar = findViewById(R.id.btnActualizarPerfil);
        progressBar = findViewById(R.id.progressBarPerfil);

        apiService = RetrofitClient.getApiService();

        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario != -1) {
            cargarDatosUsuario();
        } else {
            Toast.makeText(this, "Error: Sesión no encontrada", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnActualizar.setOnClickListener(v -> actualizarPerfil());
    }

    private void cargarDatosUsuario() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.obtenerUsuarioPorId(idUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();
                    etNombre.setText(usuario.getNombre());
                    etEmail.setText(usuario.getEmail());
                    etTelefono.setText(usuario.getTelefono());
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PerfilActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarPerfil() {
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnActualizar.setEnabled(false);

        Usuario usuarioUpdate = new Usuario();
        usuarioUpdate.setNombre(nombre);
        usuarioUpdate.setTelefono(telefono);

        apiService.actualizarUsuario(idUsuario, usuarioUpdate).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(PerfilActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    
                    // Actualizar nombre en SharedPreferences también
                    SharedPreferences.Editor editor = getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit();
                    editor.putString("nombreUsuario", nombre);
                    editor.apply();
                } else {
                    Toast.makeText(PerfilActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
                Toast.makeText(PerfilActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
