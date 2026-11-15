package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un movimiento del hato tal como llegan desde el formulario. */
public record DatosMovimientoGanado(
        LocalDate fecha,
        TipoMovimientoGanado tipo,
        CategoriaGanado categoria,
        Integer cantidad,
        CategoriaGanado categoriaDestino,
        BigDecimal monto,
        String causa,
        String observacion) {
}
