package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.model.GrupoFinancieroFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record BaseSimulacionFGEM(
        PeriodoFGEM periodoFGEM,
        int mesesFGEM,
        BigDecimal ventasFGEM,
        BigDecimal costoVentasFGEM,
        BigDecimal gastosVariablesFGEM,
        BigDecimal costosFijosFGEM,
        BigDecimal otrosIngresosFGEM,
        BigDecimal impuestoRentaFGEM) {

    public static BaseSimulacionFGEM deFGEM(PeriodoFGEM periodoFGEM, int mesesFGEM, ResumenOperacionesFGEM operacionesFGEM,
                                    ResumenFinancieroFGEM finanzasFGEM) {
        BigDecimal divisorFGEM = BigDecimal.valueOf(Math.max(1, mesesFGEM));
        return new BaseSimulacionFGEM(periodoFGEM, mesesFGEM,
                mensualFGEM(operacionesFGEM.ventasNetasFGEM(), divisorFGEM),
                mensualFGEM(operacionesFGEM.costoVentasFGEM(), divisorFGEM),
                mensualFGEM(finanzasFGEM.costosVariablesEquilibrioFGEM(), divisorFGEM),
                mensualFGEM(finanzasFGEM.costosFijosEquilibrioFGEM(), divisorFGEM),
                mensualFGEM(finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.INGRESO_FINANCIERO)
                        .add(finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.OTRO_INGRESO)), divisorFGEM),
                mensualFGEM(finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.IMPUESTO_RENTA), divisorFGEM));
    }

    public BigDecimal utilidadAntesImpuestosFGEM() {
        return ventasFGEM.subtract(costoVentasFGEM).subtract(gastosVariablesFGEM).subtract(costosFijosFGEM).add(otrosIngresosFGEM);
    }

    public BigDecimal tasaImpuestoEfectivaFGEM() {
        BigDecimal uaiFGEM = utilidadAntesImpuestosFGEM();
        if (uaiFGEM.signum() <= 0 || impuestoRentaFGEM.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return impuestoRentaFGEM.divide(uaiFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP).min(BigDecimal.ONE);
    }

    public boolean sinVentasFGEM() {
        return ventasFGEM.signum() == 0;
    }

    private static BigDecimal mensualFGEM(BigDecimal totalFGEM, BigDecimal divisorFGEM) {
        return totalFGEM.divide(divisorFGEM, ESCALA_INTERNA_FGEM, RoundingMode.HALF_UP);
    }

    public static final int ESCALA_INTERNA_FGEM = 10;
}
