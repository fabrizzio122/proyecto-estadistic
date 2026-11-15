package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.EstadoResultados;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ratios de liquidez, endeudamiento y rentabilidad que combinan el balance general con los
 * resultados de los últimos meses.
 *
 * @param liquidezCorriente     activo corriente ÷ pasivo corriente; {@code null} sin deudas de corto plazo
 * @param pruebaAcida           (activo corriente − inventario) ÷ pasivo corriente; {@code null} sin deudas de corto plazo
 * @param capitalTrabajo        activo corriente − pasivo corriente
 * @param endeudamiento         pasivo total ÷ activo total; {@code null} si no hay activos
 * @param apalancamiento        pasivo total ÷ patrimonio; {@code null} si el patrimonio no es positivo
 * @param utilidadAnual         utilidad neta llevada a 12 meses; {@code null} sin resultados
 * @param rentabilidadActivos   utilidad anual ÷ activo total (ROA)
 * @param rentabilidadPatrimonio utilidad anual ÷ patrimonio (ROE); {@code null} si el patrimonio no es positivo
 * @param utilidadOperativaMensual utilidad operativa promedio de un mes; {@code null} sin resultados
 * @param coberturaCuotas       utilidad operativa mensual ÷ cuotas mensuales; {@code null} sin cuotas
 * @param mesesResultados       meses usados para los resultados (0 si no hay ventas)
 */
public record IndicadoresPatrimonio(BigDecimal liquidezCorriente,
                                    BigDecimal pruebaAcida,
                                    BigDecimal capitalTrabajo,
                                    BigDecimal endeudamiento,
                                    BigDecimal apalancamiento,
                                    BigDecimal utilidadAnual,
                                    BigDecimal rentabilidadActivos,
                                    BigDecimal rentabilidadPatrimonio,
                                    BigDecimal utilidadOperativaMensual,
                                    BigDecimal coberturaCuotas,
                                    int mesesResultados) {

    /** Liquidez corriente desde la cual se considera holgada. */
    public static final BigDecimal LIQUIDEZ_VERDE = new BigDecimal("1.5");
    /** Por debajo de 1 las deudas de corto plazo superan a lo que se puede cobrar o vender. */
    public static final BigDecimal LIQUIDEZ_AMARILLO = BigDecimal.ONE;
    /** Endeudamiento (pasivo ÷ activo) hasta el cual se considera sano. */
    public static final BigDecimal ENDEUDAMIENTO_VERDE = new BigDecimal("0.50");
    /** Endeudamiento por encima del cual se considera riesgoso. */
    public static final BigDecimal ENDEUDAMIENTO_AMARILLO = new BigDecimal("0.70");
    /** Cobertura de cuotas mínima recomendada (la utilidad operativa supera la cuota en 30 %). */
    public static final BigDecimal COBERTURA_VERDE = new BigDecimal("1.3");

    private static final int ESCALA = 6;

    /**
     * @param resultados estado de resultados de los últimos {@code meses} meses; {@code null} o
     *                   {@code meses = 0} si no hay ventas registradas
     */
    public static IndicadoresPatrimonio de(BalanceGeneral b, EstadoResultados resultados, int meses) {
        BigDecimal ac = b.totalActivoCorriente();
        BigDecimal pc = b.totalPasivoCorriente();
        BigDecimal activo = b.totalActivo();
        BigDecimal pasivo = b.totalPasivo();
        BigDecimal patrimonio = b.patrimonio();
        boolean patrimonioPositivo = patrimonio.signum() > 0;

        BigDecimal liquidez = dividir(ac, pc);
        BigDecimal acida = dividir(ac.subtract(b.inventario()), pc);
        BigDecimal endeudamiento = dividir(pasivo, activo);
        BigDecimal apalancamiento = patrimonioPositivo ? dividir(pasivo, patrimonio) : null;

        BigDecimal anual = null;
        BigDecimal operativaMensual = null;
        if (resultados != null && meses > 0) {
            BigDecimal m = BigDecimal.valueOf(meses);
            anual = resultados.utilidadNeta().multiply(BigDecimal.valueOf(12)).divide(m, 2, RoundingMode.HALF_UP);
            operativaMensual = resultados.utilidadOperativa().divide(m, 2, RoundingMode.HALF_UP);
        }
        BigDecimal roa = anual == null ? null : dividir(anual, activo);
        BigDecimal roe = anual == null || !patrimonioPositivo ? null : dividir(anual, patrimonio);
        BigDecimal cobertura = operativaMensual == null ? null : dividir(operativaMensual, b.cuotasMensuales());

        return new IndicadoresPatrimonio(liquidez, acida, ac.subtract(pc), endeudamiento, apalancamiento,
                anual, roa, roe, operativaMensual, cobertura, resultados == null ? 0 : meses);
    }

    private static BigDecimal dividir(BigDecimal a, BigDecimal b) {
        return b == null || b.signum() == 0 ? null : a.divide(b, ESCALA, RoundingMode.HALF_UP);
    }

    /** "1.85" veces, con dos decimales. */
    public static String veces(BigDecimal valor) {
        return valor == null ? "—" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
