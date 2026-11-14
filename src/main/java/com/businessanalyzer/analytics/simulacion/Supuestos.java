package com.businessanalyzer.analytics.simulacion;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Supuestos de un escenario. Los cambios son fracciones (0.10 = +10 %, −0.05 = −5 %).
 *
 * @param costoFijoAdicional monto mensual que se suma a los costos fijos (por ejemplo, un nuevo empleado);
 *                           puede ser negativo (un ahorro)
 * @param tasaImpuesto       fracción de la utilidad antes de impuestos que se paga de impuesto a la renta
 */
public record Supuestos(BigDecimal precio, BigDecimal volumen, BigDecimal costoUnitario, BigDecimal gastosVariables,
                        BigDecimal costosFijos, BigDecimal costoFijoAdicional, BigDecimal tasaImpuesto) {

    public Supuestos {
        Objects.requireNonNull(precio, "precio");
        Objects.requireNonNull(volumen, "volumen");
        Objects.requireNonNull(costoUnitario, "costoUnitario");
        Objects.requireNonNull(gastosVariables, "gastosVariables");
        Objects.requireNonNull(costosFijos, "costosFijos");
        Objects.requireNonNull(costoFijoAdicional, "costoFijoAdicional");
        Objects.requireNonNull(tasaImpuesto, "tasaImpuesto");
    }

    /** Sin cambios: el escenario reproduce la base. */
    public static Supuestos neutros(BigDecimal tasaImpuesto) {
        BigDecimal cero = BigDecimal.ZERO;
        return new Supuestos(cero, cero, cero, cero, cero, cero, tasaImpuesto);
    }

    public BigDecimal valor(Variable variable) {
        return switch (variable) {
            case PRECIO -> precio;
            case VOLUMEN -> volumen;
            case COSTO_UNITARIO -> costoUnitario;
            case GASTOS_VARIABLES -> gastosVariables;
            case COSTOS_FIJOS -> costosFijos;
        };
    }

    public Supuestos con(Variable variable, BigDecimal nuevo) {
        return new Supuestos(
                variable == Variable.PRECIO ? nuevo : precio,
                variable == Variable.VOLUMEN ? nuevo : volumen,
                variable == Variable.COSTO_UNITARIO ? nuevo : costoUnitario,
                variable == Variable.GASTOS_VARIABLES ? nuevo : gastosVariables,
                variable == Variable.COSTOS_FIJOS ? nuevo : costosFijos,
                costoFijoAdicional, tasaImpuesto);
    }

    public Supuestos conCostoFijoAdicional(BigDecimal monto) {
        return new Supuestos(precio, volumen, costoUnitario, gastosVariables, costosFijos, monto, tasaImpuesto);
    }

    public Supuestos conTasaImpuesto(BigDecimal tasa) {
        return new Supuestos(precio, volumen, costoUnitario, gastosVariables, costosFijos, costoFijoAdicional, tasa);
    }
}
