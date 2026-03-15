package com.wishport.frontend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.models.Pista;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PistasActivity extends AppCompatActivity {

    Button futbol, baloncesto, tenis, padel, voleibol;
    
    // 1. Variable para guardar el resultado de la API
    String textoDeLaApi = "Cargando datos..."; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        futbol = findViewById(R.id.btnFutbol);
        baloncesto = findViewById(R.id.btnBaloncesto);
        tenis = findViewById(R.id.btnTenis);
        padel = findViewById(R.id.btnPadel);
        voleibol = findViewById(R.id.btnVoleibol);

        // --- CONFIGURACIÓN RETROFIT ---
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        apiService.obtenerPistas().enqueue(new Callback<List<Pista>>() {
            @Override
            public void onResponse(Call<List<Pista>> call, Response<List<Pista>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pista> listaPistas = response.body();
                    if (!listaPistas.isEmpty()) {
                        textoDeLaApi = "¡Conectado a MySQL! Primera pista: " + listaPistas.get(0).getNombre();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Pista>> call, Throwable t) {
                textoDeLaApi = "Error de conexión: " + t.getMessage();
            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PistasActivity.this, ReservasActivity.class);
                
                // 3. METEMOS EL TEXTO EN EL INTENT
                intent.putExtra("DATOS_DEL_BACKEND", textoDeLaApi);
                
                startActivity(intent);
            }
        };

        futbol.setOnClickListener(listener);
        baloncesto.setOnClickListener(listener);
        tenis.setOnClickListener(listener);
        padel.setOnClickListener(listener);
        voleibol.setOnClickListener(listener);
    }
}
