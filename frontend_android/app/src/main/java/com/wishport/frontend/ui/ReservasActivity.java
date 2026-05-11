package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.wishport.frontend.R;
import com.wishport.frontend.adapters.ReservaAdapter;
import com.wishport.frontend.api.ApiService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.models.Reserva;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA MIS RESERVAS: Lista el historial completo de reservas del usuario.
 *
 * Carga las reservas del usuario autenticado llamando a
 * GET /api/reservas/usuario/{idUsuario} (requiere JWT).
 *
 * Funcionalidades:
 * - Animación Shimmer mientras se cargan los datos.
 * - Pull-to-refresh para recargar la lista.
 * - Mensaje de "sin reservas" si la lista está vacía.
 * - Al pulsar una reserva, navega a DetalleReservaActivity.
 * - Menú de logout en la barra superior.
 */
@AndroidEntryPoint
public class ReservasActivity extends AppCompatActivity {

    /** Lista visual de reservas */
    private RecyclerView recyclerViewReservas;
    /** Adaptador que convierte la lista de Reserva en filas visuales */
    private ReservaAdapter reservaAdapter;
    /** Layout que se muestra cuando no hay reservas */
    private LinearLayout emptyStateLayout;
    /** Contenedor del gesto pull-to-refresh */
    private SwipeRefreshLayout swipeRefreshLayout;
    /** Animación de esqueleto durante la carga */
    private ShimmerFrameLayout shimmerFrameLayout;

    /** Lista mutable que se actualiza en cada recarga */
    private List<Reserva> listaReservas = new ArrayList<>();

    /** ApiService inyectado por Hilt (con AuthInterceptor que añade el JWT) */
    @Inject ApiService apiService;

    /**
     * Inicializa la pantalla:
     * 1. Vincula las vistas.
     * 2. Configura el RecyclerView con su adaptador.
     * 3. Registra el listener de clic en reservas para navegar al detalle.
     * 4. Configura el pull-to-refresh.
     * 5. Lanza la carga inicial de reservas.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_reservas);

        vincularVistas();

        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(listaReservas);
        recyclerViewReservas.setAdapter(reservaAdapter);

        // Al pulsar una reserva, abrimos DetalleReservaActivity con esa reserva
        reservaAdapter.setOnItemClickListener(reserva -> {
            Intent intent = new Intent(ReservasActivity.this, DetalleReservaActivity.class);
            intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, reserva);
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(this::obtenerReservasDelServidor);

        obtenerReservasDelServidor(); // Carga inicial

        // El botón de volver cierra esta Activity y regresa a PistasActivity
        findViewById(R.id.btnVolverPistas).setOnClickListener(v -> finish());
    }

    /**
     * Se ejecuta cada vez que la Activity vuelve a primer plano.
     * Recarga las reservas del servidor para reflejar cambios hechos
     * en otras pantallas (ej: cancelación en DetalleReservaActivity).
     * La primera carga se hace en onCreate(), así que aquí solo
     * recargamos si el adaptador ya tiene datos o estaba vacío tras la carga.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (reservaAdapter != null) {
            obtenerReservasDelServidor();
        }
    }

    /** Vincula las variables con los elementos del XML activity_pantalla_reservas */
    private void vincularVistas() {
        recyclerViewReservas = findViewById(R.id.recyclerViewReservas);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerFrameLayout = findViewById(R.id.shimmerReservas);
    }

    /**
     * Pide al servidor las reservas del usuario actualmente logueado.
     * Lee el idUsuario de SharedPreferences (guardado al hacer login).
     *
     * Flujo:
     * 1. Muestra Shimmer (si no es un pull-to-refresh manual).
     * 2. Llama a GET /api/reservas/usuario/{idUsuario}.
     * 3. Si hay éxito: actualiza listaReservas y notifica al adaptador.
     *    - Si la lista está vacía: muestra emptyStateLayout.
     *    - Si tiene datos: muestra el RecyclerView.
     * 4. Si falla: muestra un Toast de error.
     */
    private void obtenerReservasDelServidor() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) {
            Toast.makeText(this, "Error: Sesión no encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        // Solo mostramos el Shimmer en la primera carga (no en el pull-to-refresh)
        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            recyclerViewReservas.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }

        apiService.obtenerReservasPorUsuario(idUsuario).enqueue(new Callback<List<Reserva>>() {
            @Override
            public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                finalizarEstadoVisual();

                if (response.isSuccessful() && response.body() != null) {
                    listaReservas.clear();
                    listaReservas.addAll(response.body());
                    reservaAdapter.notifyDataSetChanged(); // Notifica al RecyclerView para redibujar

                    if (listaReservas.isEmpty()) {
                        recyclerViewReservas.setVisibility(View.GONE);
                        emptyStateLayout.setVisibility(View.VISIBLE); // "No tienes reservas"
                    } else {
                        recyclerViewReservas.setVisibility(View.VISIBLE);
                        emptyStateLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Reserva>> call, Throwable t) {
                finalizarEstadoVisual();
                recyclerViewReservas.setVisibility(View.VISIBLE);
                Toast.makeText(ReservasActivity.this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Finaliza el estado visual de carga:
     * - Para y oculta el Shimmer.
     * - Detiene el indicador de pull-to-refresh.
     * Se llama tanto en éxito como en fallo para limpiar siempre la UI.
     */
    private void finalizarEstadoVisual() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    /** Infla el menú de la barra superior con el botón de logout */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** Gestiona el clic en el menú superior */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            cerrarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Cierra la sesión del usuario:
     * 1. Borra los datos de SharedPreferences.
     * 2. Borra el token JWT via TokenManager.
     * 3. Vuelve a LoginActivity limpiando toda la pila de Activities.
     */
    private void cerrarSesion() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        TokenManager.clear(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
