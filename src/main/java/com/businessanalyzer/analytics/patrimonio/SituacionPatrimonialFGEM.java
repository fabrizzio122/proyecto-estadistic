package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.PeriodoFGEM;

public record SituacionPatrimonialFGEM(BalanceGeneralFGEM balanceFGEM, IndicadoresPatrimonioFGEM indicadoresFGEM,
                                   PeriodoFGEM periodoResultadosFGEM) {
}
