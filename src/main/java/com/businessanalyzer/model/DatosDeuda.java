package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de una deuda tal como llegan desde el formulario. */
public record DatosDeuda(
        CategoriaDeuda categoria,
        String acreedor,
        String descripcion,
        LocalDate fecha,
        BigDecimal monto,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        LocalDate vencimiento) {
}
