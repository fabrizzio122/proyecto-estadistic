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
     * Período anterior equivalente para comparar:
     * <ul>
     *   <li>meses completos: los mismos meses previos (octubre para noviembre);</li>
     *   <li>un mes en curso (del 1 al 15 de noviembre): los mismos días del mes anterior (1 al 15 de octubre),
     *       para no comparar medio mes contra un mes entero;</li>
     *   <li>el año en curso (del 1 de enero a hoy): las mismas fechas del año anterior;</li>
     *   <li>cualquier otro rango: la misma cantidad de días inmediatamente antes.</li>
     * </ul>
     */
    public Periodo anterior() {
        long meses = ChronoUnit.MONTHS.between(YearMonth.from(desde), YearMonth.from(hasta)) + 1;
        if (mesesCompletos()) {
            YearMonth inicio = YearMonth.from(desde).minusMonths(meses);
            return new Periodo(inicio.atDay(1), desde.minusDays(1));
        }
        if (desde.getDayOfMonth() == 1 && meses == 1) {
            return new Periodo(desde.minusMonths(1), hasta.minusMonths(1));
        }
        if (desde.getDayOfYear() == 1 && desde.getYear() == hasta.getYear()) {
            return new Periodo(desde.minusYears(1), hasta.minusYears(1));
        }
        return new Periodo(desde.minusDays(dias()), desde.minusDays(1));
    }

    /** "noviembre 2026", "enero – marzo 2026", "del 1 al 13 de noviembre 2026" o "15/10/2026 – 13/11/2026". */
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
        if (YearMonth.from(desde).equals(YearMonth.from(hasta))) {
            return "del " + desde.getDayOfMonth() + " al " + hasta.getDayOfMonth() + " de "
                    + Fechas.nombreMes(hasta.getMonthValue()).toLowerCase() + " " + hasta.getYear();
        }
        return Fechas.paraMostrar(desde) + " – " + Fechas.paraMostrar(hasta);
    }
}
