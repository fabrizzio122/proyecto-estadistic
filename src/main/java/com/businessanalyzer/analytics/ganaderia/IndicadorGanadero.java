package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.diagnostico.Semaforo;

/**
 * Indicador técnico del hato ya evaluado.
 *
 * @param valor    valor formateado ("72.0 %", "6.4 l")
 * @param criterio umbrales de referencia; {@code null} si es informativo
 * @param lectura  qué significa en palabras sencillas
 */
public record IndicadorGanadero(String nombre, String valor, Semaforo semaforo, String criterio, String lectura) {
}
