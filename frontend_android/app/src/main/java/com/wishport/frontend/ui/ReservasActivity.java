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
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.api.TokenManager;
import com.wishport.frontend.models.Reserva;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReservasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReservas;
    private ReservaAdapter reservaAdapter;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerFrameLayout;
    private List<Reserva> listaReservas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_reservas);

        recyclerViewReservas = findViewById(R.id.recyclerViewReservas);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerFrameLayout = findViewById(R.id.shimmerReservas);

        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(listaReservas);
        recyclerViewReservas.setAdapter(reservaAdapter);

        reservaAdapter.setOnItemClickListener(reserva -> {
            Intent intent = new Intent(ReservasActivity.this, DetalleReservaActivity.class);
            intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, reserva);
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(this::cargarReservas);

        cargarReservas();
    }

    private void cargarReservas() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            recyclerViewReservas.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }

        ApiService apiService = RetrofitClient.getApiService();
        apiService.obtenerReservasPorUsuario(idUsuario).enqueue(new Callback<List<Reserva>>() {
            @Override
            public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    listaReservas.clear();
                    listaReservas.addAll(response.body());
                    reservaAdapter.notifyDataSetChanged();

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
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                recyclerViewReservas.setVisibility(View.VISIBLE);
                Toast.makeText(ReservasActivity.this, "Error al conectar", Toast.LENGTH_SHORT).show();
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
