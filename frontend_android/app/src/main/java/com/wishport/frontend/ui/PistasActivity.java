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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.wishport.frontend.R;
import com.wishport.frontend.adapters.PistaAdapter;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.ui.viewmodels.PistasViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * PANTALLA PRINCIPAL (Migrada a MVVM): Muestra el listado de pistas.
 * Ahora la lógica de datos reside en PistasViewModel.
 */
@AndroidEntryPoint // Indica a Hilt que debe inyectar dependencias en esta Activity
public class PistasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPistas;
    private PistaAdapter pistaAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerFrameLayout;
    
    private PistasViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        // 1. Inicializar el ViewModel a través de Hilt
        viewModel = new ViewModelProvider(this).get(PistasViewModel.class);

        vincularVistas();
        configurarEventos();
        observarDatos(); // Escuchamos los cambios que vengan del ViewModel

        // Carga inicial
        viewModel.cargarPistas();
    }

    private void vincularVistas() {
        recyclerViewPistas = findViewById(R.id.recyclerViewPistas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshPistas);
        shimmerFrameLayout = findViewById(R.id.shimmerPistas);
        recyclerViewPistas.setLayoutManager(new LinearLayoutManager(this));
    }

    private void configurarEventos() {
        findViewById(R.id.btnMisReservas).setOnClickListener(v -> 
            startActivity(new Intent(this, ReservasActivity.class)));

        findViewById(R.id.btnPerfil).setOnClickListener(v -> 
            startActivity(new Intent(this, PerfilActivity.class)));

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.cargarPistas());
    }

    /**
     * OBSERVAR DATOS (LiveData): La Activity se suscribe a los cambios del ViewModel.
     * Cuando el ViewModel recibe datos de la API, esta función se activa sola.
     */
    private void observarDatos() {
        // Observar la lista de pistas
        viewModel.pistas.observe(this, listaPistas -> {
            pistaAdapter = new PistaAdapter(listaPistas);
            recyclerViewPistas.setAdapter(pistaAdapter);
            pistaAdapter.setOnItemClickListener(pista -> {
                Intent intent = new Intent(this, DetallePistaActivity.class);
                intent.putExtra(DetallePistaActivity.EXTRA_PISTA, pista);
                startActivity(intent);
            });
        });

        // Observar estado de carga (Shimmer)
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                if (!swipeRefreshLayout.isRefreshing()) {
                    shimmerFrameLayout.setVisibility(View.VISIBLE);
                    shimmerFrameLayout.startShimmer();
                    recyclerViewPistas.setVisibility(View.GONE);
                }
            } else {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                recyclerViewPistas.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Observar errores
        viewModel.error.observe(this, mensaje -> {
            if (mensaje != null) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private void hacerLogout() {
        getSharedPreferences("WishPortPrefs", MODE_PRIVATE).edit().clear().apply();
        TokenManager.clear(this);
        startActivity(new Intent(this, LoginActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
