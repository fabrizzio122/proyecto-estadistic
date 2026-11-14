package com.businessanalyzer.analytics.prediccion;

/**
 * Suavizado exponencial doble de Holt: estima un <b>nivel</b> y una <b>tendencia</b> que se
 * actualizan cada mes dando más peso a los datos recientes.
 * <pre>
 * nivel_t     = α · y_t + (1 − α) · (nivel_{t−1} + tendencia_{t−1})
 * tendencia_t = β · (nivel_t − nivel_{t−1}) + (1 − β) · tendencia_{t−1}
 * pronóstico_{t+h} = nivel_t + h · tendencia_t
 * </pre>
 * α y β se eligen automáticamente (entre 0.1 y 0.9) minimizando el error de pronóstico a un mes
 * sobre la propia historia.
 */
public final class SuavizadoHolt implements MetodoPronostico {

    /** Parámetros de suavizado elegidos. */
    public record Parametros(double alfa, double beta) {
    }

    @Override
    public String nombre() {
        return "Suavizado exponencial de Holt";
    }

    @Override
    public String explicacion() {
        return "Sigue el nivel y la tendencia de las ventas dando más importancia a los meses recientes. "
                + "Se adapta mejor cuando el ritmo de crecimiento cambia.";
    }

    @Override
    public double[] pronosticar(double[] historia, int horizonte) {
        Parametros p = ajustar(historia);
        double[] estado = suavizar(historia, p.alfa(), p.beta(), null);
        double[] pronostico = new double[horizonte];
        for (int h = 1; h <= horizonte; h++) {
            pronostico[h - 1] = estado[0] + h * estado[1];
        }
        return pronostico;
    }

    /** Busca en una grilla los α y β con menor suma de errores al cuadrado. */
    public Parametros ajustar(double[] historia) {
        Parametros mejor = new Parametros(0.5, 0.5);
        double menorError = Double.POSITIVE_INFINITY;
        for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= 9; j++) {
                double alfa = i / 10.0;
                double beta = j / 10.0;
                double[] error = new double[1];
                suavizar(historia, alfa, beta, error);
                if (error[0] < menorError - 1e-9) {
                    menorError = error[0];
                    mejor = new Parametros(alfa, beta);
                }
            }
        }
        return mejor;
    }

    /**
     * Aplica el suavizado y devuelve {nivel, tendencia} finales. Si {@code error} no es nulo,
     * acumula en error[0] la suma de errores al cuadrado de los pronósticos a un mes.
     */
    private static double[] suavizar(double[] y, double alfa, double beta, double[] error) {
        double nivel = y[0];
        double tendencia = y.length > 1 ? y[1] - y[0] : 0;
        for (int t = 1; t < y.length; t++) {
            double pronostico = nivel + tendencia;
            if (error != null && t >= 2) {
                error[0] += (y[t] - pronostico) * (y[t] - pronostico);
            }
            double nivelAnterior = nivel;
            nivel = alfa * y[t] + (1 - alfa) * (nivel + tendencia);
            tendencia = beta * (nivel - nivelAnterior) + (1 - beta) * tendencia;
        }
        return new double[]{nivel, tendencia};
    }
}
