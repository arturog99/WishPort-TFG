package com.wishport.frontend.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * TypeAdapter para serializar/deserializar java.time.LocalDate y LocalTime con Gson.
 * Usa formato ISO-8601 estandar.
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
            String dateStr = in.nextString();
            return dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : null;
        }
    };

    public static final TypeAdapter<LocalTime> LOCAL_TIME = new TypeAdapter<LocalTime>() {
        @Override
        public void write(JsonWriter out, LocalTime value) throws IOException {
            out.value(value != null ? value.format(TIME_FORMATTER) : null);
        }

        @Override
        public LocalTime read(JsonReader in) throws IOException {
            String timeStr = in.nextString();
            return timeStr != null ? LocalTime.parse(timeStr, TIME_FORMATTER) : null;
        }
    };
}
