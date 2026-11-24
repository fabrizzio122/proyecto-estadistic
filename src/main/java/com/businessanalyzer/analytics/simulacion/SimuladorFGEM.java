package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.MargenesFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Calcula escenarios "¿qué pasaría si...?" a partir de la base (montos mensuales).
 * <pre>
 * ventas          = base × (1 + precio) × (1 + volumen)
 * costo de ventas = base × (1 + volumen) × (1 + costo unitario)
 * gastos variables= base × (1 + volumen) × (1 + gastos variables)
 * costos fijos    = base × (1 + costos fijos) + costo fijo adicional
 * UAI             = ventas − costo de ventas − gastos variables − costos fijos + otros ingresos
 * impuesto        = UAI × tasa (solo si UAI &gt; 0)
 * </pre>
 * Los cambios de precio y de volumen son independientes: si subir precios hará que se venda
 * menos, hay que indicarlo también en el volumen.
 */
public final class SimuladorFGEM {

    private static final BigDecimal UNO_FGEM = BigDecimal.ONE;
    private static final int ESCALA_FGEM = MargenesFGEM.ESCALA_RATIO_FGEM;

    /** Variación que se aplica a cada variable en el análisis de sensibilidad. */
    public static final BigDecimal VARIACION_SENSIBILIDAD_FGEM = new BigDecimal("0.10");

    /** Efecto de mover una variable hacia arriba y hacia abajo. */
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

    /**
     * Utilidad neta si cada variable sube o baja {@value #VARIACION_SENSIBILIDAD_TEXTO} sobre el escenario,
     * ordenada de mayor a menor impacto.
     */
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

    /** Texto de la variación de sensibilidad, para documentación. */
    public static final String VARIACION_SENSIBILIDAD_TEXTO_FGEM = "10 %";

    /**
     * Cambio de precio necesario (con el resto de supuestos igual) para alcanzar una utilidad neta mensual.
     * Vacío si no hay ventas.
     */
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

    /**
     * Cambio de volumen necesario (con el resto de supuestos igual) para alcanzar una utilidad neta mensual.
     * Vacío si cada venta no deja margen de contribución o si haría falta vender menos que cero.
     */
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

    /** UAI que deja la utilidad neta pedida después de impuestos. */
    private static BigDecimal uaiNecesariaFGEM(BigDecimal metaUtilidadNetaFGEM, BigDecimal tasaFGEM) {
        if (metaUtilidadNetaFGEM.signum() <= 0) {
            return metaUtilidadNetaFGEM;
        }
        BigDecimal retenidoFGEM = UNO_FGEM.subtract(tasaFGEM);
        return retenidoFGEM.signum() <= 0 ? null : metaUtilidadNetaFGEM.divide(retenidoFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    /** Montos del escenario con precisión completa: se redondean a céntimos solo al mostrarlos. */
    private static BigDecimal dineroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(BaseSimulacionFGEM.ESCALA_INTERNA_FGEM, RoundingMode.HALF_UP);
    }
}
