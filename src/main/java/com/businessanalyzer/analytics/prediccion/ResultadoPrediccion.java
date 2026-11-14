package com.businessanalyzer.analytics.prediccion;

import java.time.YearMonth;
import java.util.List;

/**
 * Proyección de ventas y utilidad neta de los próximos meses.
 *
 * @param meses             meses completos de historia usados
 * @param ventas            ventas netas de cada mes de historia
 * @param utilidad          utilidad neta de cada mes de historia
 * @param mesActualExcluido el mes en curso no se usó como historia (está incompleto): es el primer mes proyectado
 */
public record ResultadoPrediccion(List<YearMonth> meses, double[] ventas, double[] utilidad, int horizonte,
                                  Pronostico pronosticoVentas, Pronostico pronosticoUtilidad,
                                  boolean mesActualExcluido) {

    public ResultadoPrediccion {
        meses = List.copyOf(meses);
    }

    /** Primer mes proyectado. */
    public YearMonth primerMesProyectado(YearMonth siNoHayHistoria) {
        return meses.isEmpty() ? siNoHayHistoria : meses.get(meses.size() - 1).plusMonths(1);
    }

    /** Suma de las ventas de los últimos {@code horizonte} meses (o de los que haya). */
    public double ventasUltimosMeses() {
        double suma = 0;
        for (int i = Math.max(0, ventas.length - horizonte); i < ventas.length; i++) {
            suma += ventas[i];
        }
        return suma;
    }

    /** Cantidad de meses usados en {@link #ventasUltimosMeses()}. */
    public int mesesComparados() {
        return Math.min(horizonte, ventas.length);
    }
}
