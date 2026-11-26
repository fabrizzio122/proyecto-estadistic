package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosDeudaFGEM(
        CategoriaDeudaFGEM categoriaFGEM,
        String acreedorFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal montoFGEM,
        BigDecimal tasaAnualFGEM,
        BigDecimal cuotaMensualFGEM,
        LocalDate vencimientoFGEM,
        Long cuentaIdFGEM) {

    public DatosDeudaFGEM(CategoriaDeudaFGEM categoriaFGEM, String acreedorFGEM, String descripcionFGEM, LocalDate fechaFGEM, BigDecimal montoFGEM,
                      BigDecimal tasaAnualFGEM, BigDecimal cuotaMensualFGEM, LocalDate vencimientoFGEM) {
        this(categoriaFGEM, acreedorFGEM, descripcionFGEM, fechaFGEM, montoFGEM, tasaAnualFGEM, cuotaMensualFGEM, vencimientoFGEM, null);
    }
}
