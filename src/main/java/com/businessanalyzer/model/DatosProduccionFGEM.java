package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un registro de producción tal como llegan desde el formulario. */
public record DatosProduccionFGEM(
        EspecieFGEM especieFGEM,
        ProductoGanaderoFGEM productoFGEM,
        LocalDate fechaFGEM,
        Integer diasFGEM,
        BigDecimal cantidadFGEM,
        Integer animalesFGEM,
        BigDecimal precioUnitarioFGEM,
        String observacionFGEM) {
}
