package com.businessanalyzer.analytics.prediccion;

public final class SuavizadoHoltFGEM implements MetodoPronosticoFGEM {

    public record ParametrosFGEM(double alfaFGEM, double betaFGEM) {
    }

    @Override
    public String nombreFGEM() {
        return "Suavizado exponencial de Holt";
    }

    @Override
    public String explicacionFGEM() {
        return "Sigue el nivel y la tendencia de las ventas dando más importancia a los meses recientes. "
                + "Se adapta mejor cuando el ritmo de crecimiento cambia.";
    }

    @Override
    public double[] pronosticarFGEM(double[] historiaFGEM, int horizonteFGEM) {
        ParametrosFGEM pFGEM = ajustarFGEM(historiaFGEM);
        double[] estadoFGEM = suavizarFGEM(historiaFGEM, pFGEM.alfaFGEM(), pFGEM.betaFGEM(), null);
        double[] pronosticoFGEM = new double[horizonteFGEM];
        for (int hFGEM = 1; hFGEM <= horizonteFGEM; hFGEM++) {
            pronosticoFGEM[hFGEM - 1] = estadoFGEM[0] + hFGEM * estadoFGEM[1];
        }
        return pronosticoFGEM;
    }

    public ParametrosFGEM ajustarFGEM(double[] historiaFGEM) {
        ParametrosFGEM mejorFGEM = new ParametrosFGEM(0.5, 0.5);
        double menorErrorFGEM = Double.POSITIVE_INFINITY;
        for (int iFGEM = 1; iFGEM <= 9; iFGEM++) {
            for (int jFGEM = 1; jFGEM <= 9; jFGEM++) {
                double alfaFGEM = iFGEM / 10.0;
                double betaFGEM = jFGEM / 10.0;
                double[] errorFGEM = new double[1];
                suavizarFGEM(historiaFGEM, alfaFGEM, betaFGEM, errorFGEM);
                if (errorFGEM[0] < menorErrorFGEM - 1e-9) {
                    menorErrorFGEM = errorFGEM[0];
                    mejorFGEM = new ParametrosFGEM(alfaFGEM, betaFGEM);
                }
            }
        }
        return mejorFGEM;
    }

    private static double[] suavizarFGEM(double[] yFGEM, double alfaFGEM, double betaFGEM, double[] errorFGEM) {
        double nivelFGEM = yFGEM[0];
        double tendenciaFGEM = yFGEM.length > 1 ? yFGEM[1] - yFGEM[0] : 0;
        for (int tFGEM = 1; tFGEM < yFGEM.length; tFGEM++) {
            double pronosticoFGEM = nivelFGEM + tendenciaFGEM;
            if (errorFGEM != null && tFGEM >= 2) {
                errorFGEM[0] += (yFGEM[tFGEM] - pronosticoFGEM) * (yFGEM[tFGEM] - pronosticoFGEM);
            }
            double nivelAnteriorFGEM = nivelFGEM;
            nivelFGEM = alfaFGEM * yFGEM[tFGEM] + (1 - alfaFGEM) * (nivelFGEM + tendenciaFGEM);
            tendenciaFGEM = betaFGEM * (nivelFGEM - nivelAnteriorFGEM) + (1 - betaFGEM) * tendenciaFGEM;
        }
        return new double[]{nivelFGEM, tendenciaFGEM};
    }
}
