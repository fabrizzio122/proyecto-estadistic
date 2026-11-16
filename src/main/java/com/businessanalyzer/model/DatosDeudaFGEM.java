package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de una deuda tal como llegan desde el formulario.
 *
 * @param cuentaIdFGEM cuenta a la que ingresó el dinero del préstamo; {@code null} si no ingresó dinero
 *                 (deuda con un proveedor, impuestos, una compra financiada…)
 */
public record DatosDeudaFGEM(
        CategoriaDeudaFGEM categoriaFGEM,
        String acreedorFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal montoFGEM,
        BigDecimal tasaAnualFGEM,
        BigDecimal cuotaMensualFGEM,
        LocalDate vencimientoFGEM,
        Long cuentaIdFGEM) {

    /** Deuda por la que no ingresó dinero a las cuentas. */
    public DatosDeudaFGEM(CategoriaDeudaFGEM categoriaFGEM, String acreedorFGEM, String descripcionFGEM, LocalDate fechaFGEM, BigDecimal montoFGEM,
                      BigDecimal tasaAnualFGEM, BigDecimal cuotaMensualFGEM, LocalDate vencimientoFGEM) {
        this(categoriaFGEM, acreedorFGEM, descripcionFGEM, fechaFGEM, montoFGEM, tasaAnualFGEM, cuotaMensualFGEM, vencimientoFGEM, null);
    }
}
