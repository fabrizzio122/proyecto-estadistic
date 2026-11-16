package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un ingreso o gasto, tal como llegan desde el formulario.
 *
 * @param comportamientoFGEM en gastos, si se deja vacío se usa el sugerido por la categoría
 * @param cuentaIdFGEM       cuenta donde entra o de donde sale el dinero; {@code null} = la cuenta principal
 */
public record DatosMovimientoFGEM(
        TipoMovimientoFGEM tipoFGEM,
        CategoriaFinancieraFGEM categoriaFGEM,
        ComportamientoFGEM comportamientoFGEM,
        LocalDate fechaFGEM,
        String descripcionFGEM,
        BigDecimal montoFGEM,
        String comprobanteFGEM,
        Long cuentaIdFGEM) {

    /** Ingreso o gasto en la cuenta principal. */
    public DatosMovimientoFGEM(TipoMovimientoFGEM tipoFGEM, CategoriaFinancieraFGEM categoriaFGEM, ComportamientoFGEM comportamientoFGEM,
                           LocalDate fechaFGEM, String descripcionFGEM, BigDecimal montoFGEM, String comprobanteFGEM) {
        this(tipoFGEM, categoriaFGEM, comportamientoFGEM, fechaFGEM, descripcionFGEM, montoFGEM, comprobanteFGEM, null);
    }
}
