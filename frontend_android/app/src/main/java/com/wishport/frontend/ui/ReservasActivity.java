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
 * PANTALLA MIS RESERVAS: Muestra el historial de reservas del usuario logueado.
 * Usa Shimmer para la carga y permite refrescar deslizando hacia abajo.
 */
@AndroidEntryPoint
public class ReservasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReservas;
    private ReservaAdapter reservaAdapter;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerFrameLayout;
    
    private List<Reserva> listaReservas = new ArrayList<>();

    @Inject ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_reservas);

        // 1. Enlace de vistas
        vincularVistas();

        // 2. Configuración de la lista
        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(listaReservas);
        recyclerViewReservas.setAdapter(reservaAdapter);

        // 3. Eventos: Click en reserva para ver detalle/QR
        reservaAdapter.setOnItemClickListener(reserva -> {
            Intent intent = new Intent(ReservasActivity.this, DetalleReservaActivity.class);
            intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, reserva);
            startActivity(intent);
        });

        // 4. Configurar Swipe to Refresh
        swipeRefreshLayout.setOnRefreshListener(this::obtenerReservasDelServidor);

        // 5. Carga inicial
        obtenerReservasDelServidor();
    }

    private void vincularVistas() {
        recyclerViewReservas = findViewById(R.id.recyclerViewReservas);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerFrameLayout = findViewById(R.id.shimmerReservas);
    }

    /**
     * Pide al servidor las reservas del usuario actual.
     */
    private void obtenerReservasDelServidor() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) {
            Toast.makeText(this, "Error: Sesión no encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar animación de carga si no es un refresh manual
        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            recyclerViewReservas.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }

        // apiService inyectado por Hilt (con AuthInterceptor + timeouts)
        apiService.obtenerReservasPorUsuario(idUsuario).enqueue(new Callback<List<Reserva>>() {
            @Override
            public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                finalizarEstadoVisual();

                if (response.isSuccessful() && response.body() != null) {
                    listaReservas.clear();
                    listaReservas.addAll(response.body());
                    reservaAdapter.notifyDataSetChanged();

                    // Controlar si mostramos el mensaje de "No hay reservas"
                    if (listaReservas.isEmpty()) {
                        recyclerViewReservas.setVisibility(View.GONE);
                        emptyStateLayout.setVisibility(View.VISIBLE);
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

    /** Oculta Shimmer y para el refresco */
    private void finalizarEstadoVisual() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            cerrarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
