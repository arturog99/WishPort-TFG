package com.wishport.frontend.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

/**
 * PANTALLA DE REGISTRO: Permite a los nuevos usuarios crear una cuenta.
 * Incluye validaciones de seguridad para email y contraseña.
 */
public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etTelefono;
    private Button btnRegistrar;
    private ProgressBar progressBar;
    
    // Regla de contraseña: mínimo 8 caracteres, debe contener al menos una letra y un número.
    private static final Pattern PASSWORD_PATTERN = 
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Vincular componentes de la interfaz
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPasswordRegistro);
        etTelefono = findViewById(R.id.etTelefono);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressBar = findViewById(R.id.progressBar);

        apiService = RetrofitClient.getApiService();

        btnRegistrar.setOnClickListener(v -> intentarRegistro());
    }

    /**
     * Valida los datos introducidos y los envía al servidor para crear la cuenta.
     */
    private void intentarRegistro() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        // Ejecutar validaciones antes de llamar a la API
        if (!validarFormulario(nombre, email, password, telefono)) {
            return;
        }

        // Mostrar indicador de carga y bloquear botón
        progressBar.setVisibility(View.VISIBLE);
        btnRegistrar.setEnabled(false);

        // Crear el objeto usuario (el ID lo generará la base de datos)
        Usuario nuevoUsuario = new Usuario(null, nombre, email, password, telefono);

        apiService.registrarUsuario(nuevoUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnRegistrar.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(RegistroActivity.this, "¡Cuenta creada! Ya puedes iniciar sesión.", Toast.LENGTH_SHORT).show();
                    finish(); // Volver al Login
                } else {
                    Toast.makeText(RegistroActivity.this, "Error: Este email ya está en uso.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnRegistrar.setEnabled(true);
                Toast.makeText(RegistroActivity.this, "Error de conexión con el servidor", Toast.LENGTH_LONG).show();
            }
        });
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
