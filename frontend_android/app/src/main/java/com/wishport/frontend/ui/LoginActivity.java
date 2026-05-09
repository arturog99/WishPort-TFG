package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.models.Usuario;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin, btnIrARegistro;
    EditText etUsuario, etPassword;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Verificar si ya hay una sesión activa (Auto-Login)
        String savedToken = TokenManager.getToken(this);
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", null);

        if (savedToken != null && rol != null) {
            irAPantallaPrincipal(rol);
            return;
        }

        setContentView(R.layout.activity_pantalla_login);

        btnLogin = findViewById(R.id.btnLogin);
        btnIrARegistro = findViewById(R.id.btnIrARegistro);
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);

        apiService = RetrofitClient.getApiService();

        btnLogin.setOnClickListener(view -> loginUsuario());
        btnIrARegistro.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    private void loginUsuario() {
        String email = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introduce email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario credenciales = new Usuario();
        credenciales.setEmail(email);
        credenciales.setPassword(password);

        apiService.login(credenciales).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> respuesta = response.body();

                    String token = (String) respuesta.get("token");
                    String nombre = (String) respuesta.get("nombre");
                    String emailRes = (String) respuesta.get("email");
                    String rol = (String) respuesta.get("rol");
                    Double idUsuarioDouble = (Double) respuesta.get("idUsuario");
                    int idUsuario = idUsuarioDouble != null ? idUsuarioDouble.intValue() : -1;

                    // Guardar token persistente
                    TokenManager.setToken(LoginActivity.this, token);

                    // Guardar datos de usuario
                    SharedPreferences.Editor editor = getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit();
                    editor.putInt("idUsuario", idUsuario);
                    editor.putString("nombreUsuario", nombre);
                    editor.putString("emailUsuario", emailRes);
                    editor.putString("rolUsuario", rol);
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
                    irAPantallaPrincipal(rol);
                } else {
                    Toast.makeText(LoginActivity.this, "Error: Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAPantallaPrincipal(String rol) {
        Intent intent;
        if ("ADMIN".equals(rol)) {
            intent = new Intent(LoginActivity.this, AdminActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, PistasActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
