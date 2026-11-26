package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record FlujoEfectivoFGEM(PeriodoFGEM periodoFGEM, BigDecimal saldoInicialFGEM, Map<RubroFlujoFGEM, BigDecimal> porRubroFGEM,
                            BigDecimal saldoFinalFGEM) {

    public FlujoEfectivoFGEM {
        Map<RubroFlujoFGEM, BigDecimal> copiaFGEM = new EnumMap<>(RubroFlujoFGEM.class);
        copiaFGEM.putAll(porRubroFGEM);
        porRubroFGEM = Collections.unmodifiableMap(copiaFGEM);
    }

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

    public BigDecimal rubroFGEM(RubroFlujoFGEM rubroFGEM) {
        return porRubroFGEM.getOrDefault(rubroFGEM, BigDecimal.ZERO);
    }

    public BigDecimal netoFGEM(ActividadFGEM actividadFGEM) {
        return porRubroFGEM.entrySet().stream()
                .filter(eFGEM -> eFGEM.getKey().actividadFGEM() == actividadFGEM)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal variacionFGEM() {
        return saldoFinalFGEM.subtract(saldoInicialFGEM);
    }

    public boolean sinMovimientosFGEM() {
        return porRubroFGEM.isEmpty();
    }
}
