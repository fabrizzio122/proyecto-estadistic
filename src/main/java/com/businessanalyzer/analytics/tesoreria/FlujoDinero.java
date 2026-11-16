package com.businessanalyzer.analytics.tesoreria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entrada (monto positivo) o salida (monto negativo) de dinero de una cuenta. Se obtiene de los
 * registros de la empresa: ventas, compras, cobros, ingresos, gastos, activos, deudas y movimientos
 * de tesorería.
 *
 * @param concepto texto para mostrar, por ejemplo «Venta V-000123»
 */
public record FlujoDinero(LocalDate fecha, long cuentaId, BigDecimal monto, RubroFlujo rubro, String concepto) {

    public FlujoDinero {
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(monto, "monto");
        Objects.requireNonNull(rubro, "rubro");
    }

    public boolean entrada() {
        return monto.signum() > 0;
    }
}
