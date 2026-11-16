package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuánto vale el negocio, por tres métodos:
 * <ol>
 *   <li><b>Patrimonial:</b> activos − deudas del balance general (el ganado a precio de mercado y los
 *       cultivos en proceso al costo).</li>
 *   <li><b>Capitalización de utilidades:</b> utilidad neta anual ÷ tasa de descuento (un negocio que gana
 *       lo mismo cada año, para siempre).</li>
 *   <li><b>Flujos descontados:</b> flujo anual para el dueño (utilidad neta + depreciación, que no es
 *       salida de dinero) proyectado con el crecimiento elegido, más un valor terminal con crecimiento
 *       de largo plazo, todo traído a valor de hoy con la tasa de descuento.</li>
 * </ol>
 * <b>Valor de referencia:</b> el promedio de los dos métodos por rentabilidad si supera al patrimonial
 * (el negocio vale más por lo que gana que por lo que tiene: la diferencia es el fondo de comercio);
 * si no, el valor patrimonial.
 *
 * @param flujoAnualFGEM       utilidad neta anual + depreciación anual
 * @param capitalizacionFGEM   {@code null} si la utilidad no es positiva
 * @param flujosDescontadosFGEM {@code null} si el flujo no es positivo
 * @param valorRentabilidadFGEM promedio de los métodos por rentabilidad disponibles; {@code null} si ninguno
 * @param fondoComercioFGEM    valor por rentabilidad − patrimonial; {@code null} sin valor por rentabilidad
 * @param mesesResultadosFGEM  meses con ventas usados para calcular la utilidad anual
 */
public record ValoracionNegocioFGEM(SupuestosValoracionFGEM supuestosFGEM, BigDecimal patrimonialFGEM, BigDecimal utilidadAnualFGEM,
                                BigDecimal depreciacionAnualFGEM, BigDecimal flujoAnualFGEM, BigDecimal capitalizacionFGEM,
                                BigDecimal flujosDescontadosFGEM, List<ProyeccionFGEM> proyeccionFGEM, BigDecimal valorTerminalFGEM,
                                BigDecimal valorTerminalPresenteFGEM, BigDecimal valorRentabilidadFGEM, BigDecimal fondoComercioFGEM,
                                BigDecimal valorReferenciaFGEM, BaseFGEM baseFGEM, BigDecimal minimoFGEM, BigDecimal maximoFGEM,
                                int mesesResultadosFGEM) {

    /** Meses de resultados por debajo de los cuales la valoración por rentabilidad es poco confiable. */
    public static final int MESES_CONFIABLES_FGEM = 6;
    private static final MathContext PRECISION_FGEM = new MathContext(20, RoundingMode.HALF_UP);

    /** Qué método define el valor de referencia. */
    public enum BaseFGEM { RENTABILIDAD, PATRIMONIO }

    /** Un año proyectado. */
    public record ProyeccionFGEM(int anioFGEM, BigDecimal flujoFGEM, BigDecimal factorFGEM, BigDecimal valorPresenteFGEM) {
    }

    /** Fila de la tabla de sensibilidad: valor por flujos descontados para cada combinación. */
    public record SensibilidadFGEM(List<BigDecimal> tasasFGEM, List<BigDecimal> crecimientosFGEM, BigDecimal[][] valoresFGEM) {
    }

    /**
     * @param utilidadAnualFGEM   utilidad neta llevada a 12 meses; {@code null} si aún no hay ventas
     * @param depreciacionMensualFGEM depreciación mensual de los activos fijos
     */
    public static ValoracionNegocioFGEM calcularFGEM(SupuestosValoracionFGEM sFGEM, BigDecimal patrimonialFGEM, BigDecimal utilidadAnualFGEM,
                                             BigDecimal depreciacionMensualFGEM, int mesesResultadosFGEM) {
        BigDecimal utilidadFGEM = utilidadAnualFGEM == null ? BigDecimal.ZERO : utilidadAnualFGEM;
        BigDecimal depreciacionFGEM = depreciacionMensualFGEM.multiply(BigDecimal.valueOf(12));
        BigDecimal flujoFGEM = utilidadFGEM.add(depreciacionFGEM);

        BigDecimal capitalizacionFGEM = utilidadFGEM.signum() > 0 ? dineroFGEM(utilidadFGEM.divide(sFGEM.tasaDescuentoFGEM(), PRECISION_FGEM)) : null;

        List<ProyeccionFGEM> proyeccionFGEM = new ArrayList<>();
        BigDecimal valorTerminalFGEM = null;
        BigDecimal terminalPresenteFGEM = null;
        BigDecimal descontadosFGEM = null;
        if (flujoFGEM.signum() > 0 && utilidadAnualFGEM != null) {
            BigDecimal sumaFGEM = BigDecimal.ZERO;
            BigDecimal ultimoFlujoFGEM = flujoFGEM;
            for (int tFGEM = 1; tFGEM <= sFGEM.aniosFGEM(); tFGEM++) {
                BigDecimal flujoTFGEM = flujoFGEM.multiply(BigDecimal.ONE.add(sFGEM.crecimientoFGEM()).pow(tFGEM), PRECISION_FGEM);
                BigDecimal factorFGEM = BigDecimal.ONE.divide(BigDecimal.ONE.add(sFGEM.tasaDescuentoFGEM()).pow(tFGEM), PRECISION_FGEM);
                BigDecimal presenteFGEM = flujoTFGEM.multiply(factorFGEM, PRECISION_FGEM);
                proyeccionFGEM.add(new ProyeccionFGEM(tFGEM, dineroFGEM(flujoTFGEM), factorFGEM.setScale(4, RoundingMode.HALF_UP), dineroFGEM(presenteFGEM)));
                sumaFGEM = sumaFGEM.add(presenteFGEM);
                ultimoFlujoFGEM = flujoTFGEM;
            }
            BigDecimal vtFGEM = ultimoFlujoFGEM.multiply(BigDecimal.ONE.add(sFGEM.crecimientoLargoPlazoFGEM()), PRECISION_FGEM)
                    .divide(sFGEM.tasaDescuentoFGEM().subtract(sFGEM.crecimientoLargoPlazoFGEM()), PRECISION_FGEM);
            BigDecimal vtPresenteFGEM = vtFGEM.divide(BigDecimal.ONE.add(sFGEM.tasaDescuentoFGEM()).pow(sFGEM.aniosFGEM()), PRECISION_FGEM);
            valorTerminalFGEM = dineroFGEM(vtFGEM);
            terminalPresenteFGEM = dineroFGEM(vtPresenteFGEM);
            descontadosFGEM = dineroFGEM(sumaFGEM.add(vtPresenteFGEM));
        }

        List<BigDecimal> rentabilidadFGEM = new ArrayList<>();
        if (capitalizacionFGEM != null) {
            rentabilidadFGEM.add(capitalizacionFGEM);
        }
        if (descontadosFGEM != null) {
            rentabilidadFGEM.add(descontadosFGEM);
        }
        BigDecimal valorRentabilidadFGEM = rentabilidadFGEM.isEmpty() ? null
                : dineroFGEM(rentabilidadFGEM.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rentabilidadFGEM.size()), PRECISION_FGEM));
        BigDecimal fondoFGEM = valorRentabilidadFGEM == null ? null : valorRentabilidadFGEM.subtract(patrimonialFGEM);
        boolean porRentabilidadFGEM = valorRentabilidadFGEM != null && valorRentabilidadFGEM.compareTo(patrimonialFGEM) > 0;
        BigDecimal referenciaFGEM = porRentabilidadFGEM ? valorRentabilidadFGEM : patrimonialFGEM;

        List<BigDecimal> todosFGEM = new ArrayList<>(rentabilidadFGEM);
        todosFGEM.add(patrimonialFGEM);
        BigDecimal minimoFGEM = todosFGEM.stream().min(BigDecimal::compareTo).orElse(patrimonialFGEM);
        BigDecimal maximoFGEM = todosFGEM.stream().max(BigDecimal::compareTo).orElse(patrimonialFGEM);
        return new ValoracionNegocioFGEM(sFGEM, dineroFGEM(patrimonialFGEM), dineroFGEM(utilidadFGEM), dineroFGEM(depreciacionFGEM), dineroFGEM(flujoFGEM),
                capitalizacionFGEM, descontadosFGEM, proyeccionFGEM, valorTerminalFGEM, terminalPresenteFGEM, valorRentabilidadFGEM, fondoFGEM,
                dineroFGEM(referenciaFGEM), porRentabilidadFGEM ? BaseFGEM.RENTABILIDAD : BaseFGEM.PATRIMONIO, dineroFGEM(minimoFGEM), dineroFGEM(maximoFGEM),
                mesesResultadosFGEM);
    }

    /**
     * Valor por flujos descontados con la tasa y el crecimiento movidos 5 y 2 puntos hacia cada lado.
     * Las combinaciones no válidas (tasa fuera de rango) quedan en {@code null}.
     */
    public SensibilidadFGEM sensibilidadFGEM() {
        List<BigDecimal> tasasFGEM = List.of(supuestosFGEM.tasaDescuentoFGEM().subtract(new BigDecimal("0.05")),
                supuestosFGEM.tasaDescuentoFGEM(), supuestosFGEM.tasaDescuentoFGEM().add(new BigDecimal("0.05")));
        List<BigDecimal> crecimientosFGEM = List.of(supuestosFGEM.crecimientoFGEM().subtract(new BigDecimal("0.02")),
                supuestosFGEM.crecimientoFGEM(), supuestosFGEM.crecimientoFGEM().add(new BigDecimal("0.02")));
        BigDecimal[][] valoresFGEM = new BigDecimal[tasasFGEM.size()][crecimientosFGEM.size()];
        for (int iFGEM = 0; iFGEM < tasasFGEM.size(); iFGEM++) {
            for (int jFGEM = 0; jFGEM < crecimientosFGEM.size(); jFGEM++) {
                try {
                    SupuestosValoracionFGEM sFGEM = new SupuestosValoracionFGEM(tasasFGEM.get(iFGEM), crecimientosFGEM.get(jFGEM),
                            supuestosFGEM.crecimientoLargoPlazoFGEM().min(tasasFGEM.get(iFGEM).subtract(new BigDecimal("0.01"))),
                            supuestosFGEM.aniosFGEM());
                    valoresFGEM[iFGEM][jFGEM] = calcularFGEM(sFGEM, patrimonialFGEM, utilidadAnualFGEM, depreciacionAnualFGEM
                            .divide(BigDecimal.valueOf(12), PRECISION_FGEM), mesesResultadosFGEM).flujosDescontadosFGEM();
                } catch (RuntimeException eFGEM) {
                    valoresFGEM[iFGEM][jFGEM] = null;
                }
            }
        }
        return new SensibilidadFGEM(tasasFGEM, crecimientosFGEM, valoresFGEM);
    }

    /** La utilidad se calculó con pocos meses: la valoración por rentabilidad es solo orientativa. */
    public boolean pocosDatosFGEM() {
        return mesesResultadosFGEM < MESES_CONFIABLES_FGEM;
    }

    /** Frases en lenguaje sencillo. */
    public List<String> lecturaFGEM(MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        frasesFGEM.add("Con lo que tiene y lo que debe, el negocio vale " + MontosFGEM.formatearFGEM(patrimonialFGEM, monedaFGEM)
                + " (valor patrimonial).");
        if (valorRentabilidadFGEM == null) {
            frasesFGEM.add("Como en los últimos 12 meses no tuvo utilidad, por ahora solo puede valorarse por lo que tiene: "
                    + "un comprador pagaría como mucho su valor patrimonial.");
        } else {
            frasesFGEM.add("Por lo que gana (" + MontosFGEM.formatearFGEM(utilidadAnualFGEM, monedaFGEM) + " al año), y exigiendo una "
                    + "rentabilidad de " + MontosFGEM.porcentajeFGEM(supuestosFGEM.tasaDescuentoFGEM()) + " anual, vale alrededor de "
                    + MontosFGEM.formatearFGEM(valorRentabilidadFGEM, monedaFGEM) + ".");
            if (baseFGEM == BaseFGEM.RENTABILIDAD) {
                frasesFGEM.add("El negocio vale más por lo que gana que por lo que tiene: el fondo de comercio (clientela, "
                        + "ubicación, prestigio) se estima en " + MontosFGEM.formatearFGEM(fondoComercioFGEM, monedaFGEM) + ".");
            } else {
                frasesFGEM.add("Lo que gana no alcanza a justificar el valor de sus bienes: rinde menos de lo que exigiría "
                        + "un inversionista. Mejorar la utilidad es la forma de que el negocio valga más.");
            }
        }
        frasesFGEM.add("Valor de referencia: " + MontosFGEM.formatearFGEM(valorReferenciaFGEM, monedaFGEM) + " (rango de "
                + MontosFGEM.formatearFGEM(minimoFGEM, monedaFGEM) + " a " + MontosFGEM.formatearFGEM(maximoFGEM, monedaFGEM) + ").");
        if (pocosDatosFGEM() && valorRentabilidadFGEM != null) {
            frasesFGEM.add("Atención: la utilidad se calculó con solo " + mesesResultadosFGEM + " meses de datos; "
                    + "la valoración por rentabilidad es solo orientativa.");
        }
        return frasesFGEM;
    }

    private static BigDecimal dineroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(2, RoundingMode.HALF_UP);
    }
}
