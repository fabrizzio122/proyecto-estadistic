package com.businessanalyzer.analytics.tesoreria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record FlujoDineroFGEM(LocalDate fechaFGEM, long cuentaIdFGEM, BigDecimal montoFGEM, RubroFlujoFGEM rubroFGEM, String conceptoFGEM) {

    public FlujoDineroFGEM {
        Objects.requireNonNull(fechaFGEM, "fecha");
        Objects.requireNonNull(montoFGEM, "monto");
        Objects.requireNonNull(rubroFGEM, "rubro");
    }

    public boolean entradaFGEM() {
        return montoFGEM.signum() > 0;
    }
}
