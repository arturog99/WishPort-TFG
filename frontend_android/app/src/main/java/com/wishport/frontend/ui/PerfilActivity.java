package com.wishport.frontend.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.data.repository.AuthRepository;
import com.wishport.frontend.models.Usuario;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE PERFIL: Permite al usuario ver y editar sus datos personales.
 *
 * Al abrirse, muestra inmediatamente los datos almacenados localmente
 * (nombre y email de SharedPreferences) para una respuesta rápida de la UI.
 * En paralelo, consulta el servidor para obtener los datos actualizados
 * (incluyendo el teléfono, que no se guarda localmente).
 *
 * Al guardar, hace un PUT /api/usuarios/{id} con nombre y teléfono actualizados,
 * y sincroniza el nombre nuevo en SharedPreferences.
 */
@AndroidEntryPoint
public class PerfilActivity extends AppCompatActivity {

    /** Campos del formulario de perfil (Material Design TextInputEditText) */
    private TextInputEditText etNombre, etEmail, etTelefono;
    /** Botón que guarda los cambios del perfil */
    private Button btnActualizar;
    /** Indicador de carga durante las peticiones al servidor */
    private ProgressBar progressBar;

    /**
     * Repositorio inyectado por Hilt.
     * Se usa para las llamadas GET (obtenerPerfil) y PUT (actualizarPerfil)
     * en lugar de usar apiService directamente.
     */
    @Inject AuthRepository authRepository;
    /** ID del usuario logueado, leído de SharedPreferences */
    private int idUsuario;

    /**
     * Inicializa la pantalla:
     * 1. Vincula las vistas.
     * 2. Precarga nombre y email desde SharedPreferences (respuesta instantánea).
     * 3. Consulta el servidor para obtener el teléfono actualizado.
     * 4. Configura el botón de guardar.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        vincularComponentes();

        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        idUsuario = prefs.getInt("idUsuario", -1);
        // Precargamos datos locales para que la pantalla no aparezca vacía mientras carga
        etNombre.setText(prefs.getString("nombreUsuario", ""));
        etEmail.setText(prefs.getString("emailUsuario", ""));

        if (idUsuario != -1) {
            obtenerDatosServidor(); // Actualiza con datos frescos del servidor
        }

        btnActualizar.setOnClickListener(v -> intentarActualizar());
    }

    /** Vincula las variables con los elementos del XML activity_perfil */
    private void vincularComponentes() {
        etNombre = findViewById(R.id.etNombrePerfil);
        etEmail = findViewById(R.id.etEmailPerfil);
        etTelefono = findViewById(R.id.etTelefonoPerfil);
        btnActualizar = findViewById(R.id.btnActualizarPerfil);
        progressBar = findViewById(R.id.progressBarPerfil);
    }

    /**
     * Consulta al servidor los datos actuales del usuario.
     * GET /api/usuarios/{idUsuario}
     * Al recibir la respuesta, actualiza los campos de nombre y teléfono
     * con los valores más recientes del servidor.
     */
    private void obtenerDatosServidor() {
        progressBar.setVisibility(View.VISIBLE);
        authRepository.obtenerPerfil(idUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Usuario u = response.body();
                    etNombre.setText(u.getNombre());
                    etTelefono.setText(u.getTelefono()); // Teléfono no estaba en SharedPreferences
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    /**
     * Valida y envía la actualización del perfil al servidor.
     * 1. Valida que el nombre no esté vacío.
     * 2. Crea un objeto Usuario con solo los campos editables.
     * 3. Llama a PUT /api/usuarios/{idUsuario}.
     * 4. Si tiene éxito: muestra confirmación y sincroniza el nombre en SharedPreferences.
     */
    private void intentarActualizar() {
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombre.setError("Nombre obligatorio");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnActualizar.setEnabled(false);

        // Creamos solo con los campos que el usuario puede cambiar
        Usuario update = new Usuario();
        update.setNombre(nombre);
        update.setTelefono(etTelefono.getText().toString().trim());

        authRepository.actualizarPerfil(idUsuario, update).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(PerfilActivity.this, "Perfil guardado", Toast.LENGTH_SHORT).show();
                    // Sincronizamos el nuevo nombre en SharedPreferences para que
                    // otras pantallas (ej: menú) lo muestren actualizado
                    getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit()
                            .putString("nombreUsuario", nombre).apply();
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
            }
        });
    }
}
