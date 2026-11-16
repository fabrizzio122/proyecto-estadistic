package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.model.CuentaDinero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Saldo de una cuenta a una fecha de corte: saldo inicial + entradas − salidas.
 *
 * @param ultimoMovimiento fecha del último movimiento hasta el corte; {@code null} si no tiene
 */
public record SaldoCuenta(CuentaDinero cuenta, BigDecimal entradas, BigDecimal salidas, BigDecimal saldo,
                          LocalDate ultimoMovimiento) {

    public boolean negativo() {
        return saldo.signum() < 0;
    }

    /** Saldos de las cuentas indicadas con los flujos hasta la fecha de corte (inclusive). */
    public static List<SaldoCuenta> de(Collection<CuentaDinero> cuentas, Collection<FlujoDinero> flujos,
                                       LocalDate corte) {
        List<SaldoCuenta> saldos = new ArrayList<>();
        for (CuentaDinero cuenta : cuentas) {
            BigDecimal entradas = BigDecimal.ZERO;
            BigDecimal salidas = BigDecimal.ZERO;
            LocalDate ultimo = null;
            for (FlujoDinero f : flujos) {
                if (f.cuentaId() != cuenta.id() || f.fecha().isAfter(corte)) {
                    continue;
                }
                if (f.entrada()) {
                    entradas = entradas.add(f.monto());
                } else {
                    salidas = salidas.add(f.monto().negate());
                }
                if (ultimo == null || f.fecha().isAfter(ultimo)) {
                    ultimo = f.fecha();
                }
            }
            saldos.add(new SaldoCuenta(cuenta, entradas, salidas,
                    cuenta.saldoInicial().add(entradas).subtract(salidas), ultimo));
        }
        return saldos;
    }

    /** Suma de los saldos. */
    public static BigDecimal total(Collection<SaldoCuenta> saldos) {
        return saldos.stream().map(SaldoCuenta::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
