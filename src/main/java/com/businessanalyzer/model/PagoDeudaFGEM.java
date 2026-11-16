package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pago de una deuda.
 *
 * @param montoFGEM    capital pagado (baja el saldo de la deuda)
 * @param interesFGEM  intereses pagados en la misma cuota (se registran como gasto financiero)
 * @param cuentaIdFGEM cuenta de la que salió el pago; {@code null} en pagos anteriores a la tesorería
 */
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

    /** Capital más intereses: lo que salió de la cuenta. */
    public BigDecimal totalFGEM() {
        return montoFGEM.add(interesFGEM);
    }
}
