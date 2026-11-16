package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un activo tal como llegan desde el formulario.
 *
 * @param cuentaIdFGEM cuenta con la que se pagó; {@code null} si no salió dinero de las cuentas
 */
public record DatosActivoFGEM(
        CategoriaActivoFGEM categoriaFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal valorFGEM,
        Integer vidaUtilAniosFGEM,
        BigDecimal valorResidualFGEM,
        Long cuentaIdFGEM) {

    /** Activo que no se paga desde las cuentas (ya se tenía o se financió con una deuda). */
    public DatosActivoFGEM(CategoriaActivoFGEM categoriaFGEM, String descripcionFGEM, LocalDate fechaFGEM, BigDecimal valorFGEM,
                       Integer vidaUtilAniosFGEM, BigDecimal valorResidualFGEM) {
        this(categoriaFGEM, descripcionFGEM, fechaFGEM, valorFGEM, vidaUtilAniosFGEM, valorResidualFGEM, null);
    }
}
