package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transferencia entre cuentas, aporte o retiro del dueño.
 *
 * @param cuentaOrigenId  cuenta de la que sale el dinero; {@code null} en los aportes
 * @param cuentaDestinoId cuenta a la que entra el dinero; {@code null} en los retiros
 */
public record MovimientoTesoreria(
        long id,
        long empresaId,
        TipoMovimientoTesoreria tipo,
        LocalDate fecha,
        Long cuentaOrigenId,
        String cuentaOrigen,
        Long cuentaDestinoId,
        String cuentaDestino,
        BigDecimal monto,
        String descripcion,
        boolean anulado,
        String motivoAnulacion,
        String registradoPor,
        LocalDateTime registradoEn) {
}
