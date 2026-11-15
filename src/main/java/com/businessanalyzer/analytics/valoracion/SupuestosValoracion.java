package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.exception.ValidacionException;

import java.math.BigDecimal;

/**
 * Supuestos de la valoración (fracciones: 0.20 = 20 %).
 *
 * @param tasaDescuento         rentabilidad anual que exigiría un inversionista por el riesgo del negocio
 * @param crecimiento           crecimiento anual de los flujos en los años proyectados
 * @param crecimientoLargoPlazo crecimiento anual después de la proyección (debe ser menor que la tasa)
 * @param anios                 años que se proyectan
 */
public record SupuestosValoracion(BigDecimal tasaDescuento, BigDecimal crecimiento, BigDecimal crecimientoLargoPlazo,
                                  int anios) {

    /** 20 %: costo de oportunidad habitual de una micro o pequeña empresa en el Perú. */
    public static final BigDecimal TASA_PREDETERMINADA = new BigDecimal("0.20");
    public static final BigDecimal CRECIMIENTO_LARGO_PLAZO = new BigDecimal("0.02");
    public static final int ANIOS = 5;

    public SupuestosValoracion {
        if (tasaDescuento == null || tasaDescuento.compareTo(new BigDecimal("0.05")) < 0
                || tasaDescuento.compareTo(new BigDecimal("0.60")) > 0) {
            throw new ValidacionException("La tasa de descuento debe estar entre 5 % y 60 %.");
        }
        if (crecimiento == null || crecimiento.compareTo(new BigDecimal("-0.30")) < 0
                || crecimiento.compareTo(new BigDecimal("0.50")) > 0) {
            throw new ValidacionException("El crecimiento anual debe estar entre −30 % y 50 %.");
        }
        if (crecimientoLargoPlazo == null || crecimientoLargoPlazo.compareTo(tasaDescuento) >= 0) {
            throw new ValidacionException("El crecimiento de largo plazo debe ser menor que la tasa de descuento.");
        }
        if (anios < 1 || anios > 10) {
            throw new ValidacionException("Se proyectan entre 1 y 10 años.");
        }
    }

    public static SupuestosValoracion con(BigDecimal tasa, BigDecimal crecimiento) {
        return new SupuestosValoracion(tasa, crecimiento, CRECIMIENTO_LARGO_PLAZO.min(tasa.subtract(new BigDecimal("0.01"))),
                ANIOS);
    }
}
