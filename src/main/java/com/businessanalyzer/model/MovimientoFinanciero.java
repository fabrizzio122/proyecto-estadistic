package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ingreso o gasto que no proviene de vender o comprar mercadería.
 *
 * @param comportamiento fijo o variable en gastos; {@code null} en ingresos
 * @param monto          importe sin impuestos, siempre positivo
 */
public record MovimientoFinanciero(
        long id,
        long empresaId,
        TipoMovimiento tipo,
        CategoriaFinanciera categoria,
        Comportamiento comportamiento,
        LocalDate fecha,
        String descripcion,
        BigDecimal monto,
        String comprobante,
        boolean anulado,
        String registradoPor,
        LocalDateTime registradoEn,
        String actualizadoPor,
        LocalDateTime actualizadoEn,
        String motivoAnulacion,
        String anuladoPor,
        LocalDateTime anuladoEn) {
}
