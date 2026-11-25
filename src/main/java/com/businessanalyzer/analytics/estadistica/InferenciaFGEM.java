package com.businessanalyzer.analytics.estadistica;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.apache.commons.math3.stat.inference.TTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Inferencia estadística sobre las ventas diarias (Apache Commons Math), con nivel de significancia de 5 %:
 * <ul>
 *     <li><b>Intervalo de confianza</b> de la venta diaria promedio, con la distribución t de Student
 *         (n − 1 grados de libertad): media ± t · s / √n.</li>
 *     <li><b>ANOVA de un factor</b> por día de la semana. H₀: todos los días venden lo mismo en promedio;
 *         H₁: al menos un día es distinto. Si p &lt; 0.05 se rechaza H₀. El eta cuadrado (η²) indica qué parte de la
 *         variación de las ventas se explica por el día de la semana.</li>
 *     <li><b>Prueba t de Welch</b> entre el período elegido y el anterior. H₀: la venta diaria promedio es la misma en
 *         los dos períodos. No supone varianzas iguales.</li>
 * </ul>
 * Supuesto común: los días se tratan como observaciones independientes. Con muchos días las pruebas son robustas a que
 * las ventas no sigan exactamente una distribución normal. Es cálculo puro.
 */
public final class InferenciaFGEM {

    public static final double NIVEL_SIGNIFICANCIA_FGEM = 0.05;
    public static final double CONFIANZA_FGEM = 0.95;
    /** Días mínimos de cada muestra para aplicar una prueba. */
    public static final int MINIMO_DIAS_FGEM = 2;

    private InferenciaFGEM() {
    }

    /**
     * Intervalo de confianza de la media. Los valores son {@code NaN} si hay menos de {@value #MINIMO_DIAS_FGEM} datos.
     *
     * @param margenFGEM t · s / √n (la mitad del ancho del intervalo)
     */
    public record IntervaloConfianzaFGEM(int nFGEM, double mediaFGEM, double inferiorFGEM, double superiorFGEM, double margenFGEM,
                                         double nivelFGEM) {

        public boolean calculadoFGEM() {
            return !Double.isNaN(margenFGEM);
        }
    }

    /**
     * ANOVA de un factor. {@code NaN} si no se pudo calcular (menos de dos días de la semana con al menos dos datos,
     * o todas las ventas iguales).
     *
     * @param etaCuadradoFGEM suma de cuadrados entre grupos ÷ suma de cuadrados total (0 a 1)
     */
    public record AnovaFGEM(int gruposFGEM, int nFGEM, double fFGEM, double pFGEM, int glEntreFGEM, int glDentroFGEM, double etaCuadradoFGEM) {

        public boolean calculadoFGEM() {
            return !Double.isNaN(pFGEM);
        }

        /** Se rechaza H₀: la diferencia entre días es estadísticamente significativa. */
        public boolean significativoFGEM() {
            return calculadoFGEM() && pFGEM < NIVEL_SIGNIFICANCIA_FGEM;
        }
    }

    /**
     * Prueba t de Welch entre dos muestras. {@code NaN} si alguna tiene menos de {@value #MINIMO_DIAS_FGEM} datos o
     * las dos no tienen variación.
     *
     * @param variacionFGEM (media actual − media anterior) ÷ media anterior; {@code NaN} si la anterior es cero
     */
    public record ComparacionFGEM(int nActualFGEM, double mediaActualFGEM, int nAnteriorFGEM, double mediaAnteriorFGEM, double variacionFGEM,
                                  double tFGEM, double pFGEM) {

        public boolean calculadoFGEM() {
            return !Double.isNaN(pFGEM);
        }

        public boolean significativoFGEM() {
            return calculadoFGEM() && pFGEM < NIVEL_SIGNIFICANCIA_FGEM;
        }
    }

    /** Intervalo de confianza al {@value #CONFIANZA_FGEM} de la media de {@code valores}. */
    public static IntervaloConfianzaFGEM intervaloFGEM(double[] valoresFGEM) {
        SummaryStatistics sFGEM = resumenFGEM(valoresFGEM);
        int nFGEM = valoresFGEM.length;
        double mediaFGEM = nFGEM == 0 ? Double.NaN : sFGEM.getMean();
        if (nFGEM < MINIMO_DIAS_FGEM) {
            return new IntervaloConfianzaFGEM(nFGEM, mediaFGEM, Double.NaN, Double.NaN, Double.NaN, CONFIANZA_FGEM);
        }
        double tCriticoFGEM = new TDistribution(nFGEM - 1.0).inverseCumulativeProbability(1 - (1 - CONFIANZA_FGEM) / 2);
        double margenFGEM = tCriticoFGEM * sFGEM.getStandardDeviation() / Math.sqrt(nFGEM);
        return new IntervaloConfianzaFGEM(nFGEM, mediaFGEM, mediaFGEM - margenFGEM, mediaFGEM + margenFGEM, margenFGEM, CONFIANZA_FGEM);
    }

    /** ANOVA de un factor de las ventas diarias agrupadas por día de la semana. */
    public static AnovaFGEM anovaDiaSemanaFGEM(Map<LocalDate, Double> ventasPorDiaFGEM) {
        Map<DayOfWeek, List<Double>> gruposFGEM = new EnumMap<>(DayOfWeek.class);
        ventasPorDiaFGEM.forEach((diaFGEM, ventaFGEM) -> gruposFGEM.computeIfAbsent(diaFGEM.getDayOfWeek(), kFGEM -> new ArrayList<>()).add(ventaFGEM));
        List<double[]> muestrasFGEM = gruposFGEM.values().stream()
                .filter(listaFGEM -> listaFGEM.size() >= MINIMO_DIAS_FGEM)
                .map(listaFGEM -> listaFGEM.stream().mapToDouble(Double::doubleValue).toArray())
                .toList();
        int nFGEM = muestrasFGEM.stream().mapToInt(mFGEM -> mFGEM.length).sum();
        int glEntreFGEM = muestrasFGEM.size() - 1;
        int glDentroFGEM = nFGEM - muestrasFGEM.size();
        if (muestrasFGEM.size() < 2 || glDentroFGEM < 1) {
            return new AnovaFGEM(muestrasFGEM.size(), nFGEM, Double.NaN, Double.NaN, glEntreFGEM, glDentroFGEM, Double.NaN);
        }
        double fFGEM;
        double pFGEM;
        try {
            OneWayAnova anovaFGEM = new OneWayAnova();
            fFGEM = anovaFGEM.anovaFValue(muestrasFGEM);
            pFGEM = anovaFGEM.anovaPValue(muestrasFGEM);
        } catch (MathIllegalArgumentException eFGEM) {
            fFGEM = Double.NaN;
            pFGEM = Double.NaN;
        }
        return new AnovaFGEM(muestrasFGEM.size(), nFGEM, fFGEM, pFGEM, glEntreFGEM, glDentroFGEM, etaCuadradoFGEM(muestrasFGEM));
    }

    /** Prueba t de Welch (dos colas) entre las ventas diarias del período actual y las del anterior. */
    public static ComparacionFGEM compararFGEM(double[] actualFGEM, double[] anteriorFGEM) {
        double mediaActualFGEM = actualFGEM.length == 0 ? Double.NaN : resumenFGEM(actualFGEM).getMean();
        double mediaAnteriorFGEM = anteriorFGEM.length == 0 ? Double.NaN : resumenFGEM(anteriorFGEM).getMean();
        double variacionFGEM = Double.isNaN(mediaAnteriorFGEM) || mediaAnteriorFGEM == 0 ? Double.NaN
                : (mediaActualFGEM - mediaAnteriorFGEM) / mediaAnteriorFGEM;
        double tFGEM = Double.NaN;
        double pFGEM = Double.NaN;
        if (actualFGEM.length >= MINIMO_DIAS_FGEM && anteriorFGEM.length >= MINIMO_DIAS_FGEM
                && resumenFGEM(actualFGEM).getVariance() + resumenFGEM(anteriorFGEM).getVariance() > 0) {
            TTest pruebaFGEM = new TTest();
            tFGEM = pruebaFGEM.t(actualFGEM, anteriorFGEM);
            pFGEM = pruebaFGEM.tTest(actualFGEM, anteriorFGEM);
        }
        return new ComparacionFGEM(actualFGEM.length, mediaActualFGEM, anteriorFGEM.length, mediaAnteriorFGEM, variacionFGEM, tFGEM, pFGEM);
    }

    private static double etaCuadradoFGEM(List<double[]> muestrasFGEM) {
        SummaryStatistics todoFGEM = new SummaryStatistics();
        muestrasFGEM.forEach(mFGEM -> {
            for (double vFGEM : mFGEM) {
                todoFGEM.addValue(vFGEM);
            }
        });
        double mediaGeneralFGEM = todoFGEM.getMean();
        double totalFGEM = todoFGEM.getVariance() * (todoFGEM.getN() - 1);
        double entreFGEM = 0;
        for (double[] mFGEM : muestrasFGEM) {
            double mediaFGEM = resumenFGEM(mFGEM).getMean();
            entreFGEM += mFGEM.length * Math.pow(mediaFGEM - mediaGeneralFGEM, 2);
        }
        return totalFGEM == 0 ? Double.NaN : entreFGEM / totalFGEM;
    }

    private static SummaryStatistics resumenFGEM(double[] valoresFGEM) {
        SummaryStatistics sFGEM = new SummaryStatistics();
        for (double vFGEM : valoresFGEM) {
            sFGEM.addValue(vFGEM);
        }
        return sFGEM;
    }
}
