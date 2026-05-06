package com.wishport.frontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wishport.frontend.R;
import com.wishport.frontend.adapters.PistaAdapter;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Pista;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PistasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPistas;
    private PistaAdapter pistaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        // Botón Mis Reservas
        Button btnMisReservas = findViewById(R.id.btnMisReservas);
        btnMisReservas.setOnClickListener(v -> {
            Intent intent = new Intent(PistasActivity.this, ReservasActivity.class);
            startActivity(intent);
        });

        // 1. Configurar RecyclerView
        recyclerViewPistas = findViewById(R.id.recyclerViewPistas);
        recyclerViewPistas.setLayoutManager(new LinearLayoutManager(this));

        // 2. Usar RetrofitClient centralizado (con adapters java.time)
        ApiService apiService = RetrofitClient.getApiService();

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
        // Borrar datos de sesión
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("idUsuario");
        editor.remove("nombreUsuario");
        editor.remove("emailUsuario");
        editor.apply();

        // Ir a LoginActivity y limpiar stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                        Intent.FLAG_ACTIVITY_CLEAR_TASK | 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
