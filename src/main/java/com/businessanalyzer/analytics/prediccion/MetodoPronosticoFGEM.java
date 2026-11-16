package com.businessanalyzer.analytics.prediccion;

/** Método que proyecta una serie mensual hacia el futuro. */
public interface MetodoPronosticoFGEM {

    /** Datos mínimos que necesita el método. */
    int MINIMO_DATOS_FGEM = 3;

    String nombreFGEM();

    /** Explicación breve para el usuario. */
    String explicacionFGEM();

    /**
     * @param historiaFGEM  valores observados, del más antiguo al más reciente (al menos {@link #MINIMO_DATOS_FGEM})
     * @param horizonteFGEM cantidad de períodos a proyectar
     * @return el pronóstico de cada período futuro (índice 0 = el siguiente)
     */
    double[] pronosticarFGEM(double[] historiaFGEM, int horizonteFGEM);
}
