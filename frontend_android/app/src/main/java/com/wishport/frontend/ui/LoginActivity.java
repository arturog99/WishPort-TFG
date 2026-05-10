package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.wishport.frontend.R;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.ui.viewmodels.AuthViewModel;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * PANTALLA DE LOGIN (Migrada a MVVM): Punto de entrada a la aplicación.
 * Gestiona el acceso de usuarios delegando la lógica al AuthViewModel.
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. AUTO-LOGIN: Verificación rápida de sesión activa
        verificarSesionActiva();

        setContentView(R.layout.activity_pantalla_login);

        // 2. Inicializar el ViewModel (Inyectado por Hilt)
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        vincularVistas();
        configurarEventos();
        observarDatos();
    }

    private void verificarSesionActiva() {
        String savedToken = TokenManager.getToken(this);
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", null);

        if (savedToken != null && rol != null) {
            irAPantallaPrincipal(rol);
        }
    }

    private void vincularVistas() {
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        
        findViewById(R.id.btnIrARegistro).setOnClickListener(v -> 
            startActivity(new Intent(this, RegistroActivity.class)));
    }

    private void configurarEventos() {
        btnLogin.setOnClickListener(view -> {
            String email = etUsuario.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.login(email, pass);
            }
        });
    }

    /**
     * OBSERVAR DATOS: Reaccionamos a los cambios en el estado del Login.
     */
    private void observarDatos() {
        // Observar respuesta exitosa
        viewModel.loginResponse.observe(this, respuesta -> {
            String token = (String) respuesta.get("token");
            String nombre = (String) respuesta.get("nombre");
            String email = (String) respuesta.get("email");
            String rol = (String) respuesta.get("rol");
            
            Double idD = (Double) respuesta.get("idUsuario");
            int id = idD != null ? idD.intValue() : -1;

            // Guardar sesión
            TokenManager.setToken(this, token);
            guardarDatosLocales(id, nombre, email, rol);

            Toast.makeText(this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
            irAPantallaPrincipal(rol);
        });

        // Observar estado de carga (podrías añadir un ProgressBar aquí)
        viewModel.isLoading.observe(this, loading -> {
            btnLogin.setEnabled(!loading);
        });

        // Observar errores
        viewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarDatosLocales(int id, String nombre, String email, String rol) {
        SharedPreferences.Editor editor = getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit();
        editor.putInt("idUsuario", id);
        editor.putString("nombreUsuario", nombre);
        editor.putString("emailUsuario", email);
        editor.putString("rolUsuario", rol);
        editor.apply();
    }

    private void irAPantallaPrincipal(String rol) {
        Intent intent = "ADMIN".equals(rol) 
                ? new Intent(this, AdminActivity.class) 
                : new Intent(this, PistasActivity.class);
        startActivity(intent);
        finish();
    }
}
