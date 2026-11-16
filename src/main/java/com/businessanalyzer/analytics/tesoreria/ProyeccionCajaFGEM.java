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

/**
 * Proyección del dinero disponible para los próximos meses:
 * <pre>
 *   saldo del mes = saldo del mes anterior
 *                 + flujo de operación pronosticado (con el mejor método de la Fase 9)
 *                 − capital de las deudas que vence ese mes
 * </pre>
 * El escenario pesimista usa el límite inferior del intervalo de confianza del pronóstico. Los intereses
 * ya están en el flujo de operación (se registran como gasto), por eso de las cuotas solo se descuenta
 * el capital.
 *
 * @param mesesHistoriaFGEM meses completos con los que se hizo el pronóstico
 * @param mesesFGEM         mes en curso (solo lo que falta del mes) y los siguientes
 * @param primerNegativoFGEM          primer mes en que el saldo base sería negativo; {@code null} si ninguno
 * @param primerNegativoPesimistaFGEM primer mes en que el saldo pesimista sería negativo; {@code null} si ninguno
 */
public record ProyeccionCajaFGEM(BigDecimal saldoActualFGEM, int mesesHistoriaFGEM, PronosticoFGEM pronosticoFGEM,
                             List<MesProyectadoFGEM> mesesFGEM, YearMonth primerNegativoFGEM, YearMonth primerNegativoPesimistaFGEM) {

    /** Meses completos de historia necesarios para proyectar (igual que las predicciones de ventas). */
    public static final int MINIMO_MESES_FGEM = 4;
    /** Meses siguientes al mes en curso que se proyectan. */
    public static final int HORIZONTE_FGEM = 6;

    public ProyeccionCajaFGEM {
        mesesFGEM = List.copyOf(mesesFGEM);
    }

    /**
     * @param operacionFGEM   flujo de operación proyectado (lo que falta del mes, si es el mes en curso)
     * @param deudasFGEM      capital de deudas que vence en el mes (negativo)
     */
    public record MesProyectadoFGEM(YearMonth mesFGEM, BigDecimal operacionFGEM, BigDecimal operacionPesimistaFGEM, BigDecimal deudasFGEM,
                                BigDecimal saldoFGEM, BigDecimal saldoPesimistaFGEM) {
    }

    public boolean suficienteFGEM() {
        return pronosticoFGEM != null && pronosticoFGEM.suficienteFGEM();
    }

    /**
     * @param saldoActualFGEM       dinero disponible hoy en todas las cuentas
     * @param historiaOperacionFGEM flujo de operación de cada mes completo, del más antiguo al más reciente
     * @param deudasFGEM            deudas de la empresa (se usan las pendientes)
     */
    public static ProyeccionCajaFGEM calcularFGEM(LocalDate hoyFGEM, BigDecimal saldoActualFGEM, double[] historiaOperacionFGEM,
                                          Collection<DeudaFGEM> deudasFGEM) {
        if (historiaOperacionFGEM.length < MINIMO_MESES_FGEM) {
            return new ProyeccionCajaFGEM(saldoActualFGEM, historiaOperacionFGEM.length,
                    PronosticoFGEM.insuficienteFGEM("Flujo de operación", historiaOperacionFGEM.length), List.of(), null, null);
        }
        PronosticoFGEM pronosticoFGEM = PronosticadorFGEM.pronosticarFGEM("Flujo de operación", historiaOperacionFGEM, HORIZONTE_FGEM + 1, false);
        YearMonth actualFGEM = YearMonth.from(hoyFGEM);
        Map<YearMonth, BigDecimal> capitalFGEM = capitalPorMesFGEM(deudasFGEM, actualFGEM.plusMonths(1), HORIZONTE_FGEM);
        // Del mes en curso solo falta lo que queda del mes.
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

    /**
     * Capital que se pagará cada mes en las deudas pendientes:
     * <ul>
     *   <li>Con cuota mensual: cada mes la cuota menos el interés del mes (saldo × tasa anual / 12),
     *       hasta cancelar el saldo.</li>
     *   <li>Sin cuota pero con vencimiento: todo el saldo en el mes del vencimiento (o el primer mes
     *       proyectado, si ya venció).</li>
     *   <li>Sin cuota ni vencimiento: no se puede proyectar.</li>
     * </ul>
     */
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
