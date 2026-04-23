package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wishport.frontend.R;
import com.wishport.frontend.adapters.ReservaAdapter;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Reserva;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReservasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReservas;
    private ReservaAdapter reservaAdapter;
    private ProgressBar progressBar;
    private TextView tvSinReservas;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Reserva> listaReservas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_reservas);

        recyclerViewReservas = findViewById(R.id.recyclerViewReservas);
        progressBar = findViewById(R.id.progressBar);
        tvSinReservas = findViewById(R.id.tvSinReservas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerViewReservas.setLayoutManager(new LinearLayoutManager(this));
        reservaAdapter = new ReservaAdapter(listaReservas);
        recyclerViewReservas.setAdapter(reservaAdapter);

        reservaAdapter.setOnItemClickListener(reserva -> {
            Intent intent = new Intent(ReservasActivity.this, DetalleReservaActivity.class);
            intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA, reserva);
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("RESERVAS", "Pull-to-refresh activado");
            cargarReservas();
        });

        cargarReservas();
    }

    private void cargarReservas() {
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        Log.d("RESERVAS", "idUsuario obtenido: " + idUsuario);
        Log.d("RESERVAS", "Todas las prefs: " + prefs.getAll());

        if (idUsuario == -1) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<Reserva>> call = apiService.obtenerReservasPorUsuario(idUsuario);

        Log.d("RESERVAS", "Llamando a endpoint: api/reservas/usuario/" + idUsuario);

        call.enqueue(new Callback<List<Reserva>>() {
            @Override
            public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Log.d("RESERVAS", "Response code: " + response.code());
                Log.d("RESERVAS", "Response body: " + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    listaReservas.clear();
                    listaReservas.addAll(response.body());
                    reservaAdapter.notifyDataSetChanged();

                    Log.d("RESERVAS", "Reservas cargadas: " + listaReservas.size());

                    if (listaReservas.isEmpty()) {
                        tvSinReservas.setVisibility(View.VISIBLE);
                        recyclerViewReservas.setVisibility(View.GONE);
                    } else {
                        tvSinReservas.setVisibility(View.GONE);
                        recyclerViewReservas.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("RESERVAS", "Error en respuesta: " + response.errorBody());
                    Toast.makeText(ReservasActivity.this, "Error al cargar reservas: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Reserva>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Log.e("RESERVAS", "Error de conexión", t);
                Toast.makeText(ReservasActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
