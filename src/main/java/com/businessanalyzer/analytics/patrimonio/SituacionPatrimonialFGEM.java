package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.PeriodoFGEM;

/**
 * Balance general a la fecha de corte y sus indicadores.
 *
 * @param periodoResultadosFGEM meses completos con ventas usados para la rentabilidad y la capacidad
 *                          de pago; {@code null} si aún no hay ventas
 */
public record SituacionPatrimonialFGEM(BalanceGeneralFGEM balanceFGEM, IndicadoresPatrimonioFGEM indicadoresFGEM,
                                   PeriodoFGEM periodoResultadosFGEM) {
}
