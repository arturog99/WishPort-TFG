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
 * PANTALLA DE LOGIN: Primera pantalla que ve el usuario (punto de entrada de la app).
 *
 * Tiene dos responsabilidades:
 * 1. AUTO-LOGIN: Si el usuario ya inició sesión antes y su token sigue guardado,
 *    le llevamos directamente a la pantalla principal sin mostrar el formulario.
 * 2. FORMULARIO DE LOGIN: Si no hay sesión activa, muestra el formulario y
 *    delega la lógica al AuthViewModel.
 *
 * @AndroidEntryPoint = Permite a Hilt inyectar dependencias en esta Activity.
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    /** Campo de texto donde el usuario escribe su email */
    private EditText etUsuario;
    /** Campo de texto donde el usuario escribe su contraseña */
    private EditText etPassword;
    /** Botón que dispara el proceso de login */
    private Button btnLogin;
    /** ViewModel que gestiona toda la lógica de autenticación */
    private AuthViewModel viewModel;

    /**
     * Método principal del ciclo de vida. Se ejecuta al crear la Activity.
     * Primero comprueba si hay sesión activa (auto-login).
     * Si la hay, redirige y termina la Activity sin inflar el XML.
     * Si no la hay, monta la UI normal de login.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // AUTO-LOGIN: si hay token y rol guardados, saltamos el formulario
        if (tieneSesionActiva()) {
            irAPantallaPrincipal();
            return; // IMPORTANTE: detener para no inflar la UI ni crear el ViewModel
        }

        setContentView(R.layout.activity_pantalla_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        vincularVistas();
        configurarEventos();
        observarDatos();
    }

    /**
     * Comprueba si el usuario ya tiene una sesión activa guardada.
     * Se considera sesión activa cuando existen tanto el token JWT
     * (en EncryptedSharedPreferences) como el rol del usuario
     * (en SharedPreferences normales).
     *
     * @return true si hay sesión activa, false si hay que hacer login.
     */
    private boolean tieneSesionActiva() {
        String savedToken = TokenManager.getToken(this);
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", null);
        return savedToken != null && rol != null;
    }

    /**
     * Navega a la pantalla correcta según el rol del usuario:
     * - ADMIN  -> AdminActivity (gestión de reservas y escaner QR)
     * - USER   -> PistasActivity (pantalla principal del usuario)
     *
     * Llama a finish() para que el usuario no pueda volver a esta pantalla
     * pulsando el botón "Atrás" del móvil.
     */
    private void irAPantallaPrincipal() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", "");
        Intent intent = "ADMIN".equals(rol)
                ? new Intent(this, AdminActivity.class)
                : new Intent(this, PistasActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Vincula las variables Java con los elementos del XML activity_pantalla_login.
     * También configura el botón de navegar a la pantalla de registro.
     */
    private void vincularVistas() {
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        findViewById(R.id.btnIrARegistro).setOnClickListener(v ->
            startActivity(new Intent(this, RegistroActivity.class)));
    }

    /**
     * Configura los listeners de los botones de la pantalla.
     * Al pulsar el botón de login:
     * 1. Lee los campos de texto.
     * 2. Valida que no estén vacíos.
     * 3. Delega al ViewModel que gestione el proceso de login.
     */
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
     * Suscribe esta Activity a los LiveData del ViewModel para reaccionar
     * cuando lleguen resultados del servidor.
     *
     * loginResponse: el login fue correcto. Extrae token, nombre, email, rol e id
     * del mapa, los guarda localmente y navega a la pantalla principal.
     *
     * error: algo fue mal. Muestra el mensaje en un Toast.
     */
    private void observarDatos() {
        viewModel.loginResponse.observe(this, respuesta -> {
            String token = (String) respuesta.get("token");
            String nombre = (String) respuesta.get("nombre");
            String email = (String) respuesta.get("email");
            String rol = (String) respuesta.get("rol");

            // El id viene como Number desde el JSON (puede ser Double o Integer)
            Object idObj = respuesta.get("idUsuario");
            int id = (idObj instanceof Number) ? ((Number) idObj).intValue() : -1;

            TokenManager.setToken(this, token);  // Guarda el JWT de forma segura
            guardarDatosLocales(id, nombre, email, rol); // Guarda el resto en SharedPreferences
            irAPantallaPrincipal();
        });

        viewModel.error.observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Guarda los datos del usuario en SharedPreferences para uso rápido
     * en otras pantallas (perfil, reservas, etc.) sin necesidad de volver
     * a consultar el servidor.
     *
     * @param id     Identificador único del usuario.
     * @param nombre Nombre del usuario.
     * @param email  Email del usuario.
     * @param rol    Rol: "USER" o "ADMIN".
     */
    private void guardarDatosLocales(int id, String nombre, String email, String rol) {
        getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit()
                .putInt("idUsuario", id)
                .putString("nombreUsuario", nombre)
                .putString("emailUsuario", email)
                .putString("rolUsuario", rol)
                .apply();
    }
}
