package com.example.wishport;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wishport.Pantalla_Reservas;

public class Pantalla_Deportes extends AppCompatActivity {

    Button futbol, baloncesto, tenis, padel, voleibol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_deportes);

        futbol = findViewById(R.id.btnFutbol);
        baloncesto = findViewById(R.id.btnBaloncesto);
        tenis = findViewById(R.id.btnTenis);
        padel = findViewById(R.id.btnPadel);
        voleibol = findViewById(R.id.btnVoleibol);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Pantalla_Deportes.this, Pantalla_Reservas.class);
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
