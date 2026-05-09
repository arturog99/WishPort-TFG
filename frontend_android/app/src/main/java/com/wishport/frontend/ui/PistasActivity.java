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

        btnPerfil = findViewById(R.id.btnPerfil);
        Button btnMisReservas = findViewById(R.id.btnMisReservas);
        recyclerViewPistas = findViewById(R.id.recyclerViewPistas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshPistas);
        shimmerFrameLayout = findViewById(R.id.shimmerPistas);

        recyclerViewPistas.setLayoutManager(new LinearLayoutManager(this));

        btnMisReservas.setOnClickListener(v -> {
            Intent intent = new Intent(PistasActivity.this, ReservasActivity.class);
            startActivity(intent);
        });

        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(PistasActivity.this, PerfilActivity.class);
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(this::cargarPistas);

        cargarPistas();
    }

    private void cargarPistas() {
        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            recyclerViewPistas.setVisibility(View.GONE);
        }

        ApiService apiService = RetrofitClient.getApiService();
        apiService.obtenerPistas().enqueue(new Callback<List<Pista>>() {
            @Override
            public void onResponse(Call<List<Pista>> call, Response<List<Pista>> response) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                recyclerViewPistas.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Pista> listaPistas = response.body();
                    pistaAdapter = new PistaAdapter(listaPistas);
                    recyclerViewPistas.setAdapter(pistaAdapter);

                    pistaAdapter.setOnItemClickListener(pista -> {
                        Intent intent = new Intent(PistasActivity.this, DetallePistaActivity.class);
                        intent.putExtra(DetallePistaActivity.EXTRA_PISTA, pista);
                        startActivity(intent);
                    });
                } else {
                    Toast.makeText(PistasActivity.this, "Error al cargar pistas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Pista>> call, Throwable t) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                recyclerViewPistas.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(PistasActivity.this, "Error de conexión", Toast.LENGTH_LONG).show();
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
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        TokenManager.clear(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
