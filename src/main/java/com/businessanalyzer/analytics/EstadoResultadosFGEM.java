package com.businessanalyzer.analytics;

import com.businessanalyzer.model.GrupoFinancieroFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record EstadoResultadosFGEM(
        BigDecimal ventasNetasFGEM,
        BigDecimal costoVentasFGEM,
        BigDecimal utilidadBrutaFGEM,
        BigDecimal gastosOperativosFGEM,
        BigDecimal utilidadOperativaFGEM,
        BigDecimal ingresosFinancierosFGEM,
        BigDecimal gastosFinancierosFGEM,
        BigDecimal otrosIngresosFGEM,
        BigDecimal utilidadAntesImpuestosFGEM,
        BigDecimal impuestoRentaFGEM,
        BigDecimal utilidadNetaFGEM) {

    public static EstadoResultadosFGEM deFGEM(ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM) {
        BigDecimal ventasFGEM = operacionesFGEM.ventasNetasFGEM();
        BigDecimal costoFGEM = operacionesFGEM.costoVentasFGEM();
        BigDecimal brutaFGEM = ventasFGEM.subtract(costoFGEM);
        BigDecimal gastosOperativosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.GASTO_OPERATIVO);
        BigDecimal operativaFGEM = brutaFGEM.subtract(gastosOperativosFGEM);
        BigDecimal ingresosFinancierosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.INGRESO_FINANCIERO);
        BigDecimal gastosFinancierosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.GASTO_FINANCIERO);
        BigDecimal otrosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.OTRO_INGRESO);
        BigDecimal antesImpuestosFGEM = operativaFGEM.add(ingresosFinancierosFGEM).subtract(gastosFinancierosFGEM).add(otrosFGEM);
        BigDecimal impuestoFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.IMPUESTO_RENTA);
        return new EstadoResultadosFGEM(ventasFGEM, costoFGEM, brutaFGEM, gastosOperativosFGEM, operativaFGEM, ingresosFinancierosFGEM,
                gastosFinancierosFGEM, otrosFGEM, antesImpuestosFGEM, impuestoFGEM, antesImpuestosFGEM.subtract(impuestoFGEM));
    }

    public BigDecimal margenBrutoFGEM() {
        return sobreVentasFGEM(utilidadBrutaFGEM);
    }

    public BigDecimal margenOperativoFGEM() {
        return sobreVentasFGEM(utilidadOperativaFGEM);
    }

    public BigDecimal margenNetoFGEM() {
        return sobreVentasFGEM(utilidadNetaFGEM);
    }

    public BigDecimal sobreVentasFGEM(BigDecimal montoFGEM) {
        return ventasNetasFGEM.signum() == 0
                ? null
                : montoFGEM.divide(ventasNetasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }
}
