package com.businessanalyzer.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class FechasFGEM {

    private static final DateTimeFormatter ISO_FGEM = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter PANTALLA_FGEM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_PANTALLA_FGEM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ESPANOL_FGEM = Locale.forLanguageTag("es");

    private FechasFGEM() {
    }

    public static String aTextoFGEM(LocalDateTime fechaFGEM) {
        return fechaFGEM == null ? null : fechaFGEM.truncatedTo(ChronoUnit.SECONDS).format(ISO_FGEM);
    }

    public static LocalDateTime desdeTextoFGEM(String textoFGEM) {
        return textoFGEM == null || textoFGEM.isBlank() ? null : LocalDateTime.parse(textoFGEM, ISO_FGEM);
    }

    public static String paraMostrarFGEM(LocalDateTime fechaFGEM) {
        return fechaFGEM == null ? "—" : fechaFGEM.format(PANTALLA_FGEM);
    }

    public static String fechaATextoFGEM(LocalDate fechaFGEM) {
        return fechaFGEM == null ? null : fechaFGEM.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate fechaDesdeTextoFGEM(String textoFGEM) {
        return textoFGEM == null || textoFGEM.isBlank() ? null : LocalDate.parse(textoFGEM, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String paraMostrarFGEM(LocalDate fechaFGEM) {
        return fechaFGEM == null ? "—" : fechaFGEM.format(FECHA_PANTALLA_FGEM);
    }

    public static String nombreMesFGEM(int mesFGEM) {
        String nombreFGEM = Month.of(mesFGEM).getDisplayName(TextStyle.FULL, ESPANOL_FGEM);
        return Character.toUpperCase(nombreFGEM.charAt(0)) + nombreFGEM.substring(1);
    }
}
