package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.model.CuentaDinero;

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
 * @param porRubro monto neto de cada rubro con movimiento (positivo = entra, negativo = sale)
 */
public record FlujoEfectivo(Periodo periodo, BigDecimal saldoInicial, Map<RubroFlujo, BigDecimal> porRubro,
                            BigDecimal saldoFinal) {

    public FlujoEfectivo {
        Map<RubroFlujo, BigDecimal> copia = new EnumMap<>(RubroFlujo.class);
        copia.putAll(porRubro);
        porRubro = Collections.unmodifiableMap(copia);
    }

    /**
     * @param cuentas todas las cuentas de la empresa (también las inactivas, porque su dinero ya se movió)
     * @param flujos  todos los flujos de dinero de la empresa
     */
    public static FlujoEfectivo de(Periodo periodo, Collection<CuentaDinero> cuentas, Collection<FlujoDinero> flujos) {
        BigDecimal inicial = cuentas.stream().map(CuentaDinero::saldoInicial).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<RubroFlujo, BigDecimal> rubros = new EnumMap<>(RubroFlujo.class);
        for (FlujoDinero f : flujos) {
            if (f.fecha().isBefore(periodo.desde())) {
                inicial = inicial.add(f.monto());
            } else if (!f.fecha().isAfter(periodo.hasta()) && f.rubro().actividad() != Actividad.INTERNA) {
                rubros.merge(f.rubro(), f.monto(), BigDecimal::add);
            }
        }
        BigDecimal neto = rubros.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FlujoEfectivo(periodo, inicial, rubros, inicial.add(neto));
    }

    /** Monto neto del rubro; cero si no tuvo movimiento. */
    public BigDecimal rubro(RubroFlujo rubro) {
        return porRubro.getOrDefault(rubro, BigDecimal.ZERO);
    }

    /** Flujo neto de una actividad. */
    public BigDecimal neto(Actividad actividad) {
        return porRubro.entrySet().stream()
                .filter(e -> e.getKey().actividad() == actividad)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Aumento (positivo) o disminución (negativo) del efectivo en el período. */
    public BigDecimal variacion() {
        return saldoFinal.subtract(saldoInicial);
    }

    public boolean sinMovimientos() {
        return porRubro.isEmpty();
    }
}
