package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.model.ActivoFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Depreciación en línea recta, por meses completos: el bien pierde cada mes la misma parte de
 * (valor − valor residual) hasta completar su vida útil.
 *
 * @param mesesTranscurridosFGEM meses completos desde la adquisición hasta la fecha de corte
 * @param acumuladaFGEM          depreciación acumulada a la fecha de corte
 * @param mensualFGEM            depreciación de un mes mientras dure la vida útil (0 si ya terminó)
 * @param valorEnLibrosFGEM      valor − depreciación acumulada
 */
public record DepreciacionFGEM(long mesesTranscurridosFGEM, BigDecimal acumuladaFGEM, BigDecimal mensualFGEM,
                           BigDecimal valorEnLibrosFGEM) {

    /** Montos en céntimos: la depreciación se redondea a 2 decimales. */
    private static final int ESCALA_FGEM = 2;

    public static DepreciacionFGEM deFGEM(ActivoFGEM activoFGEM, LocalDate corteFGEM) {
        BigDecimal valorFGEM = activoFGEM.valorFGEM();
        if (!activoFGEM.seDepreciaFGEM() || corteFGEM.isBefore(activoFGEM.fechaFGEM())) {
            return new DepreciacionFGEM(0, BigDecimal.ZERO, BigDecimal.ZERO, valorFGEM);
        }
        long mesesFGEM = ChronoUnit.MONTHS.between(activoFGEM.fechaFGEM(), corteFGEM);
        long vidaMesesFGEM = activoFGEM.vidaUtilAniosFGEM() * 12L;
        BigDecimal depreciableFGEM = valorFGEM.subtract(activoFGEM.valorResidualFGEM()).max(BigDecimal.ZERO);
        BigDecimal mensualFGEM = depreciableFGEM.divide(BigDecimal.valueOf(vidaMesesFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
        BigDecimal acumuladaFGEM = mesesFGEM >= vidaMesesFGEM
                ? depreciableFGEM
                : depreciableFGEM.multiply(BigDecimal.valueOf(mesesFGEM))
                .divide(BigDecimal.valueOf(vidaMesesFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
        return new DepreciacionFGEM(mesesFGEM, acumuladaFGEM, mesesFGEM >= vidaMesesFGEM ? BigDecimal.ZERO : mensualFGEM,
                valorFGEM.subtract(acumuladaFGEM));
    }

    /** Parte de la vida útil ya consumida (0 a 1); {@code null} si no se deprecia. */
    public static BigDecimal avanceFGEM(ActivoFGEM activoFGEM, LocalDate corteFGEM) {
        if (!activoFGEM.seDepreciaFGEM()) {
            return null;
        }
        DepreciacionFGEM dFGEM = deFGEM(activoFGEM, corteFGEM);
        long vidaMesesFGEM = activoFGEM.vidaUtilAniosFGEM() * 12L;
        return BigDecimal.valueOf(Math.min(dFGEM.mesesTranscurridosFGEM(), vidaMesesFGEM))
                .divide(BigDecimal.valueOf(vidaMesesFGEM), 6, RoundingMode.HALF_UP);
    }
}
