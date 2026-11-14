package com.businessanalyzer.analytics.prediccion;

/** Método que proyecta una serie mensual hacia el futuro. */
public interface MetodoPronostico {

    /** Datos mínimos que necesita el método. */
    int MINIMO_DATOS = 3;

    String nombre();

    /** Explicación breve para el usuario. */
    String explicacion();

    /**
     * @param historia  valores observados, del más antiguo al más reciente (al menos {@link #MINIMO_DATOS})
     * @param horizonte cantidad de períodos a proyectar
     * @return el pronóstico de cada período futuro (índice 0 = el siguiente)
     */
    double[] pronosticar(double[] historia, int horizonte);
}
