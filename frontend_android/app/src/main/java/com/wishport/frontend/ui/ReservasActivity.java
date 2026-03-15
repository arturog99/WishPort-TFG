package com.wishport.frontend.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.wishport.frontend.R;

public class ReservasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_reservas);


        TextView textoEnPantalla = findViewById(R.id.textoResultadoApi);


        String datosRecibidos = getIntent().getStringExtra("DATOS_DEL_BACKEND");


        if (datosRecibidos != null) {
            textoEnPantalla.setText(datosRecibidos);
        }
    }
}
