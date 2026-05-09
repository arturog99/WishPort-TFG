package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.wishport.frontend.R;
import com.wishport.frontend.adapters.PistaAdapter;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.models.Pista;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA PRINCIPAL: Muestra el listado de pistas deportivas disponibles.
 * Permite filtrar por deporte (a través del adapter) y acceder a reservas o perfil.
 */
public class PistasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPistas;
    private PistaAdapter pistaAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerFrameLayout;
    private ImageButton btnPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        // 1. Enlazamos los elementos de la interfaz (XML) con Java
        btnPerfil = findViewById(R.id.btnPerfil);
        Button btnMisReservas = findViewById(R.id.btnMisReservas);
        recyclerViewPistas = findViewById(R.id.recyclerViewPistas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshPistas);
        shimmerFrameLayout = findViewById(R.id.shimmerPistas);

        // 2. Configuramos la lista (RecyclerView) para que se vea vertical
        recyclerViewPistas.setLayoutManager(new LinearLayoutManager(this));

        // 3. Configuración de botones superiores
        btnMisReservas.setOnClickListener(v -> {
            startActivity(new Intent(PistasActivity.this, ReservasActivity.class));
        });

        btnPerfil.setOnClickListener(v -> {
            startActivity(new Intent(PistasActivity.this, PerfilActivity.class));
        });

        // 4. Configurar el gesto de "tirar para abajo" para actualizar
        swipeRefreshLayout.setOnRefreshListener(this::cargarPistas);

        // 5. Carga inicial de datos
        cargarPistas();
    }

    /**
     * Llama al servidor para obtener la lista actualizada de pistas.
     * Gestiona el estado de carga (Shimmer) y los posibles errores.
     */
    private void cargarPistas() {
        // Si no estamos refrescando manualmente, mostramos la animación de carga (Shimmer)
        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            recyclerViewPistas.setVisibility(View.GONE);
        }

        ApiService apiService = RetrofitClient.getApiService();
        apiService.obtenerPistas().enqueue(new Callback<List<Pista>>() {
            @Override
            public void onResponse(Call<List<Pista>> call, Response<List<Pista>> response) {
                // Parar animaciones de carga
                finalizarEstadoCarga();

                if (response.isSuccessful() && response.body() != null) {
                    List<Pista> listaPistas = response.body();
                    
                    // Configuramos el adaptador con los datos recibidos
                    pistaAdapter = new PistaAdapter(listaPistas);
                    recyclerViewPistas.setAdapter(pistaAdapter);

                    // Al pulsar en una pista, vamos a su detalle
                    pistaAdapter.setOnItemClickListener(pista -> {
                        Intent intent = new Intent(PistasActivity.this, DetallePistaActivity.class);
                        intent.putExtra(DetallePistaActivity.EXTRA_PISTA, pista);
                        startActivity(intent);
                    });
                } else {
                    Toast.makeText(PistasActivity.this, "Error al recibir datos del servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Pista>> call, Throwable t) {
                finalizarEstadoCarga();
                Toast.makeText(PistasActivity.this, "Sin conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Oculta los indicadores de carga y muestra la lista */
    private void finalizarEstadoCarga() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        recyclerViewPistas.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);
    }

    // --- MENÚ DE OPCIONES ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            hacerLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Cierra la sesión borrando los tokens y volviendo al Login.
     */
    private void hacerLogout() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        TokenManager.clear(this);
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
