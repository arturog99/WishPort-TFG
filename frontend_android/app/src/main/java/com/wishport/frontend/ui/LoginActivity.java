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
 * PANTALLA DE LOGIN: Gestiona el acceso y el auto-login.
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. AUTO-LOGIN: Verificamos sesión
        if (tieneSesionActiva()) {
            irAPantallaPrincipal();
            return; // IMPORTANTE: Detener ejecución aquí para evitar crashes
        }

        setContentView(R.layout.activity_pantalla_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        vincularVistas();
        configurarEventos();
        observarDatos();
    }

    private boolean tieneSesionActiva() {
        String savedToken = TokenManager.getToken(this);
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", null);
        return savedToken != null && rol != null;
    }

    private void irAPantallaPrincipal() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", "");
        Intent intent = "ADMIN".equals(rol) 
                ? new Intent(this, AdminActivity.class) 
                : new Intent(this, PistasActivity.class);
        startActivity(intent);
        finish();
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

    private void observarDatos() {
        viewModel.loginResponse.observe(this, respuesta -> {
            String token = (String) respuesta.get("token");
            String nombre = (String) respuesta.get("nombre");
            String email = (String) respuesta.get("email");
            String rol = (String) respuesta.get("rol");
            
            Object idObj = respuesta.get("idUsuario");
            int id = (idObj instanceof Number) ? ((Number) idObj).intValue() : -1;

            TokenManager.setToken(this, token);
            guardarDatosLocales(id, nombre, email, rol);
            irAPantallaPrincipal();
        });

        viewModel.error.observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    private void guardarDatosLocales(int id, String nombre, String email, String rol) {
        getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit()
                .putInt("idUsuario", id)
                .putString("nombreUsuario", nombre)
                .putString("emailUsuario", email)
                .putString("rolUsuario", rol)
                .apply();
    }
}
