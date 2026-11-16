package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de una transferencia, aporte o retiro tal como llegan desde el formulario. */
public record DatosMovimientoTesoreriaFGEM(
        TipoMovimientoTesoreriaFGEM tipoFGEM,
        LocalDate fechaFGEM,
        Long cuentaOrigenIdFGEM,
        Long cuentaDestinoIdFGEM,
        BigDecimal montoFGEM,
        String descripcionFGEM) {
}
