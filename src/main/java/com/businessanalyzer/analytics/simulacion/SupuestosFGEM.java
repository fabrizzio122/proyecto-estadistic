package com.businessanalyzer.analytics.simulacion;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Supuestos de un escenario. Los cambios son fracciones (0.10 = +10 %, −0.05 = −5 %).
 *
 * @param costoFijoAdicionalFGEM monto mensual que se suma a los costos fijos (por ejemplo, un nuevo empleado);
 *                           puede ser negativo (un ahorro)
 * @param tasaImpuestoFGEM       fracción de la utilidad antes de impuestos que se paga de impuesto a la renta
 */
public record SupuestosFGEM(BigDecimal precioFGEM, BigDecimal volumenFGEM, BigDecimal costoUnitarioFGEM, BigDecimal gastosVariablesFGEM,
                        BigDecimal costosFijosFGEM, BigDecimal costoFijoAdicionalFGEM, BigDecimal tasaImpuestoFGEM) {

    public SupuestosFGEM {
        Objects.requireNonNull(precioFGEM, "precio");
        Objects.requireNonNull(volumenFGEM, "volumen");
        Objects.requireNonNull(costoUnitarioFGEM, "costoUnitario");
        Objects.requireNonNull(gastosVariablesFGEM, "gastosVariables");
        Objects.requireNonNull(costosFijosFGEM, "costosFijos");
        Objects.requireNonNull(costoFijoAdicionalFGEM, "costoFijoAdicional");
        Objects.requireNonNull(tasaImpuestoFGEM, "tasaImpuesto");
    }

    /** Sin cambios: el escenario reproduce la base. */
    public static SupuestosFGEM neutrosFGEM(BigDecimal tasaImpuestoFGEM) {
        BigDecimal ceroFGEM = BigDecimal.ZERO;
        return new SupuestosFGEM(ceroFGEM, ceroFGEM, ceroFGEM, ceroFGEM, ceroFGEM, ceroFGEM, tasaImpuestoFGEM);
    }

    public BigDecimal valorFGEM(VariableFGEM variableFGEM) {
        return switch (variableFGEM) {
            case PRECIO -> precioFGEM;
            case VOLUMEN -> volumenFGEM;
            case COSTO_UNITARIO -> costoUnitarioFGEM;
            case GASTOS_VARIABLES -> gastosVariablesFGEM;
            case COSTOS_FIJOS -> costosFijosFGEM;
        };
    }

    public SupuestosFGEM conFGEM(VariableFGEM variableFGEM, BigDecimal nuevoFGEM) {
        return new SupuestosFGEM(
                variableFGEM == VariableFGEM.PRECIO ? nuevoFGEM : precioFGEM,
                variableFGEM == VariableFGEM.VOLUMEN ? nuevoFGEM : volumenFGEM,
                variableFGEM == VariableFGEM.COSTO_UNITARIO ? nuevoFGEM : costoUnitarioFGEM,
                variableFGEM == VariableFGEM.GASTOS_VARIABLES ? nuevoFGEM : gastosVariablesFGEM,
                variableFGEM == VariableFGEM.COSTOS_FIJOS ? nuevoFGEM : costosFijosFGEM,
                costoFijoAdicionalFGEM, tasaImpuestoFGEM);
    }

    public SupuestosFGEM conCostoFijoAdicionalFGEM(BigDecimal montoFGEM) {
        return new SupuestosFGEM(precioFGEM, volumenFGEM, costoUnitarioFGEM, gastosVariablesFGEM, costosFijosFGEM, montoFGEM, tasaImpuestoFGEM);
    }

    public SupuestosFGEM conTasaImpuestoFGEM(BigDecimal tasaFGEM) {
        return new SupuestosFGEM(precioFGEM, volumenFGEM, costoUnitarioFGEM, gastosVariablesFGEM, costosFijosFGEM, costoFijoAdicionalFGEM, tasaFGEM);
    }
}
