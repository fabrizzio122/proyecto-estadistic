package com.businessanalyzer.analytics.prediccion;

import java.time.YearMonth;
import java.util.List;

/**
 * Proyección de ventas y utilidad neta de los próximos meses.
 *
 * @param mesesFGEM             meses completos de historia usados
 * @param ventasFGEM            ventas netas de cada mes de historia
 * @param utilidadFGEM          utilidad neta de cada mes de historia
 * @param mesActualExcluidoFGEM el mes en curso no se usó como historia (está incompleto): es el primer mes proyectado
 */
public record ResultadoPrediccionFGEM(List<YearMonth> mesesFGEM, double[] ventasFGEM, double[] utilidadFGEM, int horizonteFGEM,
                                  PronosticoFGEM pronosticoVentasFGEM, PronosticoFGEM pronosticoUtilidadFGEM,
                                  boolean mesActualExcluidoFGEM) {

    public ResultadoPrediccionFGEM {
        mesesFGEM = List.copyOf(mesesFGEM);
    }

    /** Primer mes proyectado. */
    public YearMonth primerMesProyectadoFGEM(YearMonth siNoHayHistoriaFGEM) {
        return mesesFGEM.isEmpty() ? siNoHayHistoriaFGEM : mesesFGEM.get(mesesFGEM.size() - 1).plusMonths(1);
    }

    /** Suma de las ventas de los últimos {@code horizonte} meses (o de los que haya). */
    public double ventasUltimosMesesFGEM() {
        double sumaFGEM = 0;
        for (int iFGEM = Math.max(0, ventasFGEM.length - horizonteFGEM); iFGEM < ventasFGEM.length; iFGEM++) {
            sumaFGEM += ventasFGEM[iFGEM];
        }
        return sumaFGEM;
    }

    /** Cantidad de meses usados en {@link #ventasUltimosMesesFGEM()}. */
    public int mesesComparadosFGEM() {
        return Math.min(horizonteFGEM, ventasFGEM.length);
    }
}
