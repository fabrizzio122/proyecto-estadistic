package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pago de una deuda.
 *
 * @param monto    capital pagado (baja el saldo de la deuda)
 * @param interes  intereses pagados en la misma cuota (se registran como gasto financiero)
 * @param cuentaId cuenta de la que salió el pago; {@code null} en pagos anteriores a la tesorería
 */
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
        LocalDateTime anuladoEn,
        BigDecimal interes,
        Long cuentaId,
        String cuenta) {

    public PagoDeuda {
        interes = interes == null ? BigDecimal.ZERO : interes;
    }

    /** Capital más intereses: lo que salió de la cuenta. */
    public BigDecimal total() {
        return monto.add(interes);
    }
}
