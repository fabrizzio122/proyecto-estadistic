package com.businessanalyzer.analytics;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.util.FechasFGEM;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public record PeriodoFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {

    public PeriodoFGEM {
        if (desdeFGEM == null || hastaFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica las fechas del período.");
        }
        if (desdeFGEM.isAfter(hastaFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha inicial no puede ser posterior a la final.");
        }
    }

    public static PeriodoFGEM mesFGEM(YearMonth mesFGEM) {
        return new PeriodoFGEM(mesFGEM.atDay(1), mesFGEM.atEndOfMonth());
    }

    public boolean mesesCompletosFGEM() {
        return desdeFGEM.getDayOfMonth() == 1 && hastaFGEM.equals(YearMonth.from(hastaFGEM).atEndOfMonth());
    }

    public long diasFGEM() {
        return ChronoUnit.DAYS.between(desdeFGEM, hastaFGEM) + 1;
    }

    public PeriodoFGEM anteriorFGEM() {
        long mesesFGEM = ChronoUnit.MONTHS.between(YearMonth.from(desdeFGEM), YearMonth.from(hastaFGEM)) + 1;
        if (mesesCompletosFGEM()) {
            YearMonth inicioFGEM = YearMonth.from(desdeFGEM).minusMonths(mesesFGEM);
            return new PeriodoFGEM(inicioFGEM.atDay(1), desdeFGEM.minusDays(1));
        }
        if (desdeFGEM.getDayOfMonth() == 1 && mesesFGEM == 1) {
            return new PeriodoFGEM(desdeFGEM.minusMonths(1), hastaFGEM.minusMonths(1));
        }
        if (desdeFGEM.getDayOfYear() == 1 && desdeFGEM.getYear() == hastaFGEM.getYear()) {
            return new PeriodoFGEM(desdeFGEM.minusYears(1), hastaFGEM.minusYears(1));
        }
        return new PeriodoFGEM(desdeFGEM.minusDays(diasFGEM()), desdeFGEM.minusDays(1));
    }

    public String descripcionFGEM() {
        if (mesesCompletosFGEM()) {
            YearMonth inicioFGEM = YearMonth.from(desdeFGEM);
            YearMonth finFGEM = YearMonth.from(hastaFGEM);
            String nombreInicioFGEM = FechasFGEM.nombreMesFGEM(inicioFGEM.getMonthValue()).toLowerCase();
            if (inicioFGEM.equals(finFGEM)) {
                return nombreInicioFGEM + " " + inicioFGEM.getYear();
            }
            return nombreInicioFGEM + (inicioFGEM.getYear() == finFGEM.getYear() ? "" : " " + inicioFGEM.getYear()) + " – "
                    + FechasFGEM.nombreMesFGEM(finFGEM.getMonthValue()).toLowerCase() + " " + finFGEM.getYear();
        }
        if (YearMonth.from(desdeFGEM).equals(YearMonth.from(hastaFGEM))) {
            return "del " + desdeFGEM.getDayOfMonth() + " al " + hastaFGEM.getDayOfMonth() + " de "
                    + FechasFGEM.nombreMesFGEM(hastaFGEM.getMonthValue()).toLowerCase() + " " + hastaFGEM.getYear();
        }
        return FechasFGEM.paraMostrarFGEM(desdeFGEM) + " – " + FechasFGEM.paraMostrarFGEM(hastaFGEM);
    }
}
