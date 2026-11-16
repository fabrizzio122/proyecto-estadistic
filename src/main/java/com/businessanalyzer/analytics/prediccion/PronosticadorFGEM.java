package com.businessanalyzer.analytics.prediccion;

import com.businessanalyzer.analytics.prediccion.PronosticoFGEM.ConfiabilidadFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM.EvaluacionFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM.PuntoFGEM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Elige el mejor método para una serie y genera el pronóstico.
 * <ol>
 *     <li><b>Validación histórica</b>: cada método pronostica cada mes pasado usando solo los meses
 *         anteriores a él (desde el 4.º mes) y se mide su error.</li>
 *     <li>Se elige el de menor error absoluto medio (MAE). En empate, el más simple.</li>
 *     <li>El intervalo al 95 % se aproxima como pronóstico ± 1.96 · RMSE · √h: crece con la
 *         distancia al presente y con el error real que tuvo el método.</li>
 * </ol>
 */
public final class PronosticadorFGEM {

    /** Con 4 meses hay al menos un mes para validar. */
    public static final int MINIMO_DATOS_FGEM = MetodoPronosticoFGEM.MINIMO_DATOS_FGEM + 1;
    private static final double Z_95_FGEM = 1.96;

    public static final List<MetodoPronosticoFGEM> METODOS_FGEM = List.of(
            new TendenciaLinealFGEM(), new PromedioMovilFGEM(3), new SuavizadoHoltFGEM());

    private PronosticadorFGEM() {
    }

    public static PronosticoFGEM pronosticarFGEM(String serieFGEM, double[] historiaFGEM, int horizonteFGEM, boolean noNegativoFGEM) {
        return pronosticarFGEM(serieFGEM, historiaFGEM, horizonteFGEM, noNegativoFGEM, METODOS_FGEM);
    }

    public static PronosticoFGEM pronosticarFGEM(String serieFGEM, double[] historiaFGEM, int horizonteFGEM, boolean noNegativoFGEM,
                                         List<MetodoPronosticoFGEM> metodosFGEM) {
        if (historiaFGEM.length < MINIMO_DATOS_FGEM) {
            return PronosticoFGEM.insuficienteFGEM(serieFGEM, historiaFGEM.length);
        }
        List<EvaluacionFGEM> evaluacionesFGEM = new ArrayList<>();
        for (MetodoPronosticoFGEM metodoFGEM : metodosFGEM) {
            evaluacionesFGEM.add(evaluarFGEM(metodoFGEM, historiaFGEM));
        }
        EvaluacionFGEM mejorFGEM = evaluacionesFGEM.get(0);
        for (EvaluacionFGEM eFGEM : evaluacionesFGEM) {
            if (eFGEM.maeFGEM() < mejorFGEM.maeFGEM() - 1e-9) {
                mejorFGEM = eFGEM;
            }
        }

        double[] valoresFGEM = mejorFGEM.metodoFGEM().pronosticarFGEM(historiaFGEM, horizonteFGEM);
        List<PuntoFGEM> puntosFGEM = new ArrayList<>();
        for (int hFGEM = 1; hFGEM <= horizonteFGEM; hFGEM++) {
            double valorFGEM = valoresFGEM[hFGEM - 1];
            double margenFGEM = Z_95_FGEM * mejorFGEM.rmseFGEM() * Math.sqrt(hFGEM);
            double inferiorFGEM = valorFGEM - margenFGEM;
            double superiorFGEM = valorFGEM + margenFGEM;
            if (noNegativoFGEM) {
                valorFGEM = Math.max(0, valorFGEM);
                inferiorFGEM = Math.max(0, inferiorFGEM);
                superiorFGEM = Math.max(0, superiorFGEM);
            }
            puntosFGEM.add(new PuntoFGEM(hFGEM, valorFGEM, inferiorFGEM, superiorFGEM));
        }

        double promedioAbsolutoFGEM = Arrays.stream(historiaFGEM).map(Math::abs).average().orElse(0);
        double errorRelativoFGEM = promedioAbsolutoFGEM == 0 ? Double.NaN : mejorFGEM.maeFGEM() / promedioAbsolutoFGEM;
        return new PronosticoFGEM(serieFGEM, historiaFGEM.length, mejorFGEM, evaluacionesFGEM, puntosFGEM,
                confiabilidadFGEM(errorRelativoFGEM, mejorFGEM.mesesEvaluadosFGEM()), errorRelativoFGEM);
    }

    /** Pronóstico a un mes de cada mes pasado, usando solo los datos anteriores. */
    static EvaluacionFGEM evaluarFGEM(MetodoPronosticoFGEM metodoFGEM, double[] yFGEM) {
        double sumaAbsolutaFGEM = 0;
        double sumaCuadradosFGEM = 0;
        int puntosFGEM = 0;
        for (int tFGEM = MetodoPronosticoFGEM.MINIMO_DATOS_FGEM; tFGEM < yFGEM.length; tFGEM++) {
            double pronosticoFGEM = metodoFGEM.pronosticarFGEM(Arrays.copyOf(yFGEM, tFGEM), 1)[0];
            double errorFGEM = yFGEM[tFGEM] - pronosticoFGEM;
            sumaAbsolutaFGEM += Math.abs(errorFGEM);
            sumaCuadradosFGEM += errorFGEM * errorFGEM;
            puntosFGEM++;
        }
        return new EvaluacionFGEM(metodoFGEM, sumaAbsolutaFGEM / puntosFGEM, Math.sqrt(sumaCuadradosFGEM / puntosFGEM), puntosFGEM);
    }

    /** Alta si el error es menor al 10 % del valor típico; media hasta 25 %; con menos de 3 meses validados, baja. */
    static ConfiabilidadFGEM confiabilidadFGEM(double errorRelativoFGEM, int mesesEvaluadosFGEM) {
        if (mesesEvaluadosFGEM < 3 || Double.isNaN(errorRelativoFGEM)) {
            return ConfiabilidadFGEM.BAJA;
        }
        if (errorRelativoFGEM < 0.10) {
            return ConfiabilidadFGEM.ALTA;
        }
        return errorRelativoFGEM < 0.25 ? ConfiabilidadFGEM.MEDIA : ConfiabilidadFGEM.BAJA;
    }
}
