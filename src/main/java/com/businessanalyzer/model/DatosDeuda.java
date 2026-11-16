package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de una deuda tal como llegan desde el formulario.
 *
 * @param cuentaId cuenta a la que ingresó el dinero del préstamo; {@code null} si no ingresó dinero
 *                 (deuda con un proveedor, impuestos, una compra financiada…)
 */
public record DatosDeuda(
        CategoriaDeuda categoria,
        String acreedor,
        String descripcion,
        LocalDate fecha,
        BigDecimal monto,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        LocalDate vencimiento,
        Long cuentaId) {

    /** Deuda por la que no ingresó dinero a las cuentas. */
    public DatosDeuda(CategoriaDeuda categoria, String acreedor, String descripcion, LocalDate fecha, BigDecimal monto,
                      BigDecimal tasaAnual, BigDecimal cuotaMensual, LocalDate vencimiento) {
        this(categoria, acreedor, descripcion, fecha, monto, tasaAnual, cuotaMensual, vencimiento, null);
    }
}
