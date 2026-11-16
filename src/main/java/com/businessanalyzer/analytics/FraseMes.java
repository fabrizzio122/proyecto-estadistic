package com.businessanalyzer.analytics;

import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Resume el mes en curso en una frase en lenguaje simple, por ejemplo:
 * «Vas 11.5 % arriba de octubre y ya cubriste tus costos del mes más de dos veces.»
 * Se arma por partes para que la pantalla pinte en verde lo positivo y en coral lo que preocupa.
 */
public record FraseMes(List<Parte> partes) {

    public enum Tono { NORMAL, POSITIVO, NEGATIVO }

    public record Parte(String texto, Tono tono) {
    }

    /** Variaciones menores a 1 % se consideran «igual». */
    private static final BigDecimal UMBRAL = new BigDecimal("0.01");
    private static final String[] VECES = {"", "", "dos", "tres", "cuatro", "cinco"};

    public FraseMes {
        partes = List.copyOf(partes);
    }

    /**
     * @param ventas         ventas del mes hasta hoy
     * @param ventasAnterior ventas de los mismos días del mes anterior
     * @param mesAnterior    nombre del mes anterior en minúsculas («octubre»)
     * @param cobertura      ventas ÷ punto de equilibrio; {@code null} si las ventas no cubren sus costos variables
     * @param faltante       lo que falta vender para el punto de equilibrio, ya formateado («S/ 1,200.00»)
     */
    public static FraseMes de(BigDecimal ventas, BigDecimal ventasAnterior, String mesAnterior, BigDecimal cobertura,
                              String faltante) {
        List<Parte> partes = new ArrayList<>();
        if (ventas == null || ventas.signum() <= 0) {
            partes.add(normal("Aún no hay ventas registradas este mes."));
            return new FraseMes(partes);
        }
        if (ventasAnterior == null || ventasAnterior.signum() <= 0) {
            partes.add(normal("Ya empezaste a vender este mes"));
        } else {
            BigDecimal variacion = ventas.subtract(ventasAnterior).divide(ventasAnterior, 6, RoundingMode.HALF_UP);
            if (variacion.compareTo(UMBRAL) >= 0) {
                partes.add(normal("Vas "));
                partes.add(new Parte(Montos.porcentaje(variacion) + " arriba", Tono.POSITIVO));
                partes.add(normal(" de " + mesAnterior));
            } else if (variacion.compareTo(UMBRAL.negate()) <= 0) {
                partes.add(normal("Vas "));
                partes.add(new Parte(Montos.porcentaje(variacion.abs()) + " abajo", Tono.NEGATIVO));
                partes.add(normal(" de " + mesAnterior));
            } else {
                partes.add(normal("Vas igual que en " + mesAnterior));
            }
        }
        if (cobertura == null) {
            partes.add(normal(", pero "));
            partes.add(new Parte("tus ventas aún no cubren sus costos", Tono.NEGATIVO));
            partes.add(normal("."));
        } else if (cobertura.compareTo(BigDecimal.ONE) < 0) {
            partes.add(normal(" y te faltan "));
            partes.add(new Parte(faltante, Tono.NEGATIVO));
            partes.add(normal(" para cubrir tus costos del mes."));
        } else {
            int veces = cobertura.setScale(0, RoundingMode.FLOOR).intValue();
            partes.add(normal(" y ya cubriste tus costos del mes"));
            partes.add(normal(veces >= 2 ? " más de " + (veces < VECES.length ? VECES[veces] : String.valueOf(veces))
                    + " veces." : "."));
        }
        return new FraseMes(partes);
    }

    /** La frase completa, sin tonos. */
    public String texto() {
        StringBuilder sb = new StringBuilder();
        partes.forEach(p -> sb.append(p.texto()));
        return sb.toString();
    }

    private static Parte normal(String texto) {
        return new Parte(texto, Tono.NORMAL);
    }
}
