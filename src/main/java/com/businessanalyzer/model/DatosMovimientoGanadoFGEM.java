package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
