package com.businessanalyzer.analytics;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.util.Fechas;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/** Rango de fechas de un análisis, con su período anterior equivalente para comparar. */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public Periodo {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Indica las fechas del período.");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException("La fecha inicial no puede ser posterior a la final.");
        }
    }

    public static Periodo mes(YearMonth mes) {
        return new Periodo(mes.atDay(1), mes.atEndOfMonth());
    }

    /** El período comprende meses completos (del día 1 al último día). */
    public boolean mesesCompletos() {
        return desde.getDayOfMonth() == 1 && hasta.equals(YearMonth.from(hasta).atEndOfMonth());
    }

    public long dias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    /**
     * Período inmediatamente anterior de igual duración: si son meses completos, los mismos
     * meses previos (octubre para noviembre); si no, la misma cantidad de días.
     */
    public Periodo anterior() {
        if (mesesCompletos()) {
            long meses = ChronoUnit.MONTHS.between(YearMonth.from(desde), YearMonth.from(hasta)) + 1;
            YearMonth inicio = YearMonth.from(desde).minusMonths(meses);
            return new Periodo(inicio.atDay(1), desde.minusDays(1));
        }
        return new Periodo(desde.minusDays(dias()), desde.minusDays(1));
    }

    /** "noviembre 2026", "enero – marzo 2026" o "01/11/2026 – 13/11/2026". */
    public String descripcion() {
        if (mesesCompletos()) {
            YearMonth inicio = YearMonth.from(desde);
            YearMonth fin = YearMonth.from(hasta);
            String nombreInicio = Fechas.nombreMes(inicio.getMonthValue()).toLowerCase();
            if (inicio.equals(fin)) {
                return nombreInicio + " " + inicio.getYear();
            }
            return nombreInicio + (inicio.getYear() == fin.getYear() ? "" : " " + inicio.getYear()) + " – "
                    + Fechas.nombreMes(fin.getMonthValue()).toLowerCase() + " " + fin.getYear();
        }
        return Fechas.paraMostrar(desde) + " – " + Fechas.paraMostrar(hasta);
    }
}
