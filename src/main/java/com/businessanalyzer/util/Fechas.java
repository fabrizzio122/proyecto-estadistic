package com.businessanalyzer.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Conversión de fechas. En SQLite se guardan como texto ISO-8601
 * ({@code 2026-11-13T14:38:08}), que se ordena correctamente como texto.
 */
public final class Fechas {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter PANTALLA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fechas() {
    }

    public static String aTexto(LocalDateTime fecha) {
        return fecha == null ? null : fecha.truncatedTo(ChronoUnit.SECONDS).format(ISO);
    }

    public static LocalDateTime desdeTexto(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDateTime.parse(texto, ISO);
    }

    public static String paraMostrar(LocalDateTime fecha) {
        return fecha == null ? "—" : fecha.format(PANTALLA);
    }
}
