package com.wishport.frontend.ui;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.wishport.frontend.R;
import com.wishport.frontend.api.ApiService;
import com.wishport.frontend.models.Pista;
import com.wishport.frontend.models.Reserva;
import com.wishport.frontend.models.Usuario;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DetallePistaActivity extends AppCompatActivity {

    public static final String EXTRA_PISTA = "extra_pista";
    private static final int HORA_APERTURA = 8;
    private static final int HORA_CIERRE = 22;
    private static final java.util.TimeZone EUROPE_MADRID_TZ = java.util.TimeZone.getTimeZone("Europe/Madrid");

    private TextView tvNombre, tvDeporte, tvEstado, tvId, tvFechaSeleccionada;
    private Button btnSeleccionarFecha, btnReservar;
    private GridLayout gridHorarios;
    private Pista pistaActual;
    private ApiService apiService;
    private Calendar fechaSeleccionada;
    private int horaInicioSeleccionada = -1;
    private List<Reserva> reservasExistentes = new ArrayList<>();
    private List<Button> botonesHorarios = new ArrayList<>();
    private LinearLayout loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_pista);

        tvNombre = findViewById(R.id.tvDetalleNombre);
        tvDeporte = findViewById(R.id.tvDetalleDeporte);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvId = findViewById(R.id.tvDetalleId);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnReservar = findViewById(R.id.btnReservar);
        gridHorarios = findViewById(R.id.gridHorarios);
        loadingLayout = findViewById(R.id.loadingLayout);
        android.util.Log.d("LOADING", "loadingLayout inicializado: " + (loadingLayout != null));

        // Formato ISO con zona horaria Europe/Madrid (España)
        final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        isoFormat.setTimeZone(EUROPE_MADRID_TZ);
        final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateOnlyFormat.setTimeZone(EUROPE_MADRID_TZ);

        // Serializador: envía fechas en formato ISO con zona horaria
        JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == null) return null;
                return new JsonPrimitive(isoFormat.format(src));
            }
        };

        // Deserializador: convierte fechas ISO del backend (Europe/Madrid) a Date
        JsonDeserializer<Date> dateDeserializer = new JsonDeserializer<Date>() {
            private final SimpleDateFormat isoFormatWithZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            private final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            private final SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            private final SimpleDateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            @Override
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json == null || json.isJsonNull()) return null;
                String dateStr = json.getAsString();
                if (dateStr == null || dateStr.isEmpty()) return null;

                android.util.Log.d("DESERIALIZE", "Parsing: " + dateStr);

                // Primero intentar UTC con .000Z (el más específico)
                if (dateStr.endsWith("Z")) {
                    try {
                        utcFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        Date parsedDate = utcFormat.parse(dateStr);
                        // Convertir a hora local de Madrid
                        java.util.Calendar cal = java.util.Calendar.getInstance(EUROPE_MADRID_TZ);
                        cal.setTime(parsedDate);
                        android.util.Log.d("DESERIALIZE", "Parseado UTC->Madrid: " + cal.getTime());
                        return cal.getTime();
                    } catch (ParseException e) {
                        // Continuar con siguiente formato
                    }
                }

                try {
                    // Intentar formato ISO con zona horaria (ej: 2026-04-22T10:00:00+02:00)
                    isoFormatWithZone.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Madrid"));
                    Date parsedDate = isoFormatWithZone.parse(dateStr);
                    android.util.Log.d("DESERIALIZE", "Parseado con zona: " + parsedDate);
                    return parsedDate;
                } catch (ParseException e) {
                    // Continuar con siguiente formato
                }

                try {
                    // Intentar formato ISO sin zona (asume Europe/Madrid)
                    isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Madrid"));
                    Date parsedDate = isoFormat.parse(dateStr);
                    android.util.Log.d("DESERIALIZE", "Parseado sin zona: " + parsedDate);
                    return parsedDate;
                } catch (ParseException e) {
                    // Continuar con siguiente formato
                }

                try {
                    // Formato solo fecha
                    Date parsedDate = dateOnlyFormat.parse(dateStr);
                    android.util.Log.d("DESERIALIZE", "Parseado solo fecha: " + parsedDate);
                    return parsedDate;
                } catch (ParseException e) {
                    // Continuar con siguiente formato
                }

                try {
                    // Formato UTC con .000Z (fallback si no terminaba en Z por alguna razón)
                    // Parsear como UTC y convertir a Europe/Madrid
                    utcFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    Date parsedDate = utcFormat.parse(dateStr);
                    // Convertir a hora local de Madrid
                    java.util.Calendar cal = java.util.Calendar.getInstance(EUROPE_MADRID_TZ);
                    cal.setTime(parsedDate);
                    android.util.Log.d("DESERIALIZE", "Parseado UTC->Madrid: " + cal.getTime());
                    return cal.getTime();
                } catch (ParseException e) {
                    // Continuar con siguiente formato
                }

                try {
                    // Formato solo hora HH:mm:ss (ej: 09:00:00)
                    // Parsear como hora local y combinar con fecha seleccionada
                    timeOnlyFormat.setTimeZone(EUROPE_MADRID_TZ);
                    Date parsedTime = timeOnlyFormat.parse(dateStr);
                    java.util.Calendar timeCal = java.util.Calendar.getInstance(EUROPE_MADRID_TZ);
                    timeCal.setTime(parsedTime);

                    // Combinar con fecha seleccionada
                    java.util.Calendar resultCal = java.util.Calendar.getInstance(EUROPE_MADRID_TZ);
                    resultCal.setTime(fechaSeleccionada.getTime());
                    resultCal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
                    resultCal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
                    resultCal.set(java.util.Calendar.SECOND, timeCal.get(java.util.Calendar.SECOND));
                    resultCal.set(java.util.Calendar.MILLISECOND, 0);

                    android.util.Log.d("DESERIALIZE", "Parseado HH:mm:ss con fecha: " + resultCal.getTime());
                    return resultCal.getTime();
                } catch (ParseException e) {
                    android.util.Log.e("DESERIALIZE", "ERROR: No se pudo parsear: " + dateStr);
                    throw new JsonParseException("No se pudo parsear la fecha: " + dateStr);
                }
            }
        };

        // Configurar Gson con formato de fecha y estrategia para evitar recursión
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, dateSerializer)
                .registerTypeAdapter(Date.class, dateDeserializer)
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        // Ignorar campos que causan recursión infinita
                        String name = f.getName();
                        return name.equals("reservasList") || name.equals("codigoQr");
                    }
                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();

        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        apiService = retrofit.create(ApiService.class);

        pistaActual = (Pista) getIntent().getSerializableExtra(EXTRA_PISTA);
        fechaSeleccionada = Calendar.getInstance(EUROPE_MADRID_TZ);

        if (pistaActual != null) {
            tvNombre.setText(pistaActual.getNombre());
            tvDeporte.setText("Deporte: " + pistaActual.getDeporte());
            tvEstado.setText("Estado: " + pistaActual.getEstado());
            tvId.setText("ID: " + pistaActual.getIdPista());
        }

        btnSeleccionarFecha.setOnClickListener(v -> mostrarDatePicker());
        btnReservar.setOnClickListener(v -> crearReserva());

        // Inicializar grid de horarios
        inicializarGridHorarios();

        // Limpiar lista al iniciar (por si hay datos de sesión anterior)
        reservasExistentes.clear();

        // Cargar reservas de la fecha actual al iniciar
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvFechaSeleccionada.setText("Fecha: " + sdf.format(fechaSeleccionada.getTime()));
        cargarReservasExistentes();
    }

    private void inicializarGridHorarios() {
        gridHorarios.removeAllViews();
        botonesHorarios.clear();

        for (int hora = HORA_APERTURA; hora < HORA_CIERRE; hora++) {
            Button btnHora = new Button(this);
            btnHora.setText(String.format("%02d:00", hora));
            btnHora.setTag(hora);
            btnHora.setEnabled(false);
            btnHora.setAlpha(0.5f);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            btnHora.setLayoutParams(params);

            final int horaFinal = hora;
            btnHora.setOnClickListener(v -> seleccionarHora(horaFinal));

            botonesHorarios.add(btnHora);
            gridHorarios.addView(btnHora);
        }
    }

    private void mostrarDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    fechaSeleccionada.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    tvFechaSeleccionada.setText("Fecha: " + sdf.format(fechaSeleccionada.getTime()));

                    // LIMPIAR BOTONES INMEDIATAMENTE - antes de cargar datos
                    resetearBotonesHorarios();

                    // Ahora cargar reservas del servidor
                    cargarReservasExistentes();
                },
                fechaSeleccionada.get(Calendar.YEAR),
                fechaSeleccionada.get(Calendar.MONTH),
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        );

        // No permitir fechas pasadas
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private long loadingStartTime = 0;
    private static final long MIN_LOADING_TIME = 500; // Mínimo 500ms visible

    private void cargarReservasExistentes() {
        if (pistaActual == null) return;

        // Mostrar pantalla de carga
        loadingStartTime = System.currentTimeMillis();
        mostrarLoading("Cargando horarios disponibles...");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(EUROPE_MADRID_TZ);
        String fechaStr = sdf.format(fechaSeleccionada.getTime());

        android.util.Log.d("API", "Consultando: pista=" + pistaActual.getIdPista() + ", fecha=" + fechaStr);

        apiService.obtenerReservasPorPistaYFecha(pistaActual.getIdPista(), fechaStr)
                .enqueue(new Callback<List<Reserva>>() {
                    @Override
                    public void onResponse(Call<List<Reserva>> call, Response<List<Reserva>> response) {
                        android.util.Log.d("API", "Response code: " + response.code());
                        android.util.Log.d("API", "Response body null? " + (response.body() == null));
                        if (response.body() != null) {
                            android.util.Log.d("API", "Reservas recibidas: " + response.body().size());
                            for (Reserva r : response.body()) {
                                android.util.Log.d("API", "Reserva: horaInicio=" + r.getHoraInicio());
                            }
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            reservasExistentes.clear();
                            reservasExistentes.addAll(response.body());
                            android.util.Log.d("API", "Total reservas en lista: " + reservasExistentes.size());
                            actualizarHorariosDisponibles();
                        } else {
                            android.util.Log.d("API", "Respuesta vacía o error, limpiando reservas");
                            reservasExistentes.clear();
                            actualizarHorariosDisponibles();
                        }
                        ocultarLoadingConDelay();
                    }

                    @Override
                    public void onFailure(Call<List<Reserva>> call, Throwable t) {
                        android.util.Log.e("API", "Error: " + t.getMessage());
                        reservasExistentes.clear();
                        actualizarHorariosDisponibles();
                        ocultarLoadingConDelay();
                    }
                });
    }

    private void ocultarLoadingConDelay() {
        long elapsed = System.currentTimeMillis() - loadingStartTime;
        long remaining = MIN_LOADING_TIME - elapsed;

        if (remaining > 0) {
            // Esperar el tiempo restante para que se vea el loading
            loadingLayout.postDelayed(() -> ocultarLoading(), remaining);
        } else {
            ocultarLoading();
        }
    }

    private TextView tvLoadingMensaje;

    private void mostrarLoading(String mensaje) {
        android.util.Log.d("LOADING", "Mostrando loading: " + mensaje);
        if (loadingLayout == null) {
            android.util.Log.e("LOADING", "ERROR: loadingLayout es null!");
            return;
        }
        runOnUiThread(() -> {
            if (tvLoadingMensaje == null) {
                tvLoadingMensaje = findViewById(R.id.tvLoadingMensaje);
            }
            if (tvLoadingMensaje != null && mensaje != null) {
                tvLoadingMensaje.setText(mensaje);
            }
            loadingLayout.setVisibility(LinearLayout.VISIBLE);
            android.util.Log.d("LOADING", "Loading visibility cambiado a VISIBLE: " + loadingLayout.getVisibility());
        });
    }

    private void ocultarLoading() {
        android.util.Log.d("LOADING", "Ocultando loading");
        runOnUiThread(() -> {
            loadingLayout.setVisibility(LinearLayout.GONE);
        });
    }

    private void resetearBotonesHorarios() {
        horaInicioSeleccionada = -1;
        reservasExistentes.clear(); // Limpiar reservas antiguas inmediatamente

        for (Button btn : botonesHorarios) {
            btn.setEnabled(false); // Deshabilitar hasta saber si están disponibles
            btn.setAlpha(0.5f);
            btn.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void actualizarHorariosDisponibles() {
        horaInicioSeleccionada = -1;
        
        for (Button btn : botonesHorarios) {
            int hora = (int) btn.getTag();
            boolean ocupado = isHoraOcupada(hora);
            
            if (ocupado) {
                btn.setEnabled(false);
                btn.setAlpha(0.3f);
                btn.setBackgroundColor(Color.GRAY);
            } else {
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
                btn.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    private boolean isHoraOcupada(int hora) {
        android.util.Log.d("DEBUG", "Chequeando hora " + hora + " con " + reservasExistentes.size() + " reservas");
        for (Reserva reserva : reservasExistentes) {
            if (reserva.getHoraInicio() != null && reserva.getHoraFin() != null) {
                // Usar siempre zona horaria Europe/Madrid para comparar horas
                Calendar calInicio = Calendar.getInstance(EUROPE_MADRID_TZ);
                calInicio.setTime(reserva.getHoraInicio());
                int reservaHoraInicio = calInicio.get(Calendar.HOUR_OF_DAY);

                Calendar calFin = Calendar.getInstance(EUROPE_MADRID_TZ);
                calFin.setTime(reserva.getHoraFin());
                int reservaHoraFin = calFin.get(Calendar.HOUR_OF_DAY);

                android.util.Log.d("DEBUG", "Reserva: " + reservaHoraInicio + "-" + reservaHoraFin + " vs " + hora);

                if (hora >= reservaHoraInicio && hora < reservaHoraFin) {
                    android.util.Log.d("DEBUG", "Hora " + hora + " está OCUPADA");
                    return true;
                }
            }
        }
        android.util.Log.d("DEBUG", "Hora " + hora + " está LIBRE");
        return false;
    }

    private void seleccionarHora(int hora) {
        horaInicioSeleccionada = hora;
        
        // Actualizar visual de botones
        for (Button btn : botonesHorarios) {
            int btnHora = (int) btn.getTag();
            if (btnHora == hora) {
                btn.setBackgroundColor(Color.GREEN);
            } else if (!isHoraOcupada(btnHora)) {
                btn.setBackgroundColor(Color.LTGRAY);
            }
        }
        
        Toast.makeText(this, "Horario seleccionado: " + String.format("%02d:00 - %02d:00", hora, hora + 1), Toast.LENGTH_SHORT).show();
    }

    private void crearReserva() {
        if (pistaActual == null) {
            Toast.makeText(this, "Error: No hay pista seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horaInicioSeleccionada == -1) {
            Toast.makeText(this, "Selecciona un horario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener usuario de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("WishPortPrefs", MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        if (idUsuario == -1) {
            Toast.makeText(this, "Error: Usuario no logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el horario no esté ocupado
        if (isHoraOcupada(horaInicioSeleccionada)) {
            Toast.makeText(this, "Este horario ya está ocupado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear fechas (resetear milisegundos para compatibilidad con BD)
        Date fecha = fechaSeleccionada.getTime();
        
        // Fecha solo con día (sin hora) para el campo fecha
        Calendar calFecha = Calendar.getInstance(EUROPE_MADRID_TZ);
        calFecha.setTime(fecha);
        calFecha.set(Calendar.HOUR_OF_DAY, 0);
        calFecha.set(Calendar.MINUTE, 0);
        calFecha.set(Calendar.SECOND, 0);
        calFecha.set(Calendar.MILLISECOND, 0);
        
        // Hora inicio con el horario seleccionado
        Calendar calInicio = Calendar.getInstance(EUROPE_MADRID_TZ);
        calInicio.setTime(fecha);
        calInicio.set(Calendar.HOUR_OF_DAY, horaInicioSeleccionada);
        calInicio.set(Calendar.MINUTE, 0);
        calInicio.set(Calendar.SECOND, 0);
        calInicio.set(Calendar.MILLISECOND, 0);
        
        // Hora fin (una hora después)
        Calendar calFin = Calendar.getInstance(EUROPE_MADRID_TZ);
        calFin.setTime(fecha);
        calFin.set(Calendar.HOUR_OF_DAY, horaInicioSeleccionada + 1);
        calFin.set(Calendar.MINUTE, 0);
        calFin.set(Calendar.SECOND, 0);
        calFin.set(Calendar.MILLISECOND, 0);

        // Crear objetos Usuario y Pista con IDs
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);

        // LOGS para depurar zona horaria
        android.util.Log.d("HORARIO", "Hora seleccionada: " + horaInicioSeleccionada);
        android.util.Log.d("HORARIO", "calInicio.getTime(): " + calInicio.getTime());
        android.util.Log.d("HORARIO", "calInicio.get(Calendar.HOUR_OF_DAY): " + calInicio.get(Calendar.HOUR_OF_DAY));
        android.util.Log.d("HORARIO", "TimeZone default: " + java.util.TimeZone.getDefault().getID());

        Reserva reserva = new Reserva();
        reserva.setFecha(calFecha.getTime());  // Solo fecha, sin hora
        reserva.setHoraInicio(calInicio.getTime());  // Fecha con hora
        reserva.setHoraFin(calFin.getTime());  // Fecha con hora
        reserva.setIdPista(pistaActual);
        reserva.setIdUsuario(usuario);
        reserva.setEstadoReserva("activa");

        // Mostrar pantalla de carga
        mostrarLoading("Procesando reserva...");

        apiService.crearReserva(reserva).enqueue(new Callback<Reserva>() {
            @Override
            public void onResponse(Call<Reserva> call, Response<Reserva> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reservasExistentes.add(response.body());
                    actualizarHorariosDisponibles();
                    Toast.makeText(DetallePistaActivity.this, "¡Reserva creada!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 409) {
                    // El backend dice que el horario está ocupado
                    Toast.makeText(DetallePistaActivity.this,
                            "Este horario ya fue reservado por otro usuario",
                            Toast.LENGTH_LONG).show();
                    // Recargar reservas para actualizar la UI
                    cargarReservasExistentes();
                } else {
                    Toast.makeText(DetallePistaActivity.this,
                            "Error al crear reserva: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
                // Ocultar pantalla de carga
                ocultarLoading();
            }

            @Override
            public void onFailure(Call<Reserva> call, Throwable t) {
                Toast.makeText(DetallePistaActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                // Ocultar pantalla de carga
                ocultarLoading();
            }
        });


    }
}
