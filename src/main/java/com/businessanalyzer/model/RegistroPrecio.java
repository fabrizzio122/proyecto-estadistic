package com.businessanalyzer.model;

import com.businessanalyzer.analytics.Margenes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Costo y precio vigentes a partir de una fecha (historial de precios). */
public record RegistroPrecio(LocalDateTime fecha, BigDecimal costoUnitario, BigDecimal precioVenta, String usuario) {

    public Margenes.Margen margen() {
        return Margenes.calcular(costoUnitario, precioVenta);
    }
}
