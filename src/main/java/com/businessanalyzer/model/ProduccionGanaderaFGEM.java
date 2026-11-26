package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProduccionGanaderaFGEM(
        long idFGEM,
        long empresaIdFGEM,
        EspecieFGEM especieFGEM,
        ProductoGanaderoFGEM productoFGEM,
        LocalDate fechaFGEM,
        int diasFGEM,
        BigDecimal cantidadFGEM,
        Integer animalesFGEM,
        BigDecimal precioUnitarioFGEM,
        String observacionFGEM,
        boolean anuladoFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM) {

    public BigDecimal valorFGEM() {
        return precioUnitarioFGEM == null ? null : cantidadFGEM.multiply(precioUnitarioFGEM).setScale(2, RoundingMode.HALF_UP);
    }
}
