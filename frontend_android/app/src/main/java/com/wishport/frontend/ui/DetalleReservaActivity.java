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
 * PANTALLA DETALLE DE RESERVA: Muestra toda la información de una reserva concreta.
 *
 * Puede abrirse desde dos lugares:
 *   1. ReservasActivity (al pulsar una reserva de la lista).
 *   2. CheckoutActivity (inmediatamente después de crear una reserva).
 *
 * Funcionalidades:
 *   - Muestra deporte, nombre de pista, fecha, hora, estado e ID.
 *   - Genera un código QR a partir del codigoQr de la reserva.
 *   - Permite cancelar la reserva si está "activa" y no ha pasado ya.
 *   - El botón de cancelar se desactiva si la reserva está cancelada o ha terminado.
 */
@AndroidEntryPoint
public class DetalleReservaActivity extends AppCompatActivity {

    /** Clave del Intent para recibir la Reserva desde ReservasActivity o CheckoutActivity */
    public static final String EXTRA_RESERVA = "extra_reserva";

    /** Textos con los datos de la reserva */
    private TextView tvDeporte, tvPista, tvFecha, tvHora, tvEstado, tvIdReserva;
    /** ImageView donde se muestra la imagen del código QR generada */
    private ImageView ivCodigoQR;
    /** Botón de cancelación (se deshabilita si la reserva ya pasó o está cancelada) */
    private Button btnCancelarReserva;
    /** Indicador de carga durante la petición de cancelación */
    private ProgressBar progressBar;

    /** Reserva recibida desde la Activity anterior */
    private Reserva reserva;
    /** Formatea LocalDate -> "dd/MM/yyyy" para mostrar en pantalla */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Formatea LocalTime -> "HH:mm" para mostrar en pantalla */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /** ApiService inyectado por Hilt, usado para el endpoint de cancelación */
    @Inject ApiService apiService;

    /**
     * Inicializa la pantalla:
     * 1. Vincula las vistas.
     * 2. Recoge la Reserva del Intent.
     * 3. Rellena los datos en pantalla.
     * 4. Genera la imagen del QR.
     * 5. Configura el botón de cancelación.
     */
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
            finish(); // Sin datos, no hay nada que mostrar
        }

        btnCancelarReserva.setOnClickListener(v -> confirmarCancelacion());
    }

    /** Vincula las variables con los elementos del XML activity_detalle_reserva */
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

    /**
     * Rellena todos los textos de la pantalla con los datos de la reserva.
     * Aplica formato a las fechas y horas, muestra el estado con color,
     * y comprueba si la reserva ya terminó para desactivar la cancelación.
     */
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
            desactivarBotonCancelacion(); // No se puede cancelar algo que ya pasó
        }
    }

    /**
     * Colorea el texto del estado según su valor:
     * - "activa"   -> Verde (#4CAF50).
     * - "cancelada"-> Rojo (#F44336) y desactiva el botón.
     * - Otro       -> Gris (completada, etc.).
     */
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

    /**
     * Determina si la reserva ya ha pasado (hora de fin anterior a ahora).
     * Usa la zona horaria de Madrid para comparar correctamente.
     * @return true si la reserva ya terminó, false si todavía es válida.
     */
    private boolean esReservaAntigua() {
        if (reserva.getFecha() == null || reserva.getHoraFin() == null) return false;
        ZonedDateTime finReserva = ZonedDateTime.of(reserva.getFecha(), reserva.getHoraFin(), ZoneId.of("Europe/Madrid"));
        return finReserva.isBefore(ZonedDateTime.now(ZoneId.of("Europe/Madrid")));
    }

    /**
     * Deshabilita visualmente el botón de cancelación.
     * Se llama cuando la reserva ya pasó o está cancelada.
     */
    private void desactivarBotonCancelacion() {
        btnCancelarReserva.setEnabled(false);
        btnCancelarReserva.setAlpha(0.5f);
        btnCancelarReserva.setText("Finalizada o Cancelada");
    }

    /**
     * Genera la imagen del código QR usando la librería ZXing.
     * 1. Usa el codigoQr de la reserva (generado por el backend) como contenido del QR.
     *    Si es nulo, usa "ID-{idReserva}" como fallback.
     * 2. Codifica el texto en una matriz de bits (512x512 píxeles).
     * 3. Convierte la matriz en un Bitmap (negro/blanco pixel a pixel).
     * 4. Muestra el Bitmap en el ImageView.
     *
     * Este QR es el que el administrador escanea en AdminActivity para verificar el acceso.
     */
    private void generarImagenQR() {
        String data = reserva.getCodigoQr();
        if (data == null || data.isEmpty()) data = "ID-" + reserva.getIdReserva();

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);

            // Recorremos la matriz y coloreamos cada píxel: negro si es 1, blanco si es 0
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

    /**
     * Muestra un AlertDialog de confirmación antes de cancelar.
     * Protege contra cancelaciones accidentales al pulsar el botón.
     * Si el usuario confirma, llama a ejecutarPeticionCancelacion().
     */
    private void confirmarCancelacion() {
        new AlertDialog.Builder(this)
                .setTitle("¿Cancelar reserva?")
                .setMessage("Esta acción es irreversible.")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> ejecutarPeticionCancelacion())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Envía la petición de cancelación al servidor.
     * DELETE /api/reservas/{idReserva}
     *
     * El servidor devuelve 204 (No Content) si todo fue bien.
     * Retrofit interpreta 204 con Void como éxito.
     * Al cancelar con éxito, cerramos esta pantalla y volvemos a la lista.
     */
    private void ejecutarPeticionCancelacion() {
        progressBar.setVisibility(View.VISIBLE);
        btnCancelarReserva.setEnabled(false);

        apiService.cancelarReserva(reserva.getIdReserva()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(DetalleReservaActivity.this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                    finish(); // Volvemos a ReservasActivity
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
