package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bien o derecho de la empresa.
 *
 * @param fechaFGEM          fecha de adquisición (activos fijos) o del saldo (activos corrientes)
 * @param valorFGEM          costo de adquisición o saldo, sin impuestos
 * @param vidaUtilAniosFGEM  años en que se deprecia; {@code null} si no se deprecia
 * @param valorResidualFGEM  valor estimado al final de la vida útil
 * @param cuentaIdFGEM       cuenta con la que se pagó la compra; {@code null} si no salió dinero de las cuentas
 *                       (ya se tenía, fue un aporte en especie o se financió con una deuda)
 */
public record ActivoFGEM(
        long idFGEM,
        long empresaIdFGEM,
        CategoriaActivoFGEM categoriaFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal valorFGEM,
        Integer vidaUtilAniosFGEM,
        BigDecimal valorResidualFGEM,
        boolean dadoDeBajaFGEM,
        LocalDate fechaBajaFGEM,
        String motivoBajaFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM,
        Long cuentaIdFGEM) {

    /** Activo que no se pagó desde las cuentas de tesorería. */
    public ActivoFGEM(long idFGEM, long empresaIdFGEM, CategoriaActivoFGEM categoriaFGEM, String descripcionFGEM, LocalDate fechaFGEM,
                  BigDecimal valorFGEM, Integer vidaUtilAniosFGEM, BigDecimal valorResidualFGEM, boolean dadoDeBajaFGEM,
                  LocalDate fechaBajaFGEM, String motivoBajaFGEM, String registradoPorFGEM, LocalDateTime registradoEnFGEM,
                  String actualizadoPorFGEM, LocalDateTime actualizadoEnFGEM) {
        this(idFGEM, empresaIdFGEM, categoriaFGEM, descripcionFGEM, fechaFGEM, valorFGEM, vidaUtilAniosFGEM, valorResidualFGEM, dadoDeBajaFGEM, fechaBajaFGEM,
                motivoBajaFGEM, registradoPorFGEM, registradoEnFGEM, actualizadoPorFGEM, actualizadoEnFGEM, null);
    }

    public boolean seDepreciaFGEM() {
        return vidaUtilAniosFGEM != null && categoriaFGEM.depreciableFGEM();
    }

    /** Forma parte del patrimonio en la fecha indicada. */
    public boolean vigenteEnFGEM(LocalDate corteFGEM) {
        return !fechaFGEM.isAfter(corteFGEM) && (!dadoDeBajaFGEM || fechaBajaFGEM == null || fechaBajaFGEM.isAfter(corteFGEM));
    }
}
