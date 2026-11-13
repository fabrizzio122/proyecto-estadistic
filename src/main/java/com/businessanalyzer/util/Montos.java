package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Moneda;

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
public final class Montos {

    public static final int ESCALA = 4;

    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.US);

    private Montos() {
    }

    // ------------------------------------------------------------------ base de datos

    public static long aEntero(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP).unscaledValue().longValueExact();
    }

    public static BigDecimal desdeEntero(long valor) {
        return BigDecimal.valueOf(valor, ESCALA);
    }

    // ------------------------------------------------------------------ lectura de formularios

    /**
     * Interpreta un monto escrito por el usuario. El separador decimal es el punto; también
     * se aceptan separadores de miles ({@code 1,250.50}) y la coma como decimal ({@code 12,5}).
     */
    public static BigDecimal leer(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
        String limpio = texto.strip().replace(" ", "").replaceAll("^[^0-9.,-]+", "");
        if (limpio.matches("-?\\d{1,3}(,\\d{3})+(\\.\\d+)?")) {
            limpio = limpio.replace(",", "");
        } else if (limpio.matches("-?\\d+,\\d+")) {
            limpio = limpio.replace(',', '.');
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo \"" + campo + "\" debe ser un número, por ejemplo 1250.50");
        }
    }

    // ------------------------------------------------------------------ presentación

    /** {@code 1234.5} → {@code 1,234.50}; muestra hasta 4 decimales si los tiene. */
    public static String numero(BigDecimal valor) {
        return formato("#,##0.00##").format(valor);
    }

    /** {@code S/ 1,234.50}; los negativos se muestran como {@code -S/ 5.00}. */
    public static String formatear(BigDecimal valor, Moneda moneda) {
        if (valor == null) {
            return "—";
        }
        return (valor.signum() < 0 ? "-" : "") + moneda.simbolo() + " " + numero(valor.abs());
    }

    /** Fracción a porcentaje: {@code 0.255} → {@code 25.5 %}. */
    public static String porcentaje(BigDecimal fraccion) {
        if (fraccion == null) {
            return "—";
        }
        return formato("#,##0.0").format(fraccion.movePointRight(2)) + " %";
    }

    /** DecimalFormat no es seguro entre hilos: se crea uno por uso. Redondeo comercial (HALF_UP). */
    private static DecimalFormat formato(String patron) {
        DecimalFormat formato = new DecimalFormat(patron, SIMBOLOS);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato;
    }

    /** Texto para un campo de edición: sin separador de miles y con al menos 2 decimales. */
    public static String paraEditar(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return valor.setScale(Math.max(2, Math.max(0, sinCeros.scale())), RoundingMode.HALF_UP).toPlainString();
    }
}
