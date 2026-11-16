package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un activo tal como llegan desde el formulario.
 *
 * @param cuentaId cuenta con la que se pagó; {@code null} si no salió dinero de las cuentas
 */
public record DatosActivo(
        CategoriaActivo categoria,
        String descripcion,
        LocalDate fecha,
        BigDecimal valor,
        Integer vidaUtilAnios,
        BigDecimal valorResidual,
        Long cuentaId) {

    /** Activo que no se paga desde las cuentas (ya se tenía o se financió con una deuda). */
    public DatosActivo(CategoriaActivo categoria, String descripcion, LocalDate fecha, BigDecimal valor,
                       Integer vidaUtilAnios, BigDecimal valorResidual) {
        this(categoria, descripcion, fecha, valor, vidaUtilAnios, valorResidual, null);
    }
}
