package com.businessanalyzer.analytics.tesoreria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entrada (monto positivo) o salida (monto negativo) de dinero de una cuenta. Se obtiene de los
 * registros de la empresa: ventas, compras, cobros, ingresos, gastos, activos, deudas y movimientos
 * de tesorería.
 *
 * @param conceptoFGEM texto para mostrar, por ejemplo «Venta V-000123»
 */
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
