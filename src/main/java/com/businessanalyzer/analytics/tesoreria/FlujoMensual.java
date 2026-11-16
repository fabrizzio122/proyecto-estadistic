package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.model.CuentaDinero;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entradas, salidas y saldo de efectivo al cierre de un mes (sin contar las transferencias entre
 * cuentas, que no cambian el total).
 *
 * @param operacion flujo neto de las actividades de operación del mes
 */
public record FlujoMensual(YearMonth mes, BigDecimal entradas, BigDecimal salidas, BigDecimal operacion,
                           BigDecimal saldoCierre) {

    public BigDecimal neto() {
        return entradas.subtract(salidas);
    }

    /** Un registro por mes, desde {@code desde} hasta {@code hasta} (inclusive), aunque no tenga movimientos. */
    public static List<FlujoMensual> serie(YearMonth desde, YearMonth hasta, Collection<CuentaDinero> cuentas,
                                           Collection<FlujoDinero> flujos) {
        BigDecimal saldo = cuentas.stream().map(CuentaDinero::saldoInicial).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (FlujoDinero f : flujos) {
            if (YearMonth.from(f.fecha()).isBefore(desde)) {
                saldo = saldo.add(f.monto());
            }
        }
        List<FlujoMensual> serie = new ArrayList<>();
        for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
            BigDecimal entradas = BigDecimal.ZERO;
            BigDecimal salidas = BigDecimal.ZERO;
            BigDecimal operacion = BigDecimal.ZERO;
            for (FlujoDinero f : flujos) {
                if (!YearMonth.from(f.fecha()).equals(mes) || f.rubro().actividad() == Actividad.INTERNA) {
                    continue;
                }
                if (f.entrada()) {
                    entradas = entradas.add(f.monto());
                } else {
                    salidas = salidas.add(f.monto().negate());
                }
                if (f.rubro().actividad() == Actividad.OPERACION) {
                    operacion = operacion.add(f.monto());
                }
            }
            saldo = saldo.add(entradas).subtract(salidas);
            serie.add(new FlujoMensual(mes, entradas, salidas, operacion, saldo));
        }
        return serie;
    }
}
