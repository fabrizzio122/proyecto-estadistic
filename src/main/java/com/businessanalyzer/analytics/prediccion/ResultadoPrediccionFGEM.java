package com.businessanalyzer.analytics.prediccion;

import java.time.YearMonth;
import java.util.List;

public record ResultadoPrediccionFGEM(List<YearMonth> mesesFGEM, double[] ventasFGEM, double[] utilidadFGEM, int horizonteFGEM,
                                  PronosticoFGEM pronosticoVentasFGEM, PronosticoFGEM pronosticoUtilidadFGEM,
                                  boolean mesActualExcluidoFGEM) {

    public ResultadoPrediccionFGEM {
        mesesFGEM = List.copyOf(mesesFGEM);
    }

    public YearMonth primerMesProyectadoFGEM(YearMonth siNoHayHistoriaFGEM) {
        return mesesFGEM.isEmpty() ? siNoHayHistoriaFGEM : mesesFGEM.get(mesesFGEM.size() - 1).plusMonths(1);
    }

    public double ventasUltimosMesesFGEM() {
        double sumaFGEM = 0;
        for (int iFGEM = Math.max(0, ventasFGEM.length - horizonteFGEM); iFGEM < ventasFGEM.length; iFGEM++) {
            sumaFGEM += ventasFGEM[iFGEM];
        }
        return sumaFGEM;
    }

    public int mesesComparadosFGEM() {
        return Math.min(horizonteFGEM, ventasFGEM.length);
    }
}
