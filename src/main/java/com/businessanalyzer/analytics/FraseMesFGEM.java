package com.businessanalyzer.analytics;

import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Resume el mes en curso en una frase en lenguaje simple, por ejemplo:
 * «Vas 11.5 % arriba de octubre y ya cubriste tus costos del mes más de dos veces.»
 * Se arma por partes para que la pantalla pinte en verde lo positivo y en coral lo que preocupa.
 */
public record FraseMesFGEM(List<ParteFGEM> partesFGEM) {

    public enum TonoFGEM { NORMAL, POSITIVO, NEGATIVO }

    public record ParteFGEM(String textoFGEM, TonoFGEM tonoFGEM) {
    }

    /** Variaciones menores a 1 % se consideran «igual». */
    private static final BigDecimal UMBRAL_FGEM = new BigDecimal("0.01");
    private static final String[] VECES_FGEM = {"", "", "dos", "tres", "cuatro", "cinco"};

    public FraseMesFGEM {
        partesFGEM = List.copyOf(partesFGEM);
    }

    /**
     * @param ventasFGEM         ventas del mes hasta hoy
     * @param ventasAnteriorFGEM ventas de los mismos días del mes anterior
     * @param mesAnteriorFGEM    nombre del mes anterior en minúsculas («octubre»)
     * @param coberturaFGEM      ventas ÷ punto de equilibrio; {@code null} si las ventas no cubren sus costos variables
     * @param faltanteFGEM       lo que falta vender para el punto de equilibrio, ya formateado («S/ 1,200.00»)
     */
    public static FraseMesFGEM deFGEM(BigDecimal ventasFGEM, BigDecimal ventasAnteriorFGEM, String mesAnteriorFGEM, BigDecimal coberturaFGEM,
                              String faltanteFGEM) {
        List<ParteFGEM> partesFGEM = new ArrayList<>();
        if (ventasFGEM == null || ventasFGEM.signum() <= 0) {
            partesFGEM.add(normalFGEM("Aún no hay ventas registradas este mes."));
            return new FraseMesFGEM(partesFGEM);
        }
        if (ventasAnteriorFGEM == null || ventasAnteriorFGEM.signum() <= 0) {
            partesFGEM.add(normalFGEM("Ya empezaste a vender este mes"));
        } else {
            BigDecimal variacionFGEM = ventasFGEM.subtract(ventasAnteriorFGEM).divide(ventasAnteriorFGEM, 6, RoundingMode.HALF_UP);
            if (variacionFGEM.compareTo(UMBRAL_FGEM) >= 0) {
                partesFGEM.add(normalFGEM("Vas "));
                partesFGEM.add(new ParteFGEM(MontosFGEM.porcentajeFGEM(variacionFGEM) + " arriba", TonoFGEM.POSITIVO));
                partesFGEM.add(normalFGEM(" de " + mesAnteriorFGEM));
            } else if (variacionFGEM.compareTo(UMBRAL_FGEM.negate()) <= 0) {
                partesFGEM.add(normalFGEM("Vas "));
                partesFGEM.add(new ParteFGEM(MontosFGEM.porcentajeFGEM(variacionFGEM.abs()) + " abajo", TonoFGEM.NEGATIVO));
                partesFGEM.add(normalFGEM(" de " + mesAnteriorFGEM));
            } else {
                partesFGEM.add(normalFGEM("Vas igual que en " + mesAnteriorFGEM));
            }
        }
        if (coberturaFGEM == null) {
            partesFGEM.add(normalFGEM(", pero "));
            partesFGEM.add(new ParteFGEM("tus ventas aún no cubren sus costos", TonoFGEM.NEGATIVO));
            partesFGEM.add(normalFGEM("."));
        } else if (coberturaFGEM.compareTo(BigDecimal.ONE) < 0) {
            partesFGEM.add(normalFGEM(" y te faltan "));
            partesFGEM.add(new ParteFGEM(faltanteFGEM, TonoFGEM.NEGATIVO));
            partesFGEM.add(normalFGEM(" para cubrir tus costos del mes."));
        } else {
            int vecesFGEM = coberturaFGEM.setScale(0, RoundingMode.FLOOR).intValue();
            partesFGEM.add(normalFGEM(" y ya cubriste tus costos del mes"));
            partesFGEM.add(normalFGEM(vecesFGEM >= 2 ? " más de " + (vecesFGEM < VECES_FGEM.length ? VECES_FGEM[vecesFGEM] : String.valueOf(vecesFGEM))
                    + " veces." : "."));
        }
        return new FraseMesFGEM(partesFGEM);
    }

    /** La frase completa, sin tonos. */
    public String textoFGEM() {
        StringBuilder sbFGEM = new StringBuilder();
        partesFGEM.forEach(pFGEM -> sbFGEM.append(pFGEM.textoFGEM()));
        return sbFGEM.toString();
    }

    private static ParteFGEM normalFGEM(String textoFGEM) {
        return new ParteFGEM(textoFGEM, TonoFGEM.NORMAL);
    }
}
