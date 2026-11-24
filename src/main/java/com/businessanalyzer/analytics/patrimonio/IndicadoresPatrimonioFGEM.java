package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ratios de liquidez, endeudamiento y rentabilidad que combinan el balance general con los
 * resultados de los últimos meses.
 *
 * @param liquidezCorrienteFGEM     activo corriente ÷ pasivo corriente; {@code null} sin deudas de corto plazo
 * @param pruebaAcidaFGEM           (activo corriente − inventario) ÷ pasivo corriente; {@code null} sin deudas de corto plazo
 * @param capitalTrabajoFGEM        activo corriente − pasivo corriente
 * @param endeudamientoFGEM         pasivo total ÷ activo total; {@code null} si el activo no es positivo
 * @param apalancamientoFGEM        pasivo total ÷ patrimonio; {@code null} si el patrimonio no es positivo
 * @param utilidadAnualFGEM         utilidad neta llevada a 12 meses; {@code null} sin resultados
 * @param rentabilidadActivosFGEM   utilidad anual ÷ activo total (ROA); {@code null} si el activo no es positivo
 * @param rentabilidadPatrimonioFGEM utilidad anual ÷ patrimonio (ROE); {@code null} si el patrimonio no es positivo
 * @param utilidadOperativaMensualFGEM utilidad operativa promedio de un mes; {@code null} sin resultados
 * @param coberturaCuotasFGEM       utilidad operativa mensual ÷ cuotas mensuales; {@code null} sin cuotas
 * @param mesesResultadosFGEM       meses usados para los resultados (0 si no hay ventas)
 */
public record IndicadoresPatrimonioFGEM(BigDecimal liquidezCorrienteFGEM,
                                    BigDecimal pruebaAcidaFGEM,
                                    BigDecimal capitalTrabajoFGEM,
                                    BigDecimal endeudamientoFGEM,
                                    BigDecimal apalancamientoFGEM,
                                    BigDecimal utilidadAnualFGEM,
                                    BigDecimal rentabilidadActivosFGEM,
                                    BigDecimal rentabilidadPatrimonioFGEM,
                                    BigDecimal utilidadOperativaMensualFGEM,
                                    BigDecimal coberturaCuotasFGEM,
                                    int mesesResultadosFGEM) {

    /** Liquidez corriente desde la cual se considera holgada. */
    public static final BigDecimal LIQUIDEZ_VERDE_FGEM = new BigDecimal("1.5");
    /** Por debajo de 1 las deudas de corto plazo superan a lo que se puede cobrar o vender. */
    public static final BigDecimal LIQUIDEZ_AMARILLO_FGEM = BigDecimal.ONE;
    /** Endeudamiento (pasivo ÷ activo) hasta el cual se considera sano. */
    public static final BigDecimal ENDEUDAMIENTO_VERDE_FGEM = new BigDecimal("0.50");
    /** Endeudamiento por encima del cual se considera riesgoso. */
    public static final BigDecimal ENDEUDAMIENTO_AMARILLO_FGEM = new BigDecimal("0.70");
    /** Cobertura de cuotas mínima recomendada (la utilidad operativa supera la cuota en 30 %). */
    public static final BigDecimal COBERTURA_VERDE_FGEM = new BigDecimal("1.3");

    private static final int ESCALA_FGEM = 6;

    /**
     * @param resultadosFGEM estado de resultados de los últimos {@code meses} meses; {@code null} o
     *                   {@code meses = 0} si no hay ventas registradas
     */
    public static IndicadoresPatrimonioFGEM deFGEM(BalanceGeneralFGEM bFGEM, EstadoResultadosFGEM resultadosFGEM, int mesesFGEM) {
        BigDecimal acFGEM = bFGEM.totalActivoCorrienteFGEM();
        BigDecimal pcFGEM = bFGEM.totalPasivoCorrienteFGEM();
        BigDecimal activoFGEM = bFGEM.totalActivoFGEM();
        BigDecimal pasivoFGEM = bFGEM.totalPasivoFGEM();
        BigDecimal patrimonioFGEM = bFGEM.patrimonioFGEM();
        boolean patrimonioPositivoFGEM = patrimonioFGEM.signum() > 0;
        // Con activos en cero o negativos (por ejemplo, caja negativa porque falta el aporte inicial) dividir entre el
        // activo no tiene sentido: una pérdida ÷ activos negativos daría una rentabilidad positiva.
        boolean activoPositivoFGEM = activoFGEM.signum() > 0;

        BigDecimal liquidezFGEM = dividirFGEM(acFGEM, pcFGEM);
        BigDecimal acidaFGEM = dividirFGEM(acFGEM.subtract(bFGEM.inventarioFGEM()), pcFGEM);
        BigDecimal endeudamientoFGEM = activoPositivoFGEM ? dividirFGEM(pasivoFGEM, activoFGEM) : null;
        BigDecimal apalancamientoFGEM = patrimonioPositivoFGEM ? dividirFGEM(pasivoFGEM, patrimonioFGEM) : null;

        BigDecimal anualFGEM = null;
        BigDecimal operativaMensualFGEM = null;
        if (resultadosFGEM != null && mesesFGEM > 0) {
            BigDecimal mFGEM = BigDecimal.valueOf(mesesFGEM);
            anualFGEM = resultadosFGEM.utilidadNetaFGEM().multiply(BigDecimal.valueOf(12)).divide(mFGEM, 2, RoundingMode.HALF_UP);
            operativaMensualFGEM = resultadosFGEM.utilidadOperativaFGEM().divide(mFGEM, 2, RoundingMode.HALF_UP);
        }
        BigDecimal roaFGEM = anualFGEM == null || !activoPositivoFGEM ? null : dividirFGEM(anualFGEM, activoFGEM);
        BigDecimal roeFGEM = anualFGEM == null || !patrimonioPositivoFGEM ? null : dividirFGEM(anualFGEM, patrimonioFGEM);
        BigDecimal coberturaFGEM = operativaMensualFGEM == null ? null : dividirFGEM(operativaMensualFGEM, bFGEM.cuotasMensualesFGEM());

        return new IndicadoresPatrimonioFGEM(liquidezFGEM, acidaFGEM, acFGEM.subtract(pcFGEM), endeudamientoFGEM, apalancamientoFGEM,
                anualFGEM, roaFGEM, roeFGEM, operativaMensualFGEM, coberturaFGEM, resultadosFGEM == null ? 0 : mesesFGEM);
    }

    private static BigDecimal dividirFGEM(BigDecimal aFGEM, BigDecimal bFGEM) {
        return bFGEM == null || bFGEM.signum() == 0 ? null : aFGEM.divide(bFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    /** "1.85" veces, con dos decimales. */
    public static String vecesFGEM(BigDecimal valorFGEM) {
        return valorFGEM == null ? "—" : valorFGEM.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
