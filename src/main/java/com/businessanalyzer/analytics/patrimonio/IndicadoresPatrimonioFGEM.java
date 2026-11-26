package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public static final BigDecimal LIQUIDEZ_VERDE_FGEM = new BigDecimal("1.5");
    public static final BigDecimal LIQUIDEZ_AMARILLO_FGEM = BigDecimal.ONE;
    public static final BigDecimal ENDEUDAMIENTO_VERDE_FGEM = new BigDecimal("0.50");
    public static final BigDecimal ENDEUDAMIENTO_AMARILLO_FGEM = new BigDecimal("0.70");
    public static final BigDecimal COBERTURA_VERDE_FGEM = new BigDecimal("1.3");

    private static final int ESCALA_FGEM = 6;

    public static IndicadoresPatrimonioFGEM deFGEM(BalanceGeneralFGEM bFGEM, EstadoResultadosFGEM resultadosFGEM, int mesesFGEM) {
        BigDecimal acFGEM = bFGEM.totalActivoCorrienteFGEM();
        BigDecimal pcFGEM = bFGEM.totalPasivoCorrienteFGEM();
        BigDecimal activoFGEM = bFGEM.totalActivoFGEM();
        BigDecimal pasivoFGEM = bFGEM.totalPasivoFGEM();
        BigDecimal patrimonioFGEM = bFGEM.patrimonioFGEM();
        boolean patrimonioPositivoFGEM = patrimonioFGEM.signum() > 0;
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

    public static String vecesFGEM(BigDecimal valorFGEM) {
        return valorFGEM == null ? "—" : valorFGEM.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
