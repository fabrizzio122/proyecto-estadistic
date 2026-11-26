package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.prediccion.PronosticadorFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM;
import com.businessanalyzer.model.DeudaFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record ProyeccionCajaFGEM(BigDecimal saldoActualFGEM, int mesesHistoriaFGEM, PronosticoFGEM pronosticoFGEM,
                             List<MesProyectadoFGEM> mesesFGEM, YearMonth primerNegativoFGEM, YearMonth primerNegativoPesimistaFGEM) {

    public static final int MINIMO_MESES_FGEM = 4;
    public static final int HORIZONTE_FGEM = 6;

    public ProyeccionCajaFGEM {
        mesesFGEM = List.copyOf(mesesFGEM);
    }

    public record MesProyectadoFGEM(YearMonth mesFGEM, BigDecimal operacionFGEM, BigDecimal operacionPesimistaFGEM, BigDecimal deudasFGEM,
                                BigDecimal saldoFGEM, BigDecimal saldoPesimistaFGEM) {
    }

    public boolean suficienteFGEM() {
        return pronosticoFGEM != null && pronosticoFGEM.suficienteFGEM();
    }

    public static ProyeccionCajaFGEM calcularFGEM(LocalDate hoyFGEM, BigDecimal saldoActualFGEM, double[] historiaOperacionFGEM,
                                          Collection<DeudaFGEM> deudasFGEM) {
        if (historiaOperacionFGEM.length < MINIMO_MESES_FGEM) {
            return new ProyeccionCajaFGEM(saldoActualFGEM, historiaOperacionFGEM.length,
                    PronosticoFGEM.insuficienteFGEM("Flujo de operación", historiaOperacionFGEM.length), List.of(), null, null);
        }
        PronosticoFGEM pronosticoFGEM = PronosticadorFGEM.pronosticarFGEM("Flujo de operación", historiaOperacionFGEM, HORIZONTE_FGEM + 1, false);
        YearMonth actualFGEM = YearMonth.from(hoyFGEM);
        Map<YearMonth, BigDecimal> capitalFGEM = capitalPorMesFGEM(deudasFGEM, actualFGEM.plusMonths(1), HORIZONTE_FGEM);
        BigDecimal fraccionRestanteFGEM = BigDecimal.valueOf(actualFGEM.lengthOfMonth() - hoyFGEM.getDayOfMonth())
                .divide(BigDecimal.valueOf(actualFGEM.lengthOfMonth()), 6, RoundingMode.HALF_UP);

        List<MesProyectadoFGEM> mesesFGEM = new ArrayList<>();
        BigDecimal saldoFGEM = saldoActualFGEM;
        BigDecimal saldoPesimistaFGEM = saldoActualFGEM;
        YearMonth primerNegativoFGEM = null;
        YearMonth primerNegativoPesimistaFGEM = null;
        for (int hFGEM = 1; hFGEM <= HORIZONTE_FGEM + 1; hFGEM++) {
            YearMonth mesFGEM = actualFGEM.plusMonths(hFGEM - 1L);
            PronosticoFGEM.PuntoFGEM puntoFGEM = pronosticoFGEM.puntosFGEM().get(hFGEM - 1);
            BigDecimal factorFGEM = hFGEM == 1 ? fraccionRestanteFGEM : BigDecimal.ONE;
            BigDecimal operacionFGEM = dineroFGEM(puntoFGEM.valorFGEM()).multiply(factorFGEM).setScale(2, RoundingMode.HALF_UP);
            BigDecimal operacionPesimistaFGEM = dineroFGEM(puntoFGEM.inferiorFGEM()).multiply(factorFGEM).setScale(2, RoundingMode.HALF_UP);
            BigDecimal deudaFGEM = capitalFGEM.getOrDefault(mesFGEM, BigDecimal.ZERO).negate();
            saldoFGEM = saldoFGEM.add(operacionFGEM).add(deudaFGEM);
            saldoPesimistaFGEM = saldoPesimistaFGEM.add(operacionPesimistaFGEM).add(deudaFGEM);
            if (primerNegativoFGEM == null && saldoFGEM.signum() < 0) {
                primerNegativoFGEM = mesFGEM;
            }
            if (primerNegativoPesimistaFGEM == null && saldoPesimistaFGEM.signum() < 0) {
                primerNegativoPesimistaFGEM = mesFGEM;
            }
            mesesFGEM.add(new MesProyectadoFGEM(mesFGEM, operacionFGEM, operacionPesimistaFGEM, deudaFGEM, saldoFGEM, saldoPesimistaFGEM));
        }
        return new ProyeccionCajaFGEM(saldoActualFGEM, historiaOperacionFGEM.length, pronosticoFGEM, mesesFGEM, primerNegativoFGEM,
                primerNegativoPesimistaFGEM);
    }

    static Map<YearMonth, BigDecimal> capitalPorMesFGEM(Collection<DeudaFGEM> deudasFGEM, YearMonth desdeFGEM, int mesesFGEM) {
        Map<YearMonth, BigDecimal> porMesFGEM = new TreeMap<>();
        YearMonth hastaFGEM = desdeFGEM.plusMonths(mesesFGEM - 1L);
        for (DeudaFGEM dFGEM : deudasFGEM) {
            if (!dFGEM.pendienteFGEM()) {
                continue;
            }
            BigDecimal saldoFGEM = dFGEM.saldoFGEM();
            if (dFGEM.cuotaMensualFGEM() != null) {
                BigDecimal tasaMensualFGEM = dFGEM.tasaAnualFGEM() == null ? BigDecimal.ZERO
                        : dFGEM.tasaAnualFGEM().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
                for (YearMonth mesFGEM = desdeFGEM; !mesFGEM.isAfter(hastaFGEM) && saldoFGEM.signum() > 0; mesFGEM = mesFGEM.plusMonths(1)) {
                    BigDecimal interesFGEM = saldoFGEM.multiply(tasaMensualFGEM).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal pagoFGEM = dFGEM.cuotaMensualFGEM().subtract(interesFGEM).max(BigDecimal.ZERO).min(saldoFGEM);
                    if (pagoFGEM.signum() == 0) {
                        break;
                    }
                    porMesFGEM.merge(mesFGEM, pagoFGEM, BigDecimal::add);
                    saldoFGEM = saldoFGEM.subtract(pagoFGEM);
                }
            } else if (dFGEM.vencimientoFGEM() != null) {
                YearMonth mesFGEM = YearMonth.from(dFGEM.vencimientoFGEM());
                if (mesFGEM.isBefore(desdeFGEM)) {
                    mesFGEM = desdeFGEM;
                }
                if (!mesFGEM.isAfter(hastaFGEM)) {
                    porMesFGEM.merge(mesFGEM, saldoFGEM, BigDecimal::add);
                }
            }
        }
        return porMesFGEM;
    }

    private static BigDecimal dineroFGEM(double valorFGEM) {
        return BigDecimal.valueOf(valorFGEM).setScale(2, RoundingMode.HALF_UP);
    }
}
