package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosMovimientoTesoreriaFGEM(
        TipoMovimientoTesoreriaFGEM tipoFGEM,
        LocalDate fechaFGEM,
        Long cuentaOrigenIdFGEM,
        Long cuentaDestinoIdFGEM,
        BigDecimal montoFGEM,
        String descripcionFGEM) {
}
