package com.businessanalyzer.analytics.prediccion;

public interface MetodoPronosticoFGEM {

    int MINIMO_DATOS_FGEM = 3;

    String nombreFGEM();

    String explicacionFGEM();

    double[] pronosticarFGEM(double[] historiaFGEM, int horizonteFGEM);
}
