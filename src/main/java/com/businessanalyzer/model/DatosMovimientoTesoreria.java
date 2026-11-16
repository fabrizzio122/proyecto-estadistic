package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de una transferencia, aporte o retiro tal como llegan desde el formulario. */
public record DatosMovimientoTesoreria(
        TipoMovimientoTesoreria tipo,
        LocalDate fecha,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String descripcion) {
}
