package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.model.Activo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Depreciación en línea recta, por meses completos: el bien pierde cada mes la misma parte de
 * (valor − valor residual) hasta completar su vida útil.
 *
 * @param mesesTranscurridos meses completos desde la adquisición hasta la fecha de corte
 * @param acumulada          depreciación acumulada a la fecha de corte
 * @param mensual            depreciación de un mes mientras dure la vida útil (0 si ya terminó)
 * @param valorEnLibros      valor − depreciación acumulada
 */
public record Depreciacion(long mesesTranscurridos, BigDecimal acumulada, BigDecimal mensual,
                           BigDecimal valorEnLibros) {

    /** Montos en céntimos: la depreciación se redondea a 2 decimales. */
    private static final int ESCALA = 2;

    public static Depreciacion de(Activo activo, LocalDate corte) {
        BigDecimal valor = activo.valor();
        if (!activo.seDeprecia() || corte.isBefore(activo.fecha())) {
            return new Depreciacion(0, BigDecimal.ZERO, BigDecimal.ZERO, valor);
        }
        long meses = ChronoUnit.MONTHS.between(activo.fecha(), corte);
        long vidaMeses = activo.vidaUtilAnios() * 12L;
        BigDecimal depreciable = valor.subtract(activo.valorResidual()).max(BigDecimal.ZERO);
        BigDecimal mensual = depreciable.divide(BigDecimal.valueOf(vidaMeses), ESCALA, RoundingMode.HALF_UP);
        BigDecimal acumulada = meses >= vidaMeses
                ? depreciable
                : depreciable.multiply(BigDecimal.valueOf(meses))
                .divide(BigDecimal.valueOf(vidaMeses), ESCALA, RoundingMode.HALF_UP);
        return new Depreciacion(meses, acumulada, meses >= vidaMeses ? BigDecimal.ZERO : mensual,
                valor.subtract(acumulada));
    }

    /** Parte de la vida útil ya consumida (0 a 1); {@code null} si no se deprecia. */
    public static BigDecimal avance(Activo activo, LocalDate corte) {
        if (!activo.seDeprecia()) {
            return null;
        }
        Depreciacion d = de(activo, corte);
        long vidaMeses = activo.vidaUtilAnios() * 12L;
        return BigDecimal.valueOf(Math.min(d.mesesTranscurridos(), vidaMeses))
                .divide(BigDecimal.valueOf(vidaMeses), 6, RoundingMode.HALF_UP);
    }
}
