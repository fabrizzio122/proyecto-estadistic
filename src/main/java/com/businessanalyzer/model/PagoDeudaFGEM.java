package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PagoDeudaFGEM(
        long idFGEM,
        long deudaIdFGEM,
        LocalDate fechaFGEM,
        BigDecimal montoFGEM,
        String notaFGEM,
        boolean anuladoFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String anuladoPorFGEM,
        LocalDateTime anuladoEnFGEM,
        BigDecimal interesFGEM,
        Long cuentaIdFGEM,
        String cuentaFGEM) {

    public PagoDeudaFGEM {
        interesFGEM = interesFGEM == null ? BigDecimal.ZERO : interesFGEM;
    }

    public BigDecimal totalFGEM() {
        return montoFGEM.add(interesFGEM);
    }
}
