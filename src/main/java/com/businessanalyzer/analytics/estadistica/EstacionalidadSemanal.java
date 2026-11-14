package com.businessanalyzer.analytics.estadistica;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Venta promedio de cada día de la semana. Se cuentan también los días sin ventas, para que
 * un día en que el negocio no vende (o cierra) aparezca con un promedio bajo.
 * <p>
 * Índice estacional = promedio del día ÷ promedio general (1.20 = se vende 20 % más que un día típico).
 */
public record EstacionalidadSemanal(Map<DayOfWeek, Double> promedioPorDia, Map<DayOfWeek, Integer> diasObservados,
                                    double promedioGeneral) {

    public static EstacionalidadSemanal de(Map<LocalDate, Double> ventasPorDia) {
        Map<DayOfWeek, Double> sumas = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> dias = new EnumMap<>(DayOfWeek.class);
        double total = 0;
        for (Map.Entry<LocalDate, Double> e : ventasPorDia.entrySet()) {
            DayOfWeek dia = e.getKey().getDayOfWeek();
            sumas.merge(dia, e.getValue(), Double::sum);
            dias.merge(dia, 1, Integer::sum);
            total += e.getValue();
        }
        Map<DayOfWeek, Double> promedios = new EnumMap<>(DayOfWeek.class);
        sumas.forEach((dia, suma) -> promedios.put(dia, suma / dias.get(dia)));
        double general = ventasPorDia.isEmpty() ? Double.NaN : total / ventasPorDia.size();
        return new EstacionalidadSemanal(promedios, dias, general);
    }

    /** {@code NaN} si el día no aparece o no hubo ventas en el período. */
    public double indice(DayOfWeek dia) {
        Double promedio = promedioPorDia.get(dia);
        return promedio == null || Double.isNaN(promedioGeneral) || promedioGeneral == 0
                ? Double.NaN : promedio / promedioGeneral;
    }

    public DayOfWeek mejorDia() {
        return promedioPorDia.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public DayOfWeek peorDia() {
        return promedioPorDia.entrySet().stream()
                .min(Map.Entry.<DayOfWeek, Double>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).orElse(null);
    }
}
