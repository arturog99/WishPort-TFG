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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter timeFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_reserva);

        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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

        String fecha = reserva.getFecha() != null ? reserva.getFecha().format(dateFormatter) : "N/A";
        tvFecha.setText(fecha);

        String horaInicio = reserva.getHoraInicio() != null ? reserva.getHoraInicio().format(timeFormatter) : "N/A";
        String horaFin = reserva.getHoraFin() != null ? reserva.getHoraFin().format(timeFormatter) : "N/A";
        tvHora.setText(horaInicio + " - " + horaFin);

        // Actualizar la lógica de colores para incluir el nuevo estado
        String estado = reserva.getEstadoReserva() != null ? reserva.getEstadoReserva() : "Activa";
        tvEstado.setText(estado);

        // Color según estado
        int colorEstado;
        if (estado.equalsIgnoreCase("activa")) {
            colorEstado = Color.parseColor("#4CAF50"); // Verde
        } else if (estado.equalsIgnoreCase("completada") || estado.equalsIgnoreCase("finalizada")) {
            colorEstado = Color.parseColor("#757575"); // Gris
        } else {
            colorEstado = Color.parseColor("#F44336"); // Rojo (cancelada)
        }
        tvEstado.setTextColor(colorEstado);

        tvIdReserva.setText("#" + reserva.getIdReserva());

        // Verificar si la reserva ya pasó para deshabilitar cancelación
        verificarYConfigurarBotonCancelar();
    }

    private void verificarYConfigurarBotonCancelar() {
        String estado = reserva.getEstadoReserva() != null ? reserva.getEstadoReserva() : "";

        // Verificar si es una reserva pasada (estado completada/finalizada o fecha/hora ya pasó)
        boolean esReservaPasada = estado.equalsIgnoreCase("completada")
                || estado.equalsIgnoreCase("finalizada")
                || esFechaHoraPasada();

        if (esReservaPasada) {
            btnCancelarReserva.setEnabled(false);
            btnCancelarReserva.setAlpha(0.5f);
            btnCancelarReserva.setText("Reserva Finalizada");
        }
    }

    private boolean esFechaHoraPasada() {
        if (reserva.getFecha() == null || reserva.getHoraFin() == null) {
            return false;
        }

        ZoneId madrid = ZoneId.of("Europe/Madrid");
        ZonedDateTime fechaHoraReserva = ZonedDateTime.of(
                reserva.getFecha(),
                reserva.getHoraFin(),
                madrid
        );

        ZonedDateTime ahora = ZonedDateTime.now(madrid);

        return fechaHoraReserva.isBefore(ahora);
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
        // Verificar si la reserva ya pasó
        String estado = reserva.getEstadoReserva() != null ? reserva.getEstadoReserva() : "";
        boolean esReservaPasada = estado.equalsIgnoreCase("completada")
                || estado.equalsIgnoreCase("finalizada")
                || esFechaHoraPasada();

        if (esReservaPasada) {
            Toast.makeText(this, "No puedes cancelar una reserva que ya ha finalizado", Toast.LENGTH_SHORT).show();
            return;
        }

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
