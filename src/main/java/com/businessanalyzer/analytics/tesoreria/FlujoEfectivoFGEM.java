package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Estado de flujo de efectivo de un período por el método directo:
 * <pre>
 *   Saldo inicial de efectivo
 * + Flujo neto de operación      (cobros de ventas e ingresos − pagos de compras, gastos e intereses)
 * + Flujo neto de inversión      (− compra de activos)
 * + Flujo neto de financiamiento (préstamos y aportes − pagos de capital y retiros)
 * = Saldo final de efectivo
 * </pre>
 * Las transferencias entre cuentas no cambian el total, por eso no aparecen.
 *
 * @param porRubroFGEM monto neto de cada rubro con movimiento (positivo = entra, negativo = sale)
 */
public record FlujoEfectivoFGEM(PeriodoFGEM periodoFGEM, BigDecimal saldoInicialFGEM, Map<RubroFlujoFGEM, BigDecimal> porRubroFGEM,
                            BigDecimal saldoFinalFGEM) {

    public FlujoEfectivoFGEM {
        Map<RubroFlujoFGEM, BigDecimal> copiaFGEM = new EnumMap<>(RubroFlujoFGEM.class);
        copiaFGEM.putAll(porRubroFGEM);
        porRubroFGEM = Collections.unmodifiableMap(copiaFGEM);
    }

    /**
     * @param cuentasFGEM todas las cuentas de la empresa (también las inactivas, porque su dinero ya se movió)
     * @param flujosFGEM  todos los flujos de dinero de la empresa
     */
    public static FlujoEfectivoFGEM deFGEM(PeriodoFGEM periodoFGEM, Collection<CuentaDineroFGEM> cuentasFGEM, Collection<FlujoDineroFGEM> flujosFGEM) {
        BigDecimal inicialFGEM = cuentasFGEM.stream().map(CuentaDineroFGEM::saldoInicialFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<RubroFlujoFGEM, BigDecimal> rubrosFGEM = new EnumMap<>(RubroFlujoFGEM.class);
        for (FlujoDineroFGEM fFGEM : flujosFGEM) {
            if (fFGEM.fechaFGEM().isBefore(periodoFGEM.desdeFGEM())) {
                inicialFGEM = inicialFGEM.add(fFGEM.montoFGEM());
            } else if (!fFGEM.fechaFGEM().isAfter(periodoFGEM.hastaFGEM()) && fFGEM.rubroFGEM().actividadFGEM() != ActividadFGEM.INTERNA) {
                rubrosFGEM.merge(fFGEM.rubroFGEM(), fFGEM.montoFGEM(), BigDecimal::add);
            }
        }
        BigDecimal netoFGEM = rubrosFGEM.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FlujoEfectivoFGEM(periodoFGEM, inicialFGEM, rubrosFGEM, inicialFGEM.add(netoFGEM));
    }

    /** Monto neto del rubro; cero si no tuvo movimiento. */
    public BigDecimal rubroFGEM(RubroFlujoFGEM rubroFGEM) {
        return porRubroFGEM.getOrDefault(rubroFGEM, BigDecimal.ZERO);
    }

    /** Flujo neto de una actividad. */
    public BigDecimal netoFGEM(ActividadFGEM actividadFGEM) {
        return porRubroFGEM.entrySet().stream()
                .filter(eFGEM -> eFGEM.getKey().actividadFGEM() == actividadFGEM)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Aumento (positivo) o disminución (negativo) del efectivo en el período. */
    public BigDecimal variacionFGEM() {
        return saldoFinalFGEM.subtract(saldoInicialFGEM);
    }

    public boolean sinMovimientosFGEM() {
        return porRubroFGEM.isEmpty();
    }
}
