package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActivoFGEM(
        long idFGEM,
        long empresaIdFGEM,
        CategoriaActivoFGEM categoriaFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal valorFGEM,
        Integer vidaUtilAniosFGEM,
        BigDecimal valorResidualFGEM,
        boolean dadoDeBajaFGEM,
        LocalDate fechaBajaFGEM,
        String motivoBajaFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM,
        Long cuentaIdFGEM) {

    public ActivoFGEM(long idFGEM, long empresaIdFGEM, CategoriaActivoFGEM categoriaFGEM, String descripcionFGEM, LocalDate fechaFGEM,
                  BigDecimal valorFGEM, Integer vidaUtilAniosFGEM, BigDecimal valorResidualFGEM, boolean dadoDeBajaFGEM,
                  LocalDate fechaBajaFGEM, String motivoBajaFGEM, String registradoPorFGEM, LocalDateTime registradoEnFGEM,
                  String actualizadoPorFGEM, LocalDateTime actualizadoEnFGEM) {
        this(idFGEM, empresaIdFGEM, categoriaFGEM, descripcionFGEM, fechaFGEM, valorFGEM, vidaUtilAniosFGEM, valorResidualFGEM, dadoDeBajaFGEM, fechaBajaFGEM,
                motivoBajaFGEM, registradoPorFGEM, registradoEnFGEM, actualizadoPorFGEM, actualizadoEnFGEM, null);
    }

    public boolean seDepreciaFGEM() {
        return vidaUtilAniosFGEM != null && categoriaFGEM.depreciableFGEM();
    }

    public boolean vigenteEnFGEM(LocalDate corteFGEM) {
        return !fechaFGEM.isAfter(corteFGEM) && (!dadoDeBajaFGEM || fechaBajaFGEM == null || fechaBajaFGEM.isAfter(corteFGEM));
    }
}
