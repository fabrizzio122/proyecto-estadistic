package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.MonedaFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MontosFGEM {

    public static final int ESCALA_FGEM = 4;

    private static final DecimalFormatSymbols SIMBOLOS_FGEM = DecimalFormatSymbols.getInstance(Locale.US);

    private MontosFGEM() {
    }

    public static long aEnteroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(ESCALA_FGEM, RoundingMode.HALF_UP).unscaledValue().longValueExact();
    }

    public static BigDecimal desdeEnteroFGEM(long valorFGEM) {
        return BigDecimal.valueOf(valorFGEM, ESCALA_FGEM);
    }

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

    public static String numeroFGEM(BigDecimal valorFGEM) {
        return formatoFGEM("#,##0.00##").format(valorFGEM);
    }

    public static String cantidadFGEM(BigDecimal valorFGEM) {
        return valorFGEM == null ? "—" : formatoFGEM("#,##0.####").format(valorFGEM);
    }

    public static String formatearFGEM(BigDecimal valorFGEM, MonedaFGEM monedaFGEM) {
        if (valorFGEM == null) {
            return "—";
        }
        return (valorFGEM.signum() < 0 ? "-" : "") + monedaFGEM.simboloFGEM() + " " + numeroFGEM(valorFGEM.abs());
    }

    public static String porcentajeFGEM(BigDecimal fraccionFGEM) {
        if (fraccionFGEM == null) {
            return "—";
        }
        return formatoFGEM("#,##0.0").format(fraccionFGEM.movePointRight(2)) + " %";
    }

    private static DecimalFormat formatoFGEM(String patronFGEM) {
        DecimalFormat formatoFGEM = new DecimalFormat(patronFGEM, SIMBOLOS_FGEM);
        formatoFGEM.setRoundingMode(RoundingMode.HALF_UP);
        return formatoFGEM;
    }

    public static String paraEditarFGEM(BigDecimal valorFGEM) {
        if (valorFGEM == null) {
            return "";
        }
        BigDecimal sinCerosFGEM = valorFGEM.stripTrailingZeros();
        return valorFGEM.setScale(Math.max(2, Math.max(0, sinCerosFGEM.scale())), RoundingMode.HALF_UP).toPlainString();
    }
}
