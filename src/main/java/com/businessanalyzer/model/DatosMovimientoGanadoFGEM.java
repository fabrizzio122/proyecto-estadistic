package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un movimiento del hato tal como llegan desde el formulario. */
public record DatosMovimientoGanadoFGEM(
        LocalDate fechaFGEM,
        TipoMovimientoGanadoFGEM tipoFGEM,
        CategoriaGanadoFGEM categoriaFGEM,
        Integer cantidadFGEM,
        CategoriaGanadoFGEM categoriaDestinoFGEM,
        BigDecimal montoFGEM,
        String causaFGEM,
        String observacionFGEM) {
}
