package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.model.CuentaDineroFGEM;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entradas, salidas y saldo de efectivo al cierre de un mes (sin contar las transferencias entre
 * cuentas, que no cambian el total).
 *
 * @param operacionFGEM flujo neto de las actividades de operación del mes
 */
public record FlujoMensualFGEM(YearMonth mesFGEM, BigDecimal entradasFGEM, BigDecimal salidasFGEM, BigDecimal operacionFGEM,
                           BigDecimal saldoCierreFGEM) {

    public BigDecimal netoFGEM() {
        return entradasFGEM.subtract(salidasFGEM);
    }

    /** Un registro por mes, desde {@code desde} hasta {@code hasta} (inclusive), aunque no tenga movimientos. */
    public static List<FlujoMensualFGEM> serieFGEM(YearMonth desdeFGEM, YearMonth hastaFGEM, Collection<CuentaDineroFGEM> cuentasFGEM,
                                           Collection<FlujoDineroFGEM> flujosFGEM) {
        BigDecimal saldoFGEM = cuentasFGEM.stream().map(CuentaDineroFGEM::saldoInicialFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (FlujoDineroFGEM fFGEM : flujosFGEM) {
            if (YearMonth.from(fFGEM.fechaFGEM()).isBefore(desdeFGEM)) {
                saldoFGEM = saldoFGEM.add(fFGEM.montoFGEM());
            }
        }
        List<FlujoMensualFGEM> serieFGEM = new ArrayList<>();
        for (YearMonth mesFGEM = desdeFGEM; !mesFGEM.isAfter(hastaFGEM); mesFGEM = mesFGEM.plusMonths(1)) {
            BigDecimal entradasFGEM = BigDecimal.ZERO;
            BigDecimal salidasFGEM = BigDecimal.ZERO;
            BigDecimal operacionFGEM = BigDecimal.ZERO;
            for (FlujoDineroFGEM fFGEM : flujosFGEM) {
                if (!YearMonth.from(fFGEM.fechaFGEM()).equals(mesFGEM) || fFGEM.rubroFGEM().actividadFGEM() == ActividadFGEM.INTERNA) {
                    continue;
                }
                if (fFGEM.entradaFGEM()) {
                    entradasFGEM = entradasFGEM.add(fFGEM.montoFGEM());
                } else {
                    salidasFGEM = salidasFGEM.add(fFGEM.montoFGEM().negate());
                }
                if (fFGEM.rubroFGEM().actividadFGEM() == ActividadFGEM.OPERACION) {
                    operacionFGEM = operacionFGEM.add(fFGEM.montoFGEM());
                }
            }
            saldoFGEM = saldoFGEM.add(entradasFGEM).subtract(salidasFGEM);
            serieFGEM.add(new FlujoMensualFGEM(mesFGEM, entradasFGEM, salidasFGEM, operacionFGEM, saldoFGEM));
        }
        return serieFGEM;
    }
}
