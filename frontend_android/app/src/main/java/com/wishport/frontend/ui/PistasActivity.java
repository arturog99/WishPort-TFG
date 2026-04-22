package com.wishport.frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wishport.frontend.R;
import com.wishport.frontend.adapters.PistaAdapter;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.models.Pista;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PistasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPistas;
    private PistaAdapter pistaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        // 1. Configurar RecyclerView
        recyclerViewPistas = findViewById(R.id.recyclerViewPistas);
        recyclerViewPistas.setLayoutManager(new LinearLayoutManager(this));

        // 2. Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // 3. Obtener datos del backend
        apiService.obtenerPistas().enqueue(new Callback<List<Pista>>() {
            @Override
            public void onResponse(Call<List<Pista>> call, Response<List<Pista>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pista> listaPistas = response.body();

                    // 4. Crear adapter y asignarlo al RecyclerView
                    pistaAdapter = new PistaAdapter(listaPistas);
                    recyclerViewPistas.setAdapter(pistaAdapter);

                    // 5. Configurar click listener para abrir detalle
                    pistaAdapter.setOnItemClickListener(pista -> {
                        Intent intent = new Intent(PistasActivity.this, DetallePistaActivity.class);
                        intent.putExtra(DetallePistaActivity.EXTRA_PISTA, pista);
                        startActivity(intent);
                    });
                } else {
                    Toast.makeText(PistasActivity.this,
                            "Error al cargar pistas: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Pista>> call, Throwable t) {
                Toast.makeText(PistasActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
