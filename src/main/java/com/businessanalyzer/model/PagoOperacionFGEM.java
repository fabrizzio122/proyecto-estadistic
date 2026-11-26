package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PagoOperacionFGEM(
        long idFGEM,
        long operacionIdFGEM,
        long cuentaIdFGEM,
        String cuentaFGEM,
        LocalDate fechaFGEM,
        BigDecimal montoFGEM,
        String notaFGEM,
        boolean anuladoFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String anuladoPorFGEM,
        LocalDateTime anuladoEnFGEM) {
}
