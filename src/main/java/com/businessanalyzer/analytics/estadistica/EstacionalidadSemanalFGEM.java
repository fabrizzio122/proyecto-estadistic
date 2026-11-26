package com.businessanalyzer.analytics.estadistica;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

public record EstacionalidadSemanalFGEM(Map<DayOfWeek, Double> promedioPorDiaFGEM, Map<DayOfWeek, Integer> diasObservadosFGEM,
                                    double promedioGeneralFGEM) {

    public static EstacionalidadSemanalFGEM deFGEM(Map<LocalDate, Double> ventasPorDiaFGEM) {
        Map<DayOfWeek, Double> sumasFGEM = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> diasFGEM = new EnumMap<>(DayOfWeek.class);
        double totalFGEM = 0;
        for (Map.Entry<LocalDate, Double> eFGEM : ventasPorDiaFGEM.entrySet()) {
            DayOfWeek diaFGEM = eFGEM.getKey().getDayOfWeek();
            sumasFGEM.merge(diaFGEM, eFGEM.getValue(), Double::sum);
            diasFGEM.merge(diaFGEM, 1, Integer::sum);
            totalFGEM += eFGEM.getValue();
        }
        Map<DayOfWeek, Double> promediosFGEM = new EnumMap<>(DayOfWeek.class);
        sumasFGEM.forEach((diaFGEM, sumaFGEM) -> promediosFGEM.put(diaFGEM, sumaFGEM / diasFGEM.get(diaFGEM)));
        double generalFGEM = ventasPorDiaFGEM.isEmpty() ? Double.NaN : totalFGEM / ventasPorDiaFGEM.size();
        return new EstacionalidadSemanalFGEM(promediosFGEM, diasFGEM, generalFGEM);
    }

    public double indiceFGEM(DayOfWeek diaFGEM) {
        Double promedioFGEM = promedioPorDiaFGEM.get(diaFGEM);
        return promedioFGEM == null || Double.isNaN(promedioGeneralFGEM) || promedioGeneralFGEM == 0
                ? Double.NaN : promedioFGEM / promedioGeneralFGEM;
    }

    public DayOfWeek mejorDiaFGEM() {
        return promedioPorDiaFGEM.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public DayOfWeek peorDiaFGEM() {
        return promedioPorDiaFGEM.entrySet().stream()
                .min(Map.Entry.<DayOfWeek, Double>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).orElse(null);
    }
}
