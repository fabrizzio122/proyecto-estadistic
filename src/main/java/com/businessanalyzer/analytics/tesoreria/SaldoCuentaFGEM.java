package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.model.CuentaDineroFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Saldo de una cuenta a una fecha de corte: saldo inicial + entradas − salidas.
 *
 * @param ultimoMovimientoFGEM fecha del último movimiento hasta el corte; {@code null} si no tiene
 */
public record SaldoCuentaFGEM(CuentaDineroFGEM cuentaFGEM, BigDecimal entradasFGEM, BigDecimal salidasFGEM, BigDecimal saldoFGEM,
                          LocalDate ultimoMovimientoFGEM) {

    public boolean negativoFGEM() {
        return saldoFGEM.signum() < 0;
    }

    /** Saldos de las cuentas indicadas con los flujos hasta la fecha de corte (inclusive). */
    public static List<SaldoCuentaFGEM> deFGEM(Collection<CuentaDineroFGEM> cuentasFGEM, Collection<FlujoDineroFGEM> flujosFGEM,
                                       LocalDate corteFGEM) {
        List<SaldoCuentaFGEM> saldosFGEM = new ArrayList<>();
        for (CuentaDineroFGEM cuentaFGEM : cuentasFGEM) {
            BigDecimal entradasFGEM = BigDecimal.ZERO;
            BigDecimal salidasFGEM = BigDecimal.ZERO;
            LocalDate ultimoFGEM = null;
            for (FlujoDineroFGEM fFGEM : flujosFGEM) {
                if (fFGEM.cuentaIdFGEM() != cuentaFGEM.idFGEM() || fFGEM.fechaFGEM().isAfter(corteFGEM)) {
                    continue;
                }
                if (fFGEM.entradaFGEM()) {
                    entradasFGEM = entradasFGEM.add(fFGEM.montoFGEM());
                } else {
                    salidasFGEM = salidasFGEM.add(fFGEM.montoFGEM().negate());
                }
                if (ultimoFGEM == null || fFGEM.fechaFGEM().isAfter(ultimoFGEM)) {
                    ultimoFGEM = fFGEM.fechaFGEM();
                }
            }
            saldosFGEM.add(new SaldoCuentaFGEM(cuentaFGEM, entradasFGEM, salidasFGEM,
                    cuentaFGEM.saldoInicialFGEM().add(entradasFGEM).subtract(salidasFGEM), ultimoFGEM));
        }
        return saldosFGEM;
    }

    /** Suma de los saldos. */
    public static BigDecimal totalFGEM(Collection<SaldoCuentaFGEM> saldosFGEM) {
        return saldosFGEM.stream().map(SaldoCuentaFGEM::saldoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
