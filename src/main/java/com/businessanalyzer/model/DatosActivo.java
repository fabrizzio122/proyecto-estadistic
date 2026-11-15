package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un activo tal como llegan desde el formulario. */
public record DatosActivo(
        CategoriaActivo categoria,
        String descripcion,
        LocalDate fecha,
        BigDecimal valor,
        Integer vidaUtilAnios,
        BigDecimal valorResidual) {
}
