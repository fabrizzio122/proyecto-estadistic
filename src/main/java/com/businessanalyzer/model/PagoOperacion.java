package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cobro de una venta al crédito o pago de una compra al crédito. */
public record PagoOperacion(
        long id,
        long operacionId,
        long cuentaId,
        String cuenta,
        LocalDate fecha,
        BigDecimal monto,
        String nota,
        boolean anulado,
        String registradoPor,
        LocalDateTime registradoEn,
        String anuladoPor,
        LocalDateTime anuladoEn) {
}
