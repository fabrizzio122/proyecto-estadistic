package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.MonedaFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Manejo de montos de dinero con {@link BigDecimal} (nunca {@code double}).
 * <p>
 * En la base de datos se guardan como enteros en diezmilésimas ({@value #ESCALA} decimales):
 * {@code 12.5} se guarda como {@code 125000}.
 */
public final class MontosFGEM {

    public static final int ESCALA_FGEM = 4;

    private static final DecimalFormatSymbols SIMBOLOS_FGEM = DecimalFormatSymbols.getInstance(Locale.US);

    private MontosFGEM() {
    }

    // ------------------------------------------------------------------ base de datos

    public static long aEnteroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(ESCALA_FGEM, RoundingMode.HALF_UP).unscaledValue().longValueExact();
    }

    public static BigDecimal desdeEnteroFGEM(long valorFGEM) {
        return BigDecimal.valueOf(valorFGEM, ESCALA_FGEM);
    }

    // ------------------------------------------------------------------ lectura de formularios

    /**
     * Interpreta un monto escrito por el usuario. El separador decimal es el punto; también
     * se aceptan separadores de miles ({@code 1,250.50}) y la coma como decimal ({@code 12,5}).
     */
    public static BigDecimal leerFGEM(String textoFGEM, String campoFGEM) {
        if (textoFGEM == null || textoFGEM.isBlank()) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" es obligatorio.");
        }
        String limpioFGEM = textoFGEM.strip().replace(" ", "").replaceAll("^[^0-9.,-]+", "");
        if (limpioFGEM.matches("-?\\d{1,3}(,\\d{3})+(\\.\\d+)?")) {
            limpioFGEM = limpioFGEM.replace(",", "");
        } else if (limpioFGEM.matches("-?\\d+,\\d+")) {
            limpioFGEM = limpioFGEM.replace(',', '.');
        }
        try {
            return new BigDecimal(limpioFGEM);
        } catch (NumberFormatException eFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe ser un número, por ejemplo 1250.50");
        }
    }

    // ------------------------------------------------------------------ presentación

    /** {@code 1234.5} → {@code 1,234.50}; muestra hasta 4 decimales si los tiene. */
    public static String numeroFGEM(BigDecimal valorFGEM) {
        return formatoFGEM("#,##0.00##").format(valorFGEM);
    }

    /** Cantidades: sin decimales innecesarios ({@code 3}, {@code 2.5}, {@code 1,200}). */
    public static String cantidadFGEM(BigDecimal valorFGEM) {
        return valorFGEM == null ? "—" : formatoFGEM("#,##0.####").format(valorFGEM);
    }

    /** {@code S/ 1,234.50}; los negativos se muestran como {@code -S/ 5.00}. */
    public static String formatearFGEM(BigDecimal valorFGEM, MonedaFGEM monedaFGEM) {
        if (valorFGEM == null) {
            return "—";
        }
        return (valorFGEM.signum() < 0 ? "-" : "") + monedaFGEM.simboloFGEM() + " " + numeroFGEM(valorFGEM.abs());
    }

    /** Fracción a porcentaje: {@code 0.255} → {@code 25.5 %}. */
    public static String porcentajeFGEM(BigDecimal fraccionFGEM) {
        if (fraccionFGEM == null) {
            return "—";
        }
        return formatoFGEM("#,##0.0").format(fraccionFGEM.movePointRight(2)) + " %";
    }

    /** DecimalFormat no es seguro entre hilos: se crea uno por uso. Redondeo comercial (HALF_UP). */
    private static DecimalFormat formatoFGEM(String patronFGEM) {
        DecimalFormat formatoFGEM = new DecimalFormat(patronFGEM, SIMBOLOS_FGEM);
        formatoFGEM.setRoundingMode(RoundingMode.HALF_UP);
        return formatoFGEM;
    }

    /** Texto para un campo de edición: sin separador de miles y con al menos 2 decimales. */
    public static String paraEditarFGEM(BigDecimal valorFGEM) {
        if (valorFGEM == null) {
            return "";
        }
        BigDecimal sinCerosFGEM = valorFGEM.stripTrailingZeros();
        return valorFGEM.setScale(Math.max(2, Math.max(0, sinCerosFGEM.scale())), RoundingMode.HALF_UP).toPlainString();
    }
}
