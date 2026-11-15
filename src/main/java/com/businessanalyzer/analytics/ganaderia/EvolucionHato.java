package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

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
 * @param existencias animales al cierre de cada mes, por especie (solo especies con animales)
 * @param produccion  producción de cada mes, por producto (solo productos registrados)
 */
public record EvolucionHato(List<YearMonth> meses, Map<Especie, int[]> existencias, int[] nacimientos,
                            int[] muertes, int[] salidas, Map<ProductoGanadero, double[]> produccion) {

    /** Meses con datos necesarios para hablar de tendencia. */
    public static final int MINIMO_MESES_TENDENCIA = 6;

    public static EvolucionHato calcular(YearMonth desde, YearMonth hasta, Collection<MovimientoGanado> movimientos,
                                         Collection<ProduccionGanadera> registros) {
        List<YearMonth> meses = new ArrayList<>();
        for (YearMonth m = desde; !m.isAfter(hasta); m = m.plusMonths(1)) {
            meses.add(m);
        }
        int n = meses.size();
        Map<Especie, int[]> existencias = new EnumMap<>(Especie.class);
        int[] nacimientos = new int[n];
        int[] muertes = new int[n];
        int[] salidas = new int[n];
        for (int i = 0; i < n; i++) {
            Map<CategoriaGanado, Integer> alCierre = Hato.existencias(movimientos, meses.get(i).atEndOfMonth());
            for (Map.Entry<CategoriaGanado, Integer> e : alCierre.entrySet()) {
                existencias.computeIfAbsent(e.getKey().especie(), k -> new int[n])[i] += e.getValue();
            }
        }
        for (MovimientoGanado m : movimientos) {
            int i = meses.indexOf(YearMonth.from(m.fecha()));
            if (m.anulado() || i < 0) {
                continue;
            }
            switch (m.tipo()) {
                case NACIMIENTO -> nacimientos[i] += m.cantidad();
                case MUERTE -> muertes[i] += m.cantidad();
                case VENTA, CONSUMO -> salidas[i] += m.cantidad();
                default -> {
                }
            }
        }
        Map<ProductoGanadero, double[]> produccion = new EnumMap<>(ProductoGanadero.class);
        for (ProduccionGanadera p : registros) {
            int i = meses.indexOf(YearMonth.from(p.fecha()));
            if (!p.anulado() && i >= 0) {
                produccion.computeIfAbsent(p.producto(), k -> new double[n])[i] += p.cantidad().doubleValue();
            }
        }
        return new EvolucionHato(meses, existencias, nacimientos, muertes, salidas, produccion);
    }

    /** Animales de todas las especies al cierre de cada mes. */
    public int[] totalAnimales() {
        int[] total = new int[meses.size()];
        existencias.values().forEach(serie -> {
            for (int i = 0; i < serie.length; i++) {
                total[i] += serie[i];
            }
        });
        return total;
    }

    /** Tendencia de la producción mensual, desde el primer mes con producción; {@code null} con pocos datos. */
    public Tendencia tendencia(ProductoGanadero producto) {
        double[] serie = produccion.get(producto);
        if (serie == null) {
            return null;
        }
        int inicio = 0;
        while (inicio < serie.length && serie[inicio] == 0) {
            inicio++;
        }
        double[] util = java.util.Arrays.copyOfRange(serie, inicio, serie.length);
        return util.length < MINIMO_MESES_TENDENCIA ? null : Tendencia.de(util);
    }

    /** Frases en lenguaje sencillo sobre la evolución. */
    public List<String> lectura() {
        List<String> frases = new ArrayList<>();
        int[] total = totalAnimales();
        int n = total.length;
        if (n == 0 || java.util.Arrays.stream(total).allMatch(v -> v == 0)) {
            frases.add("Aún no hay animales registrados en estos meses.");
            return frases;
        }
        int inicio = 0;
        while (inicio < n && total[inicio] == 0) {
            inicio++;
        }
        int cambio = total[n - 1] - total[inicio];
        frases.add("El hato pasó de " + total[inicio] + " animales en " + mes(meses.get(inicio)) + " a " + total[n - 1]
                + " en " + mes(meses.get(n - 1)) + (cambio == 0 ? " (sin cambio)."
                : " (" + (cambio > 0 ? "+" : "") + cambio + ")."));
        int nacidos = java.util.Arrays.stream(nacimientos).sum();
        int muertos = java.util.Arrays.stream(muertes).sum();
        if (nacidos > 0 || muertos > 0) {
            int peor = 0;
            for (int i = 1; i < n; i++) {
                if (muertes[i] > muertes[peor]) {
                    peor = i;
                }
            }
            frases.add("En el período nacieron " + nacidos + " y murieron " + muertos + " animales"
                    + (muertos == 0 ? "." : "; el mes con más muertes fue " + mes(meses.get(peor)) + " ("
                    + muertes[peor] + ")."));
        }
        for (ProductoGanadero p : produccion.keySet()) {
            Tendencia t = tendencia(p);
            if (t == null) {
                continue;
            }
            String sentido = switch (t.direccion()) {
                case CRECIENTE -> "va en aumento, alrededor de " + Montos.porcentaje(BigDecimal.valueOf(t.crecimientoMensual())) + " al mes";
                case DECRECIENTE -> "va en descenso, alrededor de " + Montos.porcentaje(BigDecimal.valueOf(-t.crecimientoMensual())) + " al mes";
                default -> "se mantiene estable, sin una tendencia clara";
            };
            frases.add("La producción de " + p.descripcion().toLowerCase() + " " + sentido + " (promedio de "
                    + Montos.cantidad(BigDecimal.valueOf(t.media()).setScale(0, java.math.RoundingMode.HALF_UP)) + " "
                    + p.abreviatura() + " al mes).");
        }
        return frases;
    }

    private static String mes(YearMonth m) {
        return Fechas.nombreMes(m.getMonthValue()).toLowerCase() + " " + m.getYear();
    }
}
