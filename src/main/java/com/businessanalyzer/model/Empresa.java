package com.businessanalyzer.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Empresa o negocio que se analiza.
 *
 * @param mesInicioEjercicio mes (1-12) en que empieza su año fiscal; los análisis anuales lo usan
 * @param activa             {@code false} si está archivada (no aparece en el selector)
 */
public record Empresa(
        long id,
        String razonSocial,
        String nombreComercial,
        String identificacionFiscal,
        Sector sector,
        TamanoEmpresa tamano,
        Moneda moneda,
        int mesInicioEjercicio,
        LocalDate fechaInicioOperaciones,
        String direccion,
        String telefono,
        String email,
        boolean activa,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {

    /** Nombre comercial si existe; si no, la razón social. */
    public String nombreVisible() {
        return nombreComercial != null && !nombreComercial.isBlank() ? nombreComercial : razonSocial;
    }

    @Override
    public String toString() {
        return nombreVisible();
    }
}
