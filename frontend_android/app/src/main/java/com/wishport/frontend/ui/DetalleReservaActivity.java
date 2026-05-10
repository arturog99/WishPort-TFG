package com.wishport.frontend.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Reserva;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PANTALLA DETALLE DE RESERVA: Muestra el QR y los datos de una reserva.
 * Actualizada para soportar Hilt.
 */
@AndroidEntryPoint
public class DetalleReservaActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA = "extra_reserva";

    private TextView tvDeporte, tvPista, tvFecha, tvHora, tvEstado, tvIdReserva;
    private ImageView ivCodigoQR;
    private Button btnCancelarReserva;
    private ProgressBar progressBar;

    private Reserva reserva;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Inject ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_reserva);

        vincularVistas();

        reserva = (Reserva) getIntent().getSerializableExtra(EXTRA_RESERVA);
        
        if (reserva != null) {
            rellenarDatosPantalla();
            generarImagenQR();
        } else {
            Toast.makeText(this, "Error al cargar la reserva", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnCancelarReserva.setOnClickListener(v -> confirmarCancelacion());
    }

    private void vincularVistas() {
        tvDeporte = findViewById(R.id.tvDeporte);
        tvPista = findViewById(R.id.tvPista);
        tvFecha = findViewById(R.id.tvFecha);
        tvHora = findViewById(R.id.tvHora);
        tvEstado = findViewById(R.id.tvEstado);
        tvIdReserva = findViewById(R.id.tvIdReserva);
        ivCodigoQR = findViewById(R.id.ivCodigoQR);
        btnCancelarReserva = findViewById(R.id.btnCancelarReserva);
        progressBar = findViewById(R.id.progressBar);
    }

    private void rellenarDatosPantalla() {
        String deporte = (reserva.getIdPista() != null) ? reserva.getIdPista().getDeporte() : "Pista";
        String nombrePista = (reserva.getIdPista() != null) ? reserva.getIdPista().getNombre() : "N/A";
        
        tvDeporte.setText(deporte);
        tvPista.setText(nombrePista);

        String fecha = (reserva.getFecha() != null) ? reserva.getFecha().format(dateFormatter) : "N/A";
        String horaI = (reserva.getHoraInicio() != null) ? reserva.getHoraInicio().format(timeFormatter) : "N/A";
        String horaF = (reserva.getHoraFin() != null) ? reserva.getHoraFin().format(timeFormatter) : "N/A";
        
        tvFecha.setText(fecha);
        tvHora.setText(horaI + " - " + horaF);
        tvIdReserva.setText("#" + reserva.getIdReserva());

        configurarVisualEstado();

        if (esReservaAntigua()) {
            desactivarBotonCancelacion();
        }
    }

    private void configurarVisualEstado() {
        String estado = (reserva.getEstadoReserva() != null) ? reserva.getEstadoReserva() : "activa";
        tvEstado.setText(estado.toUpperCase());

        if (estado.equalsIgnoreCase("activa")) {
            tvEstado.setTextColor(Color.parseColor("#4CAF50"));
        } else if (estado.equalsIgnoreCase("cancelada")) {
            tvEstado.setTextColor(Color.parseColor("#F44336"));
            desactivarBotonCancelacion();
        } else {
            tvEstado.setTextColor(Color.GRAY);
        }
    }

    private boolean esReservaAntigua() {
        if (reserva.getFecha() == null || reserva.getHoraFin() == null) return false;
        ZonedDateTime finReserva = ZonedDateTime.of(reserva.getFecha(), reserva.getHoraFin(), ZoneId.of("Europe/Madrid"));
        return finReserva.isBefore(ZonedDateTime.now(ZoneId.of("Europe/Madrid")));
    }

    private void desactivarBotonCancelacion() {
        btnCancelarReserva.setEnabled(false);
        btnCancelarReserva.setAlpha(0.5f);
        btnCancelarReserva.setText("Finalizada o Cancelada");
    }

    private void generarImagenQR() {
        String data = reserva.getCodigoQr();
        if (data == null || data.isEmpty()) data = "ID-" + reserva.getIdReserva();

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);

            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivCodigoQR.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e("QR_ERROR", "No se pudo generar el QR", e);
        }
    }

    private void confirmarCancelacion() {
        new AlertDialog.Builder(this)
                .setTitle("¿Cancelar reserva?")
                .setMessage("Esta acción es irreversible.")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> ejecutarPeticionCancelacion())
                .setNegativeButton("No", null)
                .show();
    }

    private void ejecutarPeticionCancelacion() {
        progressBar.setVisibility(View.VISIBLE);
        btnCancelarReserva.setEnabled(false);

        apiService.cancelarReserva(reserva.getIdReserva()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(DetalleReservaActivity.this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnCancelarReserva.setEnabled(true);
                    Toast.makeText(DetalleReservaActivity.this, "No se pudo cancelar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnCancelarReserva.setEnabled(true);
            }
        });
    }
}
