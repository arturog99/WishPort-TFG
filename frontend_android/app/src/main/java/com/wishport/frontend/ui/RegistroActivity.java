package com.wishport.frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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
import com.wishport.frontend.models.Usuario;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etTelefono;
    private Button btnRegistrar;
    private ProgressBar progressBar;
    
    // Regla de contraseña: mínimo 8 caracteres, una letra y un número
    private static final Pattern PASSWORD_PATTERN = 
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPasswordRegistro);
        etTelefono = findViewById(R.id.etTelefono);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressBar = findViewById(R.id.progressBar);

        apiService = RetrofitClient.getApiService();

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (!validarCampos(nombre, email, password, telefono)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegistrar.setEnabled(false);

        Usuario nuevoUsuario = new Usuario(null, nombre, email, password, telefono);

        apiService.registrarUsuario(nuevoUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnRegistrar.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(RegistroActivity.this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegistroActivity.this, "Error: El email ya está registrado", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnRegistrar.setEnabled(true);
                Toast.makeText(RegistroActivity.this, "Error de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validarCampos(String nombre, String email, String password, String telefono) {
        if (nombre.isEmpty()) {
            etNombre.setError("El nombre es obligatorio");
            return false;
        }
        
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Introduce un email válido");
            return false;
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            etPassword.setError("La contraseña debe tener al menos 8 caracteres, letras y números");
            return false;
        }
        
        if (telefono.length() < 9) {
            etTelefono.setError("Introduce un teléfono válido");
            return false;
        }
        
        return true;
    }
}
