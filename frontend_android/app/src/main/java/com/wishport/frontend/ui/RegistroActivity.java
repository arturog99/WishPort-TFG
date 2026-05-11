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
 *
 * Realiza validación local antes de enviar nada al servidor:
 * - Nombre no vacío.
 * - Email con formato válido (usando android.util.Patterns).
 * - Contraseña segura: mínimo 8 caracteres con al menos 1 letra y 1 número.
 * - Teléfono con al menos 9 dígitos.
 *
 * Si la validación pasa, delega al AuthViewModel que crea el RegistroRequest
 * y lo envía al backend via AuthRepository.
 */
@AndroidEntryPoint
public class RegistroActivity extends AppCompatActivity {

    /** Campos del formulario de registro */
    private EditText etNombre, etEmail, etPassword, etTelefono;
    /** Botón que intenta el registro */
    private Button btnRegistrar;
    /** Indicador de carga que bloquea la UI mientras se procesa la petición */
    private ProgressBar progressBar;
    /** ViewModel compartido con LoginActivity para operaciones de autenticación */
    private AuthViewModel viewModel;

    /**
     * Expresión regular para validar la contraseña localmente.
     * Exige: mínimo 8 caracteres, al menos un dígito y al menos una letra.
     * Esto evita enviar contraseñas débiles al servidor.
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

    /**
     * Inicializa la pantalla de registro:
     * 1. Infla el XML del formulario.
     * 2. Vincula los campos de texto y el botón.
     * 3. Obtiene el ViewModel desde Hilt.
     * 4. Configura los listeners de los botones.
     * 5. Suscribe los observadores del ViewModel.
     */
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

        // Al pulsar "Registrarse", intentamos el registro con validación previa
        btnRegistrar.setOnClickListener(v -> intentarRegistro());
        // Al pulsar "Volver", cerramos esta Activity y volvemos al Login
        findViewById(R.id.btnVolverLogin).setOnClickListener(v -> finish());

        observarEstadoRegistro();
    }

    /**
     * Suscribe esta Activity a los tres LiveData relevantes del ViewModel:
     *
     * registroResponse: el servidor creó el usuario correctamente.
     *   -> Muestra un mensaje de éxito y vuelve al Login con finish().
     *
     * isLoading: hay una operación HTTP en curso.
     *   -> Muestra/oculta el ProgressBar y habilita/deshabilita el botón.
     *
     * error: el servidor rechazó el registro (ej: email ya en uso, 409).
     *   -> Muestra el mensaje de error en un Toast.
     */
    private void observarEstadoRegistro() {
        viewModel.registroResponse.observe(this, usuario -> {
            Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
            finish(); // Cierra esta Activity y vuelve al Login
        });

        viewModel.isLoading.observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegistrar.setEnabled(!loading); // Evitar doble clic mientras se procesa
        });

        viewModel.error.observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Recoge los datos del formulario, los valida y, si pasan la validación,
     * los envía al ViewModel para que gestione la petición al servidor.
     */
    private void intentarRegistro() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (validarDatos(nombre, email, password, telefono)) {
            viewModel.registrar(nombre, email, password, telefono);
        }
    }

    /**
     * Valida los datos del formulario antes de enviarlos al servidor.
     * Usa setError() para mostrar los mensajes de error directamente
     * debajo del campo correspondiente (UX más clara que un Toast).
     *
     * @return true si todos los datos son válidos, false si alguno falla.
     */
    private boolean validarDatos(String nombre, String email, String password, String telefono) {
        if (nombre.isEmpty()) { etNombre.setError("Escribe tu nombre"); return false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Email inválido"); return false; }
        if (!PASSWORD_PATTERN.matcher(password).matches()) { etPassword.setError("Contraseña poco segura (8+ chars, letras y números)"); return false; }
        if (telefono.length() < 9) { etTelefono.setError("Teléfono demasiado corto"); return false; }
        return true;
    }
}
