package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un ingreso o gasto, tal como llegan desde el formulario.
 *
 * @param comportamiento en gastos, si se deja vacío se usa el sugerido por la categoría
 * @param cuentaId       cuenta donde entra o de donde sale el dinero; {@code null} = la cuenta principal
 */
public record DatosMovimiento(
        TipoMovimiento tipo,
        CategoriaFinanciera categoria,
        Comportamiento comportamiento,
        LocalDate fecha,
        String descripcion,
        BigDecimal monto,
        String comprobante,
        Long cuentaId) {

    /** Ingreso o gasto en la cuenta principal. */
    public DatosMovimiento(TipoMovimiento tipo, CategoriaFinanciera categoria, Comportamiento comportamiento,
                           LocalDate fecha, String descripcion, BigDecimal monto, String comprobante) {
        this(tipo, categoria, comportamiento, fecha, descripcion, monto, comprobante, null);
    }
}
