package com.wishport.frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.models.Usuario;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword;
    private Button btnRegistrar;
    private ProgressBar progressBar;
    private TextView tvMensaje;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPasswordRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressBar = findViewById(R.id.progressBar);
        tvMensaje = findViewById(R.id.tvMensaje);

        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Listener del botón
        btnRegistrar.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        // Obtener valores de los campos
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar campos
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        progressBar.setVisibility(View.VISIBLE);
        btnRegistrar.setEnabled(false);

        // Crear objeto Usuario (idUsuario es null porque lo genera la BD)
        Usuario nuevoUsuario = new Usuario(null, nombre, email, password);

        // Llamada a la API
        apiService.registrarUsuario(nuevoUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnRegistrar.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    // Registro exitoso
                    Toast.makeText(RegistroActivity.this,
                            "Cuenta creada correctamente",
                            Toast.LENGTH_SHORT).show();

                    // Ir a la pantalla de login
                    Intent intent = new Intent(RegistroActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else if (response.code() == 500) {
                    // Probablemente email duplicado
                    Toast.makeText(RegistroActivity.this,
                            "Este email ya está registrado. ¿Quieres iniciar sesión?",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Otro error
                    Toast.makeText(RegistroActivity.this,
                            "Error al registrar: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnRegistrar.setEnabled(true);

                Toast.makeText(RegistroActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
