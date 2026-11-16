package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bien o derecho de la empresa.
 *
 * @param fecha          fecha de adquisición (activos fijos) o del saldo (activos corrientes)
 * @param valor          costo de adquisición o saldo, sin impuestos
 * @param vidaUtilAnios  años en que se deprecia; {@code null} si no se deprecia
 * @param valorResidual  valor estimado al final de la vida útil
 * @param cuentaId       cuenta con la que se pagó la compra; {@code null} si no salió dinero de las cuentas
 *                       (ya se tenía, fue un aporte en especie o se financió con una deuda)
 */
public record Activo(
        long id,
        long empresaId,
        CategoriaActivo categoria,
        String descripcion,
        LocalDate fecha,
        BigDecimal valor,
        Integer vidaUtilAnios,
        BigDecimal valorResidual,
        boolean dadoDeBaja,
        LocalDate fechaBaja,
        String motivoBaja,
        String registradoPor,
        LocalDateTime registradoEn,
        String actualizadoPor,
        LocalDateTime actualizadoEn,
        Long cuentaId) {

    /** Activo que no se pagó desde las cuentas de tesorería. */
    public Activo(long id, long empresaId, CategoriaActivo categoria, String descripcion, LocalDate fecha,
                  BigDecimal valor, Integer vidaUtilAnios, BigDecimal valorResidual, boolean dadoDeBaja,
                  LocalDate fechaBaja, String motivoBaja, String registradoPor, LocalDateTime registradoEn,
                  String actualizadoPor, LocalDateTime actualizadoEn) {
        this(id, empresaId, categoria, descripcion, fecha, valor, vidaUtilAnios, valorResidual, dadoDeBaja, fechaBaja,
                motivoBaja, registradoPor, registradoEn, actualizadoPor, actualizadoEn, null);
    }

    public boolean seDeprecia() {
        return vidaUtilAnios != null && categoria.depreciable();
    }

    /** Forma parte del patrimonio en la fecha indicada. */
    public boolean vigenteEn(LocalDate corte) {
        return !fecha.isAfter(corte) && (!dadoDeBaja || fechaBaja == null || fechaBaja.isAfter(corte));
    }
}
