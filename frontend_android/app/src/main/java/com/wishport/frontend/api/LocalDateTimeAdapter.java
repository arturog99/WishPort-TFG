package com.wishport.frontend.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * TRADUCTOR DE FECHAS: Ahora incluye manejo de errores (try-catch).
 * Si el servidor envía un dato corrupto, la app ya no se cierra.
 */
public class LocalDateTimeAdapter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    public static final TypeAdapter<LocalDate> LOCAL_DATE = new TypeAdapter<LocalDate>() {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            out.value(value != null ? value.format(DATE_FORMATTER) : null);
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            try {
                String dateStr = in.nextString();
                if (dateStr == null || dateStr.isEmpty()) return null;
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (Exception e) {
                return null; // Fallback seguro
            }
        }
    };

    public static final TypeAdapter<LocalTime> LOCAL_TIME = new TypeAdapter<LocalTime>() {
        @Override
        public void write(JsonWriter out, LocalTime value) throws IOException {
            out.value(value != null ? value.format(TIME_FORMATTER) : null);
        }

        @Override
        public LocalTime read(JsonReader in) throws IOException {
            try {
                String timeStr = in.nextString();
                if (timeStr == null || timeStr.isEmpty()) return null;
                return LocalTime.parse(timeStr, TIME_FORMATTER);
            } catch (Exception e) {
                return null; // Fallback seguro
            }
        }
    };
}
