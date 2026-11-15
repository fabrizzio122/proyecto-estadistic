package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un registro de producción tal como llegan desde el formulario. */
public record DatosProduccion(
        Especie especie,
        ProductoGanadero producto,
        LocalDate fecha,
        Integer dias,
        BigDecimal cantidad,
        Integer animales,
        BigDecimal precioUnitario,
        String observacion) {
}
