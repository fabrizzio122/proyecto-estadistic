package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.Margenes;

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
public final class Simulador {

    private static final BigDecimal UNO = BigDecimal.ONE;
    private static final int ESCALA = Margenes.ESCALA_RATIO;

    /** Variación que se aplica a cada variable en el análisis de sensibilidad. */
    public static final BigDecimal VARIACION_SENSIBILIDAD = new BigDecimal("0.10");

    /** Efecto de mover una variable hacia arriba y hacia abajo. */
    public record Sensibilidad(Variable variable, BigDecimal utilidadSiBaja, BigDecimal utilidadSiSube,
                               BigDecimal utilidadEscenario) {

        public BigDecimal impacto() {
            return utilidadSiSube.subtract(utilidadSiBaja).abs();
        }
    }

    private Simulador() {
    }

    public static ResultadoEscenario simular(BaseSimulacion b, Supuestos s) {
        BigDecimal volumen = UNO.add(s.volumen());
        BigDecimal ventas = b.ventas().multiply(UNO.add(s.precio())).multiply(volumen);
        BigDecimal costoVentas = b.costoVentas().multiply(volumen).multiply(UNO.add(s.costoUnitario()));
        BigDecimal gastosVariables = b.gastosVariables().multiply(volumen).multiply(UNO.add(s.gastosVariables()));
        BigDecimal costosFijos = b.costosFijos().multiply(UNO.add(s.costosFijos())).add(s.costoFijoAdicional());
        BigDecimal uai = ventas.subtract(costoVentas).subtract(gastosVariables).subtract(costosFijos)
                .add(b.otrosIngresos());
        BigDecimal impuesto = uai.signum() > 0 ? uai.multiply(s.tasaImpuesto()) : BigDecimal.ZERO;
        return new ResultadoEscenario(dinero(ventas), dinero(costoVentas), dinero(gastosVariables), dinero(costosFijos),
                dinero(b.otrosIngresos()), dinero(uai), dinero(impuesto), dinero(uai.subtract(impuesto)));
    }

    /**
     * Utilidad neta si cada variable sube o baja {@value #VARIACION_SENSIBILIDAD_TEXTO} sobre el escenario,
     * ordenada de mayor a menor impacto.
     */
    public static List<Sensibilidad> sensibilidad(BaseSimulacion b, Supuestos s) {
        BigDecimal actual = simular(b, s).utilidadNeta();
        List<Sensibilidad> resultado = new ArrayList<>();
        for (Variable variable : Variable.values()) {
            BigDecimal valor = s.valor(variable);
            BigDecimal baja = simular(b, s.con(variable, valor.subtract(VARIACION_SENSIBILIDAD))).utilidadNeta();
            BigDecimal sube = simular(b, s.con(variable, valor.add(VARIACION_SENSIBILIDAD))).utilidadNeta();
            resultado.add(new Sensibilidad(variable, baja, sube, actual));
        }
        resultado.sort(Comparator.comparing(Sensibilidad::impacto).reversed());
        return resultado;
    }

    /** Texto de la variación de sensibilidad, para documentación. */
    public static final String VARIACION_SENSIBILIDAD_TEXTO = "10 %";

    /**
     * Cambio de precio necesario (con el resto de supuestos igual) para alcanzar una utilidad neta mensual.
     * Vacío si no hay ventas.
     */
    public static Optional<BigDecimal> cambioPrecioParaMeta(BaseSimulacion b, Supuestos s, BigDecimal metaUtilidadNeta) {
        BigDecimal ventasSinPrecio = b.ventas().multiply(UNO.add(s.volumen()));
        if (ventasSinPrecio.signum() <= 0) {
            return Optional.empty();
        }
        ResultadoEscenario r = simular(b, s);
        BigDecimal resto = r.costoVentas().add(r.gastosVariables()).add(r.costosFijos()).subtract(r.otrosIngresos());
        BigDecimal uaiMeta = uaiNecesaria(metaUtilidadNeta, s.tasaImpuesto());
        if (uaiMeta == null) {
            return Optional.empty();
        }
        return Optional.of(uaiMeta.add(resto).divide(ventasSinPrecio, ESCALA, RoundingMode.HALF_UP).subtract(UNO));
    }

    /**
     * Cambio de volumen necesario (con el resto de supuestos igual) para alcanzar una utilidad neta mensual.
     * Vacío si cada venta no deja margen de contribución o si haría falta vender menos que cero.
     */
    public static Optional<BigDecimal> cambioVolumenParaMeta(BaseSimulacion b, Supuestos s, BigDecimal metaUtilidadNeta) {
        BigDecimal contribucionPorUnidadDeVolumen = b.ventas().multiply(UNO.add(s.precio()))
                .subtract(b.costoVentas().multiply(UNO.add(s.costoUnitario())))
                .subtract(b.gastosVariables().multiply(UNO.add(s.gastosVariables())));
        BigDecimal uaiMeta = uaiNecesaria(metaUtilidadNeta, s.tasaImpuesto());
        if (contribucionPorUnidadDeVolumen.signum() <= 0 || uaiMeta == null) {
            return Optional.empty();
        }
        BigDecimal fijos = b.costosFijos().multiply(UNO.add(s.costosFijos())).add(s.costoFijoAdicional());
        BigDecimal cambio = uaiMeta.add(fijos).subtract(b.otrosIngresos())
                .divide(contribucionPorUnidadDeVolumen, ESCALA, RoundingMode.HALF_UP).subtract(UNO);
        return cambio.compareTo(UNO.negate()) < 0 ? Optional.empty() : Optional.of(cambio);
    }

    /** UAI que deja la utilidad neta pedida después de impuestos. */
    private static BigDecimal uaiNecesaria(BigDecimal metaUtilidadNeta, BigDecimal tasa) {
        if (metaUtilidadNeta.signum() <= 0) {
            return metaUtilidadNeta;
        }
        BigDecimal retenido = UNO.subtract(tasa);
        return retenido.signum() <= 0 ? null : metaUtilidadNeta.divide(retenido, ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
