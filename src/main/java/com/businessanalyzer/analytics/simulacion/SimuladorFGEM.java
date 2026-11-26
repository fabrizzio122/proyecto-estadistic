package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.MargenesFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SimuladorFGEM {

    private static final BigDecimal UNO_FGEM = BigDecimal.ONE;
    private static final int ESCALA_FGEM = MargenesFGEM.ESCALA_RATIO_FGEM;

    public static final BigDecimal VARIACION_SENSIBILIDAD_FGEM = new BigDecimal("0.10");

    public record SensibilidadFGEM(VariableFGEM variableFGEM, BigDecimal utilidadSiBajaFGEM, BigDecimal utilidadSiSubeFGEM,
                               BigDecimal utilidadEscenarioFGEM) {

        public BigDecimal impactoFGEM() {
            return utilidadSiSubeFGEM.subtract(utilidadSiBajaFGEM).abs();
        }
    }

    private SimuladorFGEM() {
    }

    public static ResultadoEscenarioFGEM simularFGEM(BaseSimulacionFGEM bFGEM, SupuestosFGEM sFGEM) {
        BigDecimal volumenFGEM = UNO_FGEM.add(sFGEM.volumenFGEM());
        BigDecimal ventasFGEM = bFGEM.ventasFGEM().multiply(UNO_FGEM.add(sFGEM.precioFGEM())).multiply(volumenFGEM);
        BigDecimal costoVentasFGEM = bFGEM.costoVentasFGEM().multiply(volumenFGEM).multiply(UNO_FGEM.add(sFGEM.costoUnitarioFGEM()));
        BigDecimal gastosVariablesFGEM = bFGEM.gastosVariablesFGEM().multiply(volumenFGEM).multiply(UNO_FGEM.add(sFGEM.gastosVariablesFGEM()));
        BigDecimal costosFijosFGEM = bFGEM.costosFijosFGEM().multiply(UNO_FGEM.add(sFGEM.costosFijosFGEM())).add(sFGEM.costoFijoAdicionalFGEM());
        BigDecimal uaiFGEM = ventasFGEM.subtract(costoVentasFGEM).subtract(gastosVariablesFGEM).subtract(costosFijosFGEM)
                .add(bFGEM.otrosIngresosFGEM());
        BigDecimal impuestoFGEM = uaiFGEM.signum() > 0 ? uaiFGEM.multiply(sFGEM.tasaImpuestoFGEM()) : BigDecimal.ZERO;
        return new ResultadoEscenarioFGEM(dineroFGEM(ventasFGEM), dineroFGEM(costoVentasFGEM), dineroFGEM(gastosVariablesFGEM), dineroFGEM(costosFijosFGEM),
                dineroFGEM(bFGEM.otrosIngresosFGEM()), dineroFGEM(uaiFGEM), dineroFGEM(impuestoFGEM), dineroFGEM(uaiFGEM.subtract(impuestoFGEM)));
    }

    public static List<SensibilidadFGEM> sensibilidadFGEM(BaseSimulacionFGEM bFGEM, SupuestosFGEM sFGEM) {
        BigDecimal actualFGEM = simularFGEM(bFGEM, sFGEM).utilidadNetaFGEM();
        List<SensibilidadFGEM> resultadoFGEM = new ArrayList<>();
        for (VariableFGEM variableFGEM : VariableFGEM.values()) {
            BigDecimal valorFGEM = sFGEM.valorFGEM(variableFGEM);
            BigDecimal bajaFGEM = simularFGEM(bFGEM, sFGEM.conFGEM(variableFGEM, valorFGEM.subtract(VARIACION_SENSIBILIDAD_FGEM))).utilidadNetaFGEM();
            BigDecimal subeFGEM = simularFGEM(bFGEM, sFGEM.conFGEM(variableFGEM, valorFGEM.add(VARIACION_SENSIBILIDAD_FGEM))).utilidadNetaFGEM();
            resultadoFGEM.add(new SensibilidadFGEM(variableFGEM, bajaFGEM, subeFGEM, actualFGEM));
        }
        resultadoFGEM.sort(Comparator.comparing(SensibilidadFGEM::impactoFGEM).reversed());
        return resultadoFGEM;
    }

    public static final String VARIACION_SENSIBILIDAD_TEXTO_FGEM = "10 %";

    public static Optional<BigDecimal> cambioPrecioParaMetaFGEM(BaseSimulacionFGEM bFGEM, SupuestosFGEM sFGEM, BigDecimal metaUtilidadNetaFGEM) {
        BigDecimal ventasSinPrecioFGEM = bFGEM.ventasFGEM().multiply(UNO_FGEM.add(sFGEM.volumenFGEM()));
        if (ventasSinPrecioFGEM.signum() <= 0) {
            return Optional.empty();
        }
        ResultadoEscenarioFGEM rFGEM = simularFGEM(bFGEM, sFGEM);
        BigDecimal restoFGEM = rFGEM.costoVentasFGEM().add(rFGEM.gastosVariablesFGEM()).add(rFGEM.costosFijosFGEM()).subtract(rFGEM.otrosIngresosFGEM());
        BigDecimal uaiMetaFGEM = uaiNecesariaFGEM(metaUtilidadNetaFGEM, sFGEM.tasaImpuestoFGEM());
        if (uaiMetaFGEM == null) {
            return Optional.empty();
        }
        return Optional.of(uaiMetaFGEM.add(restoFGEM).divide(ventasSinPrecioFGEM, ESCALA_FGEM, RoundingMode.HALF_UP).subtract(UNO_FGEM));
    }

    public static Optional<BigDecimal> cambioVolumenParaMetaFGEM(BaseSimulacionFGEM bFGEM, SupuestosFGEM sFGEM, BigDecimal metaUtilidadNetaFGEM) {
        BigDecimal contribucionPorUnidadDeVolumenFGEM = bFGEM.ventasFGEM().multiply(UNO_FGEM.add(sFGEM.precioFGEM()))
                .subtract(bFGEM.costoVentasFGEM().multiply(UNO_FGEM.add(sFGEM.costoUnitarioFGEM())))
                .subtract(bFGEM.gastosVariablesFGEM().multiply(UNO_FGEM.add(sFGEM.gastosVariablesFGEM())));
        BigDecimal uaiMetaFGEM = uaiNecesariaFGEM(metaUtilidadNetaFGEM, sFGEM.tasaImpuestoFGEM());
        if (contribucionPorUnidadDeVolumenFGEM.signum() <= 0 || uaiMetaFGEM == null) {
            return Optional.empty();
        }
        BigDecimal fijosFGEM = bFGEM.costosFijosFGEM().multiply(UNO_FGEM.add(sFGEM.costosFijosFGEM())).add(sFGEM.costoFijoAdicionalFGEM());
        BigDecimal cambioFGEM = uaiMetaFGEM.add(fijosFGEM).subtract(bFGEM.otrosIngresosFGEM())
                .divide(contribucionPorUnidadDeVolumenFGEM, ESCALA_FGEM, RoundingMode.HALF_UP).subtract(UNO_FGEM);
        return cambioFGEM.compareTo(UNO_FGEM.negate()) < 0 ? Optional.empty() : Optional.of(cambioFGEM);
    }

    private static BigDecimal uaiNecesariaFGEM(BigDecimal metaUtilidadNetaFGEM, BigDecimal tasaFGEM) {
        if (metaUtilidadNetaFGEM.signum() <= 0) {
            return metaUtilidadNetaFGEM;
        }
        BigDecimal retenidoFGEM = UNO_FGEM.subtract(tasaFGEM);
        return retenidoFGEM.signum() <= 0 ? null : metaUtilidadNetaFGEM.divide(retenidoFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    private static BigDecimal dineroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(BaseSimulacionFGEM.ESCALA_INTERNA_FGEM, RoundingMode.HALF_UP);
    }
}
