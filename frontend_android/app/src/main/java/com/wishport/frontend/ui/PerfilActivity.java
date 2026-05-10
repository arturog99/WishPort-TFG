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
import com.wishport.frontend.models.Usuario;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE PERFIL: Permite al usuario consultar y modificar sus datos personales.
 * Muestra información como nombre, email (solo lectura) y teléfono.
 * Migrada a Hilt para usar ApiService con AuthInterceptor y timeouts.
 */
@AndroidEntryPoint
public class PerfilActivity extends AppCompatActivity {

    // Componentes de la interfaz
    private TextInputEditText etNombre, etEmail, etTelefono;
    private Button btnActualizar;
    private ProgressBar progressBar;

    // Herramientas de datos
    @Inject ApiService apiService;
    private int idUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // 1. Vincular los elementos del diseño XML con el código Java
        vincularComponentes();

        // apiService inyectado por Hilt (con AuthInterceptor + timeouts)

        // 2. Carga inicial: Recuperamos los datos que ya tenemos guardados en el móvil (SharedPreferences)
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        idUsuario = prefs.getInt("idUsuario", -1);
        String nombreActual = prefs.getString("nombreUsuario", "");
        String emailActual = prefs.getString("emailUsuario", "");

        if (idUsuario != -1) {
            // Rellenamos los campos inmediatamente con la info local para una mejor UX
            etNombre.setText(nombreActual);
            etEmail.setText(emailActual);
            
            // 3. Carga en segundo plano: Pedimos los datos actualizados al servidor (como el teléfono)
            obtenerDatosCompletosDelServidor();
        } else {
            Toast.makeText(this, "Sesión no válida. Por favor, inicia sesión de nuevo.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 4. Configurar la acción al pulsar el botón de guardar cambios
        btnActualizar.setOnClickListener(v -> intentarActualizarPerfil());
    }

    /** Conecta las variables Java con los IDs definidos en el XML (activity_perfil.xml) */
    private void vincularComponentes() {
        etNombre = findViewById(R.id.etNombrePerfil);
        etEmail = findViewById(R.id.etEmailPerfil);
        etTelefono = findViewById(R.id.etTelefonoPerfil);
        btnActualizar = findViewById(R.id.btnActualizarPerfil);
        progressBar = findViewById(R.id.progressBarPerfil);
    }

    /** 
     * Llama a la API para traer la información más reciente del usuario desde la base de datos.
     * Esto nos permite obtener campos que no siempre guardamos localmente, como el teléfono.
     */
    private void obtenerDatosCompletosDelServidor() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.obtenerUsuarioPorId(idUsuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Usuario user = response.body();
                    etNombre.setText(user.getNombre());
                    etEmail.setText(user.getEmail());
                    if (user.getTelefono() != null) {
                        etTelefono.setText(user.getTelefono());
                    }
                } else {
                    Toast.makeText(PerfilActivity.this,
                            "No se pudieron cargar los datos actualizados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PerfilActivity.this,
                        "Error de red al cargar perfil. Revisa tu conexión.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Recoge los datos editados por el usuario, valida que sean correctos y los envía al servidor.
     */
    private void intentarActualizarPerfil() {
        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevoTelefono = etTelefono.getText().toString().trim();

        // Validación básica de seguridad
        if (nuevoNombre.isEmpty()) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }

        // Mostramos carga y bloqueamos el botón para evitar envíos duplicados
        progressBar.setVisibility(View.VISIBLE);
        btnActualizar.setEnabled(false);

        // Preparamos el objeto Usuario con los nuevos valores
        Usuario usuarioUpdate = new Usuario();
        usuarioUpdate.setNombre(nuevoNombre);
        usuarioUpdate.setTelefono(nuevoTelefono);

        // Llamada a la API de actualización (PUT api/usuarios/{id})
        apiService.actualizarUsuario(idUsuario, usuarioUpdate).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
                
                if (response.isSuccessful()) {
                    Toast.makeText(PerfilActivity.this, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show();
                    
                    // IMPORTANTE: Actualizamos también los datos locales (SharedPreferences)
                    // para que el nuevo nombre aparezca en el resto de pantallas sin reiniciar la app.
                    actualizarNombreEnLocal(nuevoNombre);
                } else {
                    Toast.makeText(PerfilActivity.this, "No se pudieron guardar los cambios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnActualizar.setEnabled(true);
                Toast.makeText(PerfilActivity.this, "Error de red: revisa tu conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Guarda de forma permanente el nuevo nombre en las preferencias del dispositivo */
    private void actualizarNombreEnLocal(String nombre) {
        SharedPreferences.Editor editor = getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit();
        editor.putString("nombreUsuario", nombre);
        editor.apply();
    }
}
