package com.businessanalyzer.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Conversión de fechas. En SQLite se guardan como texto ISO-8601
 * ({@code 2026-11-13T14:38:08}), que se ordena correctamente como texto.
 */
public final class Fechas {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter PANTALLA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_PANTALLA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ESPANOL = Locale.forLanguageTag("es");

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

    /** Fecha sin hora en formato ISO ({@code 2026-11-13}). */
    public static String fechaATexto(LocalDate fecha) {
        return fecha == null ? null : fecha.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate fechaDesdeTexto(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String paraMostrar(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FECHA_PANTALLA);
    }

    /** Nombre del mes en español con mayúscula inicial (1 = Enero). */
    public static String nombreMes(int mes) {
        String nombre = Month.of(mes).getDisplayName(TextStyle.FULL, ESPANOL);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }
}
