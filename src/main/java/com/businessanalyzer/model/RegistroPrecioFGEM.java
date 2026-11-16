package com.businessanalyzer.model;

import com.businessanalyzer.analytics.MargenesFGEM;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Costo y precio vigentes a partir de una fecha (historial de precios). */
public record RegistroPrecioFGEM(LocalDateTime fechaFGEM, BigDecimal costoUnitarioFGEM, BigDecimal precioVentaFGEM, String usuarioFGEM) {

    public MargenesFGEM.MargenFGEM margenFGEM() {
        return MargenesFGEM.calcularFGEM(costoUnitarioFGEM, precioVentaFGEM);
    }
}
