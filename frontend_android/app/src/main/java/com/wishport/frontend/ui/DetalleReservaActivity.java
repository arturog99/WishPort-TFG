package com.wishport.frontend.ui;

import android.content.Intent;
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

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleReservaActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA = "extra_reserva";

    private TextView tvDeporte, tvPista, tvFecha, tvHora, tvEstado, tvIdReserva;
    private ImageView ivCodigoQR;
    private Button btnCancelarReserva;
    private ProgressBar progressBar;

    private Reserva reserva;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_reserva);

        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        initViews();

        reserva = (Reserva) getIntent().getSerializableExtra(EXTRA_RESERVA);
        if (reserva == null) {
            Toast.makeText(this, "Error: No se pudo cargar la reserva", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mostrarDatosReserva();
        generarCodigoQR();

        btnCancelarReserva.setOnClickListener(v -> mostrarDialogoCancelar());
    }

    private void initViews() {
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

    private void mostrarDatosReserva() {
        String deporte = reserva.getIdPista() != null && reserva.getIdPista().getDeporte() != null
                ? reserva.getIdPista().getDeporte() : "Pádel";
        tvDeporte.setText(deporte);

        String pista = reserva.getIdPista() != null && reserva.getIdPista().getNombre() != null
                ? reserva.getIdPista().getNombre() : "Pista " + (reserva.getIdPista() != null ? reserva.getIdPista().getIdPista() : "1");
        tvPista.setText(pista);

        String fecha = reserva.getFecha() != null ? dateFormat.format(reserva.getFecha()) : "N/A";
        tvFecha.setText(fecha);

        String horaInicio = reserva.getHoraInicio() != null ? timeFormat.format(reserva.getHoraInicio()) : "N/A";
        String horaFin = reserva.getHoraFin() != null ? timeFormat.format(reserva.getHoraFin()) : "N/A";
        tvHora.setText(horaInicio + " - " + horaFin);

        String estado = reserva.getEstadoReserva() != null ? reserva.getEstadoReserva() : "Activa";
        tvEstado.setText(estado);
        tvEstado.setTextColor(estado.equalsIgnoreCase("activa") ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        tvIdReserva.setText("#" + reserva.getIdReserva());
    }

    private void generarCodigoQR() {
        String qrData = reserva.getCodigoQr();
        if (qrData == null || qrData.isEmpty()) {
            qrData = "RESERVA-" + reserva.getIdReserva();
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ivCodigoQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e("QR", "Error generando QR", e);
            Toast.makeText(this, "Error generando código QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoCancelar() {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar Reserva")
                .setMessage("¿Estás seguro de que quieres cancelar esta reserva? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, Cancelar", (dialog, which) -> cancelarReserva())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelarReserva() {
        progressBar.setVisibility(View.VISIBLE);
        btnCancelarReserva.setEnabled(false);

        ApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.cancelarReserva(reserva.getIdReserva());

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(DetalleReservaActivity.this, "Reserva cancelada correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnCancelarReserva.setEnabled(true);
                    Toast.makeText(DetalleReservaActivity.this, "Error al cancelar la reserva", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnCancelarReserva.setEnabled(true);
                Toast.makeText(DetalleReservaActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
