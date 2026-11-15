package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.Periodo;

/**
 * Balance general a la fecha de corte y sus indicadores.
 *
 * @param periodoResultados meses completos con ventas usados para la rentabilidad y la capacidad
 *                          de pago; {@code null} si aún no hay ventas
 */
public record SituacionPatrimonial(BalanceGeneral balance, IndicadoresPatrimonio indicadores,
                                   Periodo periodoResultados) {
}
