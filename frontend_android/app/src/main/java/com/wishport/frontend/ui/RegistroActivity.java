package com.wishport.frontend.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.wishport.frontend.R;
import com.wishport.frontend.ui.viewmodels.AuthViewModel;

import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * PANTALLA DE REGISTRO: Permite a los nuevos usuarios crear una cuenta.
 * Usa AuthViewModel para procesar el registro de forma segura mediante DTOs.
 */
@AndroidEntryPoint
public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etTelefono;
    private Button btnRegistrar;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    // Regla de seguridad: Mínimo 8 caracteres, al menos una letra y un número.
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializamos componentes
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPasswordRegistro);
        etTelefono = findViewById(R.id.etTelefono);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressBar = findViewById(R.id.progressBar);

        // Obtenemos el ViewModel inyectado por Hilt
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        btnRegistrar.setOnClickListener(v -> intentarRegistro());
        findViewById(R.id.btnVolverLogin).setOnClickListener(v -> finish());

        // Escuchamos las respuestas del servidor
        observarEstadoRegistro();
    }

    /**
     * Suscribe la actividad a los cambios del ViewModel (Éxito, Carga o Error).
     */
    private void observarEstadoRegistro() {
        viewModel.registroResponse.observe(this, usuario -> {
            Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
            finish(); // Volver al Login
        });

        viewModel.isLoading.observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegistrar.setEnabled(!loading);
        });

        viewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void intentarRegistro() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (validarDatos(nombre, email, password, telefono)) {
            // Delegamos al ViewModel la creación del RegistroRequest y la llamada a la API
            viewModel.registrar(nombre, email, password, telefono);
        }
    }

    private boolean validarDatos(String nombre, String email, String password, String telefono) {
        if (nombre.isEmpty()) { etNombre.setError("Escribe tu nombre"); return false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Email inválido"); return false; }
        if (!PASSWORD_PATTERN.matcher(password).matches()) { etPassword.setError("Contraseña poco segura"); return false; }
        if (telefono.length() < 9) { etTelefono.setError("Teléfono demasiado corto"); return false; }
        return true;
    }
}
