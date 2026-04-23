package com.wishport.frontend.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TypeAdapter para manejar diferentes formatos de fecha/hora del backend.
 * Soporta: ISO8601 (yyyy-MM-dd) y formato hora (HH:mm:ss)
 */
public class DateTimeAdapter extends TypeAdapter<Date> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    @Override
    public void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(dateTimeFormat.format(value));
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        String dateStr = in.nextString();
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Intentar formato fecha completa ISO8601
            if (dateStr.contains("T")) {
                return dateTimeFormat.parse(dateStr);
            }
            // Intentar formato fecha simple (yyyy-MM-dd)
            else if (dateStr.length() == 10 && dateStr.contains("-")) {
                return dateFormat.parse(dateStr);
            }
            // Intentar formato hora (HH:mm:ss)
            else if (dateStr.contains(":")) {
                return timeFormat.parse(dateStr);
            }
        } catch (ParseException e) {
            throw new IOException("No se pudo parsear la fecha: " + dateStr, e);
        }

        return null;
    }
}
