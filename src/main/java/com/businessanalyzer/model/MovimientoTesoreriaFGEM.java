package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transferencia entre cuentas, aporte o retiro del dueño.
 *
 * @param cuentaOrigenIdFGEM  cuenta de la que sale el dinero; {@code null} en los aportes
 * @param cuentaDestinoIdFGEM cuenta a la que entra el dinero; {@code null} en los retiros
 */
public record MovimientoTesoreriaFGEM(
        long idFGEM,
        long empresaIdFGEM,
        TipoMovimientoTesoreriaFGEM tipoFGEM,
        LocalDate fechaFGEM,
        Long cuentaOrigenIdFGEM,
        String cuentaOrigenFGEM,
        Long cuentaDestinoIdFGEM,
        String cuentaDestinoFGEM,
        BigDecimal montoFGEM,
        String descripcionFGEM,
        boolean anuladoFGEM,
        String motivoAnulacionFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM) {
}
