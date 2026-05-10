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
 * Incluye validaciones de seguridad para email y contraseña.
 * Migrada a MVVM/Hilt para consistencia con LoginActivity.
 */
@AndroidEntryPoint
public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etTelefono;
    private Button btnRegistrar;
    private ProgressBar progressBar;

    // Regla de contraseña: mínimo 8 caracteres, debe contener al menos una letra y un número.
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    private AuthViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        btnRegistrar.setOnClickListener(v -> intentarRegistro());
        observarDatos();
    }

    private void observarDatos() {
        viewModel.registroResponse.observe(this, usuario -> {
            Toast.makeText(this, "¡Cuenta creada! Ya puedes iniciar sesión.", Toast.LENGTH_SHORT).show();
            finish();
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

    /**
     * Valida los datos introducidos y los envía al ViewModel para crear la cuenta.
     */
    private void intentarRegistro() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (!validarFormulario(nombre, email, password, telefono)) {
            return;
        }

        viewModel.registrar(nombre, email, password, telefono);
    }

    /**
     * Comprueba que todos los campos cumplan con los requisitos de formato y seguridad.
     */
    private boolean validarFormulario(String nombre, String email, String password, String telefono) {
        if (nombre.isEmpty()) {
            etNombre.setError("Escribe tu nombre completo");
            return false;
        }
        
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Introduce un email válido");
            return false;
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            etPassword.setError("Mínimo 8 caracteres, con letras y números");
            return false;
        }
        
        if (telefono.length() < 9) {
            etTelefono.setError("Introduce un número de teléfono válido");
            return false;
        }
        
        return true;
    }
}
