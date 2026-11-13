package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un ingreso o gasto, tal como llegan desde el formulario.
 *
 * @param comportamiento en gastos, si se deja vacío se usa el sugerido por la categoría
 */
public record DatosMovimiento(
        TipoMovimiento tipo,
        CategoriaFinanciera categoria,
        Comportamiento comportamiento,
        LocalDate fecha,
        String descripcion,
        BigDecimal monto,
        String comprobante) {
}
