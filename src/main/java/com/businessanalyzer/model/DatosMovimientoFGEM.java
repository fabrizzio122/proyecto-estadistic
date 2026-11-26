package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosMovimientoFGEM(
        TipoMovimientoFGEM tipoFGEM,
        CategoriaFinancieraFGEM categoriaFGEM,
        ComportamientoFGEM comportamientoFGEM,
        LocalDate fechaFGEM,
        String descripcionFGEM,
        BigDecimal montoFGEM,
        String comprobanteFGEM,
        Long cuentaIdFGEM) {

    public DatosMovimientoFGEM(TipoMovimientoFGEM tipoFGEM, CategoriaFinancieraFGEM categoriaFGEM, ComportamientoFGEM comportamientoFGEM,
                           LocalDate fechaFGEM, String descripcionFGEM, BigDecimal montoFGEM, String comprobanteFGEM) {
        this(tipoFGEM, categoriaFGEM, comportamientoFGEM, fechaFGEM, descripcionFGEM, montoFGEM, comprobanteFGEM, null);
    }
}
