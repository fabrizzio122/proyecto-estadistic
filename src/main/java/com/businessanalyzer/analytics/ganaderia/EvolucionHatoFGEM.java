package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Evolución mensual del hato: animales al cierre de cada mes por especie, nacimientos, muertes y
 * ventas de cada mes, y producción mensual de leche y fibra con su tendencia (regresión lineal).
 *
 * @param existenciasFGEM animales al cierre de cada mes, por especie (solo especies con animales)
 * @param produccionFGEM  producción de cada mes, por producto (solo productos registrados)
 */
public record EvolucionHatoFGEM(List<YearMonth> mesesFGEM, Map<EspecieFGEM, int[]> existenciasFGEM, int[] nacimientosFGEM,
                            int[] muertesFGEM, int[] salidasFGEM, Map<ProductoGanaderoFGEM, double[]> produccionFGEM) {

    /** Meses con datos necesarios para hablar de tendencia. */
    public static final int MINIMO_MESES_TENDENCIA_FGEM = 6;

    public static EvolucionHatoFGEM calcularFGEM(YearMonth desdeFGEM, YearMonth hastaFGEM, Collection<MovimientoGanadoFGEM> movimientosFGEM,
                                         Collection<ProduccionGanaderaFGEM> registrosFGEM) {
        List<YearMonth> mesesFGEM = new ArrayList<>();
        for (YearMonth mFGEM = desdeFGEM; !mFGEM.isAfter(hastaFGEM); mFGEM = mFGEM.plusMonths(1)) {
            mesesFGEM.add(mFGEM);
        }
        int nFGEM = mesesFGEM.size();
        Map<EspecieFGEM, int[]> existenciasFGEM = new EnumMap<>(EspecieFGEM.class);
        int[] nacimientosFGEM = new int[nFGEM];
        int[] muertesFGEM = new int[nFGEM];
        int[] salidasFGEM = new int[nFGEM];
        for (int iFGEM = 0; iFGEM < nFGEM; iFGEM++) {
            Map<CategoriaGanadoFGEM, Integer> alCierreFGEM = HatoFGEM.existenciasFGEM(movimientosFGEM, mesesFGEM.get(iFGEM).atEndOfMonth());
            for (Map.Entry<CategoriaGanadoFGEM, Integer> eFGEM : alCierreFGEM.entrySet()) {
                existenciasFGEM.computeIfAbsent(eFGEM.getKey().especieFGEM(), kFGEM -> new int[nFGEM])[iFGEM] += eFGEM.getValue();
            }
        }
        for (MovimientoGanadoFGEM mFGEM : movimientosFGEM) {
            int iFGEM = mesesFGEM.indexOf(YearMonth.from(mFGEM.fechaFGEM()));
            if (mFGEM.anuladoFGEM() || iFGEM < 0) {
                continue;
            }
            switch (mFGEM.tipoFGEM()) {
                case NACIMIENTO -> nacimientosFGEM[iFGEM] += mFGEM.cantidadFGEM();
                case MUERTE -> muertesFGEM[iFGEM] += mFGEM.cantidadFGEM();
                case VENTA, CONSUMO -> salidasFGEM[iFGEM] += mFGEM.cantidadFGEM();
                default -> {
                }
            }
        }
        Map<ProductoGanaderoFGEM, double[]> produccionFGEM = new EnumMap<>(ProductoGanaderoFGEM.class);
        for (ProduccionGanaderaFGEM pFGEM : registrosFGEM) {
            int iFGEM = mesesFGEM.indexOf(YearMonth.from(pFGEM.fechaFGEM()));
            if (!pFGEM.anuladoFGEM() && iFGEM >= 0) {
                produccionFGEM.computeIfAbsent(pFGEM.productoFGEM(), kFGEM -> new double[nFGEM])[iFGEM] += pFGEM.cantidadFGEM().doubleValue();
            }
        }
        return new EvolucionHatoFGEM(mesesFGEM, existenciasFGEM, nacimientosFGEM, muertesFGEM, salidasFGEM, produccionFGEM);
    }

    /** Animales de todas las especies al cierre de cada mes. */
    public int[] totalAnimalesFGEM() {
        int[] totalFGEM = new int[mesesFGEM.size()];
        existenciasFGEM.values().forEach(serieFGEM -> {
            for (int iFGEM = 0; iFGEM < serieFGEM.length; iFGEM++) {
                totalFGEM[iFGEM] += serieFGEM[iFGEM];
            }
        });
        return totalFGEM;
    }

    /** Tendencia de la producción mensual, desde el primer mes con producción; {@code null} con pocos datos. */
    public TendenciaFGEM tendenciaFGEM(ProductoGanaderoFGEM productoFGEM) {
        double[] serieFGEM = produccionFGEM.get(productoFGEM);
        if (serieFGEM == null) {
            return null;
        }
        int inicioFGEM = 0;
        while (inicioFGEM < serieFGEM.length && serieFGEM[inicioFGEM] == 0) {
            inicioFGEM++;
        }
        double[] utilFGEM = java.util.Arrays.copyOfRange(serieFGEM, inicioFGEM, serieFGEM.length);
        return utilFGEM.length < MINIMO_MESES_TENDENCIA_FGEM ? null : TendenciaFGEM.deFGEM(utilFGEM);
    }

    /** Frases en lenguaje sencillo sobre la evolución. */
    public List<String> lecturaFGEM() {
        List<String> frasesFGEM = new ArrayList<>();
        int[] totalFGEM = totalAnimalesFGEM();
        int nFGEM = totalFGEM.length;
        if (nFGEM == 0 || java.util.Arrays.stream(totalFGEM).allMatch(vFGEM -> vFGEM == 0)) {
            frasesFGEM.add("Aún no hay animales registrados en estos meses.");
            return frasesFGEM;
        }
        int inicioFGEM = 0;
        while (inicioFGEM < nFGEM && totalFGEM[inicioFGEM] == 0) {
            inicioFGEM++;
        }
        int cambioFGEM = totalFGEM[nFGEM - 1] - totalFGEM[inicioFGEM];
        frasesFGEM.add("El hato pasó de " + totalFGEM[inicioFGEM] + " animales en " + mesFGEM(mesesFGEM.get(inicioFGEM)) + " a " + totalFGEM[nFGEM - 1]
                + " en " + mesFGEM(mesesFGEM.get(nFGEM - 1)) + (cambioFGEM == 0 ? " (sin cambio)."
                : " (" + (cambioFGEM > 0 ? "+" : "") + cambioFGEM + ")."));
        int nacidosFGEM = java.util.Arrays.stream(nacimientosFGEM).sum();
        int muertosFGEM = java.util.Arrays.stream(muertesFGEM).sum();
        if (nacidosFGEM > 0 || muertosFGEM > 0) {
            int peorFGEM = 0;
            for (int iFGEM = 1; iFGEM < nFGEM; iFGEM++) {
                if (muertesFGEM[iFGEM] > muertesFGEM[peorFGEM]) {
                    peorFGEM = iFGEM;
                }
            }
            frasesFGEM.add("En el período nacieron " + nacidosFGEM + " y murieron " + muertosFGEM + " animales"
                    + (muertosFGEM == 0 ? "." : "; el mes con más muertes fue " + mesFGEM(mesesFGEM.get(peorFGEM)) + " ("
                    + muertesFGEM[peorFGEM] + ")."));
        }
        for (ProductoGanaderoFGEM pFGEM : produccionFGEM.keySet()) {
            TendenciaFGEM tFGEM = tendenciaFGEM(pFGEM);
            if (tFGEM == null) {
                continue;
            }
            String sentidoFGEM = switch (tFGEM.direccionFGEM()) {
                case CRECIENTE -> "va en aumento, alrededor de " + MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(tFGEM.crecimientoMensualFGEM())) + " al mes";
                case DECRECIENTE -> "va en descenso, alrededor de " + MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(-tFGEM.crecimientoMensualFGEM())) + " al mes";
                default -> "se mantiene estable, sin una tendencia clara";
            };
            frasesFGEM.add("La producción de " + pFGEM.descripcionFGEM().toLowerCase() + " " + sentidoFGEM + " (promedio de "
                    + MontosFGEM.cantidadFGEM(BigDecimal.valueOf(tFGEM.mediaFGEM()).setScale(0, java.math.RoundingMode.HALF_UP)) + " "
                    + pFGEM.abreviaturaFGEM() + " al mes).");
        }
        return frasesFGEM;
    }

    private static String mesFGEM(YearMonth mFGEM) {
        return FechasFGEM.nombreMesFGEM(mFGEM.getMonthValue()).toLowerCase() + " " + mFGEM.getYear();
    }
}
