package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;

/**
 * Indicador técnico del hato ya evaluado.
 *
 * @param valorFGEM    valor formateado ("72.0 %", "6.4 l")
 * @param criterioFGEM umbrales de referencia; {@code null} si es informativo
 * @param lecturaFGEM  qué significa en palabras sencillas
 */
public record IndicadorGanaderoFGEM(String nombreFGEM, String valorFGEM, SemaforoFGEM semaforoFGEM, String criterioFGEM, String lecturaFGEM) {
}
