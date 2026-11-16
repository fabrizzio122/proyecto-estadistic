package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;

import java.math.BigDecimal;

/**
 * Supuestos de la valoración (fracciones: 0.20 = 20 %).
 *
 * @param tasaDescuentoFGEM         rentabilidad anual que exigiría un inversionista por el riesgo del negocio
 * @param crecimientoFGEM           crecimiento anual de los flujos en los años proyectados
 * @param crecimientoLargoPlazoFGEM crecimiento anual después de la proyección (debe ser menor que la tasa)
 * @param aniosFGEM                 años que se proyectan
 */
public record SupuestosValoracionFGEM(BigDecimal tasaDescuentoFGEM, BigDecimal crecimientoFGEM, BigDecimal crecimientoLargoPlazoFGEM,
                                  int aniosFGEM) {

    /** 20 %: costo de oportunidad habitual de una micro o pequeña empresa en el Perú. */
    public static final BigDecimal TASA_PREDETERMINADA_FGEM = new BigDecimal("0.20");
    public static final BigDecimal CRECIMIENTO_LARGO_PLAZO_FGEM = new BigDecimal("0.02");
    public static final int ANIOS_FGEM = 5;

    public SupuestosValoracionFGEM {
        if (tasaDescuentoFGEM == null || tasaDescuentoFGEM.compareTo(new BigDecimal("0.05")) < 0
                || tasaDescuentoFGEM.compareTo(new BigDecimal("0.60")) > 0) {
            throw new ValidacionExceptionFGEM("La tasa de descuento debe estar entre 5 % y 60 %.");
        }
        if (crecimientoFGEM == null || crecimientoFGEM.compareTo(new BigDecimal("-0.30")) < 0
                || crecimientoFGEM.compareTo(new BigDecimal("0.50")) > 0) {
            throw new ValidacionExceptionFGEM("El crecimiento anual debe estar entre −30 % y 50 %.");
        }
        if (crecimientoLargoPlazoFGEM == null || crecimientoLargoPlazoFGEM.compareTo(tasaDescuentoFGEM) >= 0) {
            throw new ValidacionExceptionFGEM("El crecimiento de largo plazo debe ser menor que la tasa de descuento.");
        }
        if (aniosFGEM < 1 || aniosFGEM > 10) {
            throw new ValidacionExceptionFGEM("Se proyectan entre 1 y 10 años.");
        }
    }

    public static SupuestosValoracionFGEM conFGEM(BigDecimal tasaFGEM, BigDecimal crecimientoFGEM) {
        return new SupuestosValoracionFGEM(tasaFGEM, crecimientoFGEM, CRECIMIENTO_LARGO_PLAZO_FGEM.min(tasaFGEM.subtract(new BigDecimal("0.01"))),
                ANIOS_FGEM);
    }
}
