package com.wishport.frontend.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wishport.frontend.R;
import com.wishport.frontend.adapters.ReservaAdapter;
import com.wishport.frontend.api.ApiService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DE ADMINISTRADOR: Panel de control exclusivo para usuarios con rol "ADMIN".
 *
 * Funcionalidades:
 * 1. LISTA DE RESERVAS DE HOY: Descarga todas las reservas del servidor y
 *    filtra las de la fecha actual para mostrarlas en un RecyclerView.
 *
 * 2. ESCÁNER QR: El administrador puede escanear el código QR que muestra
 *    un usuario en DetalleReservaActivity. El sistema lo compara contra
 *    la lista de reservas de hoy para confirmar o denegar el acceso.
 *
 * 3. SEGURIDAD DOBLE: Al abrirse, verifica que el rol guardado en
 *    SharedPreferences sea "ADMIN". Si no lo es, cierra la Activity.
 *    (El backend también valida el rol mediante el token JWT).
 */
@AndroidEntryPoint
public class AdminActivity extends AppCompatActivity {

    /** Código de solicitud para el permiso de cámara (número arbitrario único) */
    private static final int CAMERA_PERMISSION_CODE = 100;

    /** Lista visual de las reservas del día */
    private RecyclerView recyclerViewReservas;
    /** Adaptador del RecyclerView con la lista de reservas de hoy */
    private ReservaAdapter reservaAdapter;
    /** Indicador de carga mientras se descargan las reservas */
    private ProgressBar progressBar;
    /** Layout que aparece cuando no hay reservas hoy */
    private View emptyStateLayout;

    /** ApiService inyectado por Hilt para llamar a GET /api/reservas */
    @Inject ApiService apiService;
    /** Lista de reservas del día actual (filtrada del total del servidor) */
    private List<Reserva> reservasHoy = new ArrayList<>();

    /**
     * Inicializa la pantalla de administrador:
     * 1. Verifica que el usuario tiene rol ADMIN.
     * 2. Vincula las vistas.
     * 3. Configura el RecyclerView.
     * 4. Configura los botones de logout y escáner QR.
     * 5. Carga las reservas del día.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        validarAccesoAdmin(); // Si no es admin, termina la Activity

        vincularVistas();

        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(new ArrayList<>());
        recyclerViewReservas.setAdapter(reservaAdapter);

        findViewById(R.id.btnLogout).setOnClickListener(v -> hacerLogout());
        findViewById(R.id.btnCrearAdmin).setOnClickListener(v -> mostrarDialogoCrearAdmin());
        findViewById(R.id.btnEscanearQr).setOnClickListener(v -> solicitarPermisoCamara());

        cargarReservasDelDia();
    }

    /**
     * Comprueba que el rol guardado en SharedPreferences sea "ADMIN".
     * Si no lo es, muestra un mensaje y cierra la Activity.
     * Esta es una capa de seguridad extra en el cliente; el servidor
     * también valida el JWT para rechazar peticiones de no admins.
     */
    private void validarAccesoAdmin() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        String rol = prefs.getString("rolUsuario", "");
        if (!"ADMIN".equals(rol)) {
            Toast.makeText(this, "Acceso denegado: No eres administrador", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** Vincula las variables con los elementos del XML activity_admin */
    private void vincularVistas() {
        recyclerViewReservas = findViewById(R.id.recyclerViewReservasHoy);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    /**
     * Descarga las reservas del día de hoy desde el servidor (GET /api/reservas/hoy).
     * El filtrado por fecha se hace en el backend, evitando descargar todas las reservas.
     */
    private void cargarReservasDelDia() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);

        apiService.obtenerReservasHoy().enqueue(new Callback<List<Reserva>>() {
            @Override
            public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    reservasHoy.clear();
                    reservasHoy.addAll(response.body());
                    actualizarInterfaz();
                }
            }

            @Override
            public void onFailure(Call<List<Reserva>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Actualiza la UI según si hay o no reservas para hoy:
     * - Sin reservas: muestra emptyStateLayout ("No hay reservas hoy").
     * - Con reservas: muestra el RecyclerView y actualiza el adaptador.
     */
    private void actualizarInterfaz() {
        if (reservasHoy.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewReservas.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewReservas.setVisibility(View.VISIBLE);
            reservaAdapter.actualizarLista(reservasHoy); // Reemplaza la lista y redibuja
        }
    }

    // =========================================================================
    // GESTIÓN DEL ESCÁNER QR
    // =========================================================================

    /**
     * Antes de abrir la cámara, solicita el permiso CAMERA al sistema.
     * Si ya tiene el permiso: abre el escáner directamente.
     * Si no: muestra el diálogo del sistema para pedir el permiso.
     */
    private void solicitarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            abrirEscanner();
        }
    }

    /**
     * Abre la Activity de escaneo QR usando la librería ZXing (IntentIntegrator).
     * IntentIntegrator lanza una Activity nativa de ZXing que muestra la cámara
     * y devuelve el resultado a onActivityResult().
     */
    private void abrirEscanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Enfoca el código QR del usuario");
        integrator.setBeepEnabled(true); // Emite un pitido al escanear
        integrator.setOrientationLocked(true); // Bloquea la orientación vertical
        integrator.initiateScan();
    }

    /**
     * Callback del sistema cuando el usuario responde a la solicitud de permiso.
     * Si concede el permiso de cámara, abre el escáner.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            abrirEscanner();
        }
    }

    /**
     * Callback que recibe el resultado del escáner QR de ZXing.
     * IntentIntegrator.parseActivityResult() extrae el texto escaneado del Intent.
     * Si hay contenido válido, lo pasa a validarCodigoEscaneado().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            validarCodigoEscaneado(result.getContents());
        }
    }

    /**
     * Valida el código QR escaneado comparando con reservasHoy.
     * Busca una reserva cuyo codigoQr coincida exactamente con el texto del QR.
     *
     * Si coincide   -> ACCESO CONCEDIDO: muestra el nombre del usuario.
     * Si no coincide-> ACCESO DENEGADO: el QR no es de hoy o es falso.
     *
     * @param qrLeido Texto extraído del código QR escaneado.
     */
    private void validarCodigoEscaneado(String qrLeido) {
        Reserva reservaValida = null;
        for (Reserva r : reservasHoy) {
            if (qrLeido.equals(r.getCodigoQr())) {
                reservaValida = r;
                break;
            }
        }

        if (reservaValida != null) {
            String nombre = reservaValida.getIdUsuario() != null ? reservaValida.getIdUsuario().getNombre() : "Usuario";
            Toast.makeText(this, "✅ ACCESO CONCEDIDO: " + nombre, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "❌ ACCESO DENEGADO: QR inválido o de otro día", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Cierra la sesión del administrador:
     * 1. Borra los datos de SharedPreferences.
     * 2. Borra el token JWT.
     * 3. Vuelve a LoginActivity.
     */
    private void hacerLogout() {
        getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit().clear().apply();
        TokenManager.clear(this);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // =========================================================================
    // CREACIÓN DE ADMINISTRADORES
    // =========================================================================

    /**
     * Muestra un diálogo con formulario para crear un nuevo administrador.
     * Campos: Nombre, Email, Contraseña, Teléfono.
     * Al confirmar, valida los campos y llama al endpoint crearAdmin.
     */
    private void mostrarDialogoCrearAdmin() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etNombre = new EditText(this);
        etNombre.setHint("Nombre completo");
        etNombre.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(etNombre);

        EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        EditText etPassword = new EditText(this);
        etPassword.setHint("Contraseña");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassword);

        EditText etTelefono = new EditText(this);
        etTelefono.setHint("Teléfono");
        etTelefono.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etTelefono);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crear Nuevo Administrador");
        builder.setView(layout);
        builder.setPositiveButton("Crear", null);
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String telefono = etTelefono.getText().toString().trim();

            if (nombre.isEmpty()) {
                etNombre.setError("El nombre es obligatorio");
                return;
            }
            if (email.isEmpty()) {
                etEmail.setError("El email es obligatorio");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("La contraseña es obligatoria");
                return;
            }
            if (telefono.isEmpty()) {
                etTelefono.setError("El teléfono es obligatorio");
                return;
            }

            Usuario nuevoAdmin = new Usuario();
            nuevoAdmin.setNombre(nombre);
            nuevoAdmin.setEmail(email);
            nuevoAdmin.setPassword(password);
            nuevoAdmin.setTelefono(telefono);
            nuevoAdmin.setRol("ADMIN");

            crearAdmin(nuevoAdmin, dialog);
        });
    }

    /**
     * Llama al endpoint POST /api/usuarios/crear-admin para crear un administrador.
     * El backend verifica que el usuario autenticado tenga rol ADMIN.
     */
    private void crearAdmin(Usuario usuario, AlertDialog dialog) {
        apiService.crearAdmin(usuario).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(AdminActivity.this,
                            "Administrador creado: " + response.body().getNombre(),
                            Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else if (response.code() == 403) {
                    Toast.makeText(AdminActivity.this,
                            "No tienes permisos para crear administradores",
                            Toast.LENGTH_LONG).show();
                } else if (response.code() == 409) {
                    Toast.makeText(AdminActivity.this,
                            "Este email ya está registrado",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AdminActivity.this,
                            "Error al crear administrador: " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Toast.makeText(AdminActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
