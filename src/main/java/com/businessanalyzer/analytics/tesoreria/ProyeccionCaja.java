package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.prediccion.Pronosticador;
import com.businessanalyzer.analytics.prediccion.Pronostico;
import com.businessanalyzer.model.Deuda;

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
 * @param mesesHistoria meses completos con los que se hizo el pronóstico
 * @param meses         mes en curso (solo lo que falta del mes) y los siguientes
 * @param primerNegativo          primer mes en que el saldo base sería negativo; {@code null} si ninguno
 * @param primerNegativoPesimista primer mes en que el saldo pesimista sería negativo; {@code null} si ninguno
 */
public record ProyeccionCaja(BigDecimal saldoActual, int mesesHistoria, Pronostico pronostico,
                             List<MesProyectado> meses, YearMonth primerNegativo, YearMonth primerNegativoPesimista) {

    /** Meses completos de historia necesarios para proyectar (igual que las predicciones de ventas). */
    public static final int MINIMO_MESES = 4;
    /** Meses siguientes al mes en curso que se proyectan. */
    public static final int HORIZONTE = 6;

    public ProyeccionCaja {
        meses = List.copyOf(meses);
    }

    /**
     * @param operacion   flujo de operación proyectado (lo que falta del mes, si es el mes en curso)
     * @param deudas      capital de deudas que vence en el mes (negativo)
     */
    public record MesProyectado(YearMonth mes, BigDecimal operacion, BigDecimal operacionPesimista, BigDecimal deudas,
                                BigDecimal saldo, BigDecimal saldoPesimista) {
    }

    public boolean suficiente() {
        return pronostico != null && pronostico.suficiente();
    }

    /**
     * @param saldoActual       dinero disponible hoy en todas las cuentas
     * @param historiaOperacion flujo de operación de cada mes completo, del más antiguo al más reciente
     * @param deudas            deudas de la empresa (se usan las pendientes)
     */
    public static ProyeccionCaja calcular(LocalDate hoy, BigDecimal saldoActual, double[] historiaOperacion,
                                          Collection<Deuda> deudas) {
        if (historiaOperacion.length < MINIMO_MESES) {
            return new ProyeccionCaja(saldoActual, historiaOperacion.length,
                    Pronostico.insuficiente("Flujo de operación", historiaOperacion.length), List.of(), null, null);
        }
        Pronostico pronostico = Pronosticador.pronosticar("Flujo de operación", historiaOperacion, HORIZONTE + 1, false);
        YearMonth actual = YearMonth.from(hoy);
        Map<YearMonth, BigDecimal> capital = capitalPorMes(deudas, actual.plusMonths(1), HORIZONTE);
        // Del mes en curso solo falta lo que queda del mes.
        BigDecimal fraccionRestante = BigDecimal.valueOf(actual.lengthOfMonth() - hoy.getDayOfMonth())
                .divide(BigDecimal.valueOf(actual.lengthOfMonth()), 6, RoundingMode.HALF_UP);

        List<MesProyectado> meses = new ArrayList<>();
        BigDecimal saldo = saldoActual;
        BigDecimal saldoPesimista = saldoActual;
        YearMonth primerNegativo = null;
        YearMonth primerNegativoPesimista = null;
        for (int h = 1; h <= HORIZONTE + 1; h++) {
            YearMonth mes = actual.plusMonths(h - 1L);
            Pronostico.Punto punto = pronostico.puntos().get(h - 1);
            BigDecimal factor = h == 1 ? fraccionRestante : BigDecimal.ONE;
            BigDecimal operacion = dinero(punto.valor()).multiply(factor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal operacionPesimista = dinero(punto.inferior()).multiply(factor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal deuda = capital.getOrDefault(mes, BigDecimal.ZERO).negate();
            saldo = saldo.add(operacion).add(deuda);
            saldoPesimista = saldoPesimista.add(operacionPesimista).add(deuda);
            if (primerNegativo == null && saldo.signum() < 0) {
                primerNegativo = mes;
            }
            if (primerNegativoPesimista == null && saldoPesimista.signum() < 0) {
                primerNegativoPesimista = mes;
            }
            meses.add(new MesProyectado(mes, operacion, operacionPesimista, deuda, saldo, saldoPesimista));
        }
        return new ProyeccionCaja(saldoActual, historiaOperacion.length, pronostico, meses, primerNegativo,
                primerNegativoPesimista);
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
    static Map<YearMonth, BigDecimal> capitalPorMes(Collection<Deuda> deudas, YearMonth desde, int meses) {
        Map<YearMonth, BigDecimal> porMes = new TreeMap<>();
        YearMonth hasta = desde.plusMonths(meses - 1L);
        for (Deuda d : deudas) {
            if (!d.pendiente()) {
                continue;
            }
            BigDecimal saldo = d.saldo();
            if (d.cuotaMensual() != null) {
                BigDecimal tasaMensual = d.tasaAnual() == null ? BigDecimal.ZERO
                        : d.tasaAnual().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
                for (YearMonth mes = desde; !mes.isAfter(hasta) && saldo.signum() > 0; mes = mes.plusMonths(1)) {
                    BigDecimal interes = saldo.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal pago = d.cuotaMensual().subtract(interes).max(BigDecimal.ZERO).min(saldo);
                    if (pago.signum() == 0) {
                        break;
                    }
                    porMes.merge(mes, pago, BigDecimal::add);
                    saldo = saldo.subtract(pago);
                }
            } else if (d.vencimiento() != null) {
                YearMonth mes = YearMonth.from(d.vencimiento());
                if (mes.isBefore(desde)) {
                    mes = desde;
                }
                if (!mes.isAfter(hasta)) {
                    porMes.merge(mes, saldo, BigDecimal::add);
                }
            }
        }
        return porMes;
    }

    private static BigDecimal dinero(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
