package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosActivoFGEM(
        CategoriaActivoFGEM categoriaFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal valorFGEM,
        Integer vidaUtilAniosFGEM,
        BigDecimal valorResidualFGEM,
        Long cuentaIdFGEM) {

    public DatosActivoFGEM(CategoriaActivoFGEM categoriaFGEM, String descripcionFGEM, LocalDate fechaFGEM, BigDecimal valorFGEM,
                       Integer vidaUtilAniosFGEM, BigDecimal valorResidualFGEM) {
        this(categoriaFGEM, descripcionFGEM, fechaFGEM, valorFGEM, vidaUtilAniosFGEM, valorResidualFGEM, null);
    }
}
