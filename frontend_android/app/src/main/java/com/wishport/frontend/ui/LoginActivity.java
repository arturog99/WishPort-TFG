package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

/**
 * PANTALLA DE LOGIN: Punto de entrada a la aplicación.
 * Gestiona el acceso de usuarios y la persistencia de la sesión.
 */
public class LoginActivity extends AppCompatActivity {

    private Button btnLogin, btnIrARegistro;
    private EditText etUsuario, etPassword;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. AUTO-LOGIN: Si ya tenemos el Token y el Rol guardados, entramos directamente.
        String savedToken = TokenManager.getToken(this);
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", null);

        if (savedToken != null && rol != null) {
            irAPantallaPrincipal(rol);
            return;
        }

        setContentView(R.layout.activity_pantalla_login);

        // Inicializar componentes de la interfaz
        btnLogin = findViewById(R.id.btnLogin);
        btnIrARegistro = findViewById(R.id.btnIrARegistro);
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);

        // Obtener el cliente de la API
        apiService = RetrofitClient.getApiService();

        // Configurar acciones de los botones
        btnLogin.setOnClickListener(view -> intentarLogin());
        btnIrARegistro.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
        });
    }

    /**
     * Recoge los datos del formulario y los envía al servidor para validar el acceso.
     */
    private void intentarLogin() {
        String email = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creamos un objeto con los datos de acceso
        Usuario credenciales = new Usuario();
        credenciales.setEmail(email);
        credenciales.setPassword(password);

        // Realizamos la petición al servidor
        apiService.login(credenciales).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> respuesta = response.body();

                    // Extraemos la información que nos devuelve el Backend
                    String token = (String) respuesta.get("token");
                    String nombre = (String) respuesta.get("nombre");
                    String emailRes = (String) respuesta.get("email");
                    String rol = (String) respuesta.get("rol");
                    
                    // Manejo del ID de usuario (Retrofit lo recibe como Double por defecto desde JSON)
                    Double idUsuarioDouble = (Double) respuesta.get("idUsuario");
                    int idUsuario = idUsuarioDouble != null ? idUsuarioDouble.intValue() : -1;

                    // GUARDAR SESIÓN: El token va cifrado y los datos básicos a SharedPreferences
                    TokenManager.setToken(LoginActivity.this, token);
                    guardarDatosLocalmente(idUsuario, nombre, emailRes, rol);

                    Toast.makeText(LoginActivity.this, "¡Hola de nuevo, " + nombre + "!", Toast.LENGTH_SHORT).show();
                    irAPantallaPrincipal(rol);
                } else {
                    Toast.makeText(LoginActivity.this, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Guarda la información básica del usuario para no tener que pedirla al servidor constantemente.
     */
    private void guardarDatosLocalmente(int id, String nombre, String email, String rol) {
        SharedPreferences.Editor editor = getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit();
        editor.putInt("idUsuario", id);
        editor.putString("nombreUsuario", nombre);
        editor.putString("emailUsuario", email);
        editor.putString("rolUsuario", rol);
        editor.apply();
    }

    /**
     * Redirige al usuario a su pantalla correspondiente según su nivel de acceso.
     */
    private void irAPantallaPrincipal(String rol) {
        Intent intent;
        if ("ADMIN".equals(rol)) {
            intent = new Intent(LoginActivity.this, AdminActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, PistasActivity.class);
        }
        startActivity(intent);
        finish(); // Cerramos el login para que no se pueda volver atrás con el botón del móvil
    }
}
