package com.businessanalyzer.analytics.prediccion;

import com.businessanalyzer.analytics.prediccion.Pronostico.Confiabilidad;
import com.businessanalyzer.analytics.prediccion.Pronostico.Evaluacion;
import com.businessanalyzer.analytics.prediccion.Pronostico.Punto;

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
public final class Pronosticador {

    /** Con 4 meses hay al menos un mes para validar. */
    public static final int MINIMO_DATOS = MetodoPronostico.MINIMO_DATOS + 1;
    private static final double Z_95 = 1.96;

    public static final List<MetodoPronostico> METODOS = List.of(
            new TendenciaLineal(), new PromedioMovil(3), new SuavizadoHolt());

    private Pronosticador() {
    }

    public static Pronostico pronosticar(String serie, double[] historia, int horizonte, boolean noNegativo) {
        return pronosticar(serie, historia, horizonte, noNegativo, METODOS);
    }

    public static Pronostico pronosticar(String serie, double[] historia, int horizonte, boolean noNegativo,
                                         List<MetodoPronostico> metodos) {
        if (historia.length < MINIMO_DATOS) {
            return Pronostico.insuficiente(serie, historia.length);
        }
        List<Evaluacion> evaluaciones = new ArrayList<>();
        for (MetodoPronostico metodo : metodos) {
            evaluaciones.add(evaluar(metodo, historia));
        }
        Evaluacion mejor = evaluaciones.get(0);
        for (Evaluacion e : evaluaciones) {
            if (e.mae() < mejor.mae() - 1e-9) {
                mejor = e;
            }
        }

        double[] valores = mejor.metodo().pronosticar(historia, horizonte);
        List<Punto> puntos = new ArrayList<>();
        for (int h = 1; h <= horizonte; h++) {
            double valor = valores[h - 1];
            double margen = Z_95 * mejor.rmse() * Math.sqrt(h);
            double inferior = valor - margen;
            double superior = valor + margen;
            if (noNegativo) {
                valor = Math.max(0, valor);
                inferior = Math.max(0, inferior);
                superior = Math.max(0, superior);
            }
            puntos.add(new Punto(h, valor, inferior, superior));
        }

        double promedioAbsoluto = Arrays.stream(historia).map(Math::abs).average().orElse(0);
        double errorRelativo = promedioAbsoluto == 0 ? Double.NaN : mejor.mae() / promedioAbsoluto;
        return new Pronostico(serie, historia.length, mejor, evaluaciones, puntos,
                confiabilidad(errorRelativo, mejor.mesesEvaluados()), errorRelativo);
    }

    /** Pronóstico a un mes de cada mes pasado, usando solo los datos anteriores. */
    static Evaluacion evaluar(MetodoPronostico metodo, double[] y) {
        double sumaAbsoluta = 0;
        double sumaCuadrados = 0;
        int puntos = 0;
        for (int t = MetodoPronostico.MINIMO_DATOS; t < y.length; t++) {
            double pronostico = metodo.pronosticar(Arrays.copyOf(y, t), 1)[0];
            double error = y[t] - pronostico;
            sumaAbsoluta += Math.abs(error);
            sumaCuadrados += error * error;
            puntos++;
        }
        return new Evaluacion(metodo, sumaAbsoluta / puntos, Math.sqrt(sumaCuadrados / puntos), puntos);
    }

    /** Alta si el error es menor al 10 % del valor típico; media hasta 25 %; con menos de 3 meses validados, baja. */
    static Confiabilidad confiabilidad(double errorRelativo, int mesesEvaluados) {
        if (mesesEvaluados < 3 || Double.isNaN(errorRelativo)) {
            return Confiabilidad.BAJA;
        }
        if (errorRelativo < 0.10) {
            return Confiabilidad.ALTA;
        }
        return errorRelativo < 0.25 ? Confiabilidad.MEDIA : Confiabilidad.BAJA;
    }
}
