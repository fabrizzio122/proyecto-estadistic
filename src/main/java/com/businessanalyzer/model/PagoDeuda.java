package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pago de capital de una deuda. */
public record PagoDeuda(
        long id,
        long deudaId,
        LocalDate fecha,
        BigDecimal monto,
        String nota,
        boolean anulado,
        String registradoPor,
        LocalDateTime registradoEn,
        String anuladoPor,
        LocalDateTime anuladoEn) {
}
