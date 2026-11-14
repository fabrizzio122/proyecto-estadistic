package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.Margenes;
import com.businessanalyzer.analytics.RentabilidadProducto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Clasificación ABC (principio de Pareto) de los productos según sus ventas:
 * <ul>
 *     <li><b>A</b>: los que, sumados de mayor a menor, generan el primer 80 % de las ventas.</li>
 *     <li><b>B</b>: los que completan hasta el 95 %.</li>
 *     <li><b>C</b>: el resto (muchos productos que aportan poco).</li>
 * </ul>
 * Un producto es A si el acumulado <i>antes</i> de él es menor al 80 %; así el primero siempre es A.
 */
public record AnalisisAbc(List<Item> items, BigDecimal ventasTotales) {

    public static final BigDecimal LIMITE_A = new BigDecimal("0.80");
    public static final BigDecimal LIMITE_B = new BigDecimal("0.95");

    public enum Clase { A, B, C }

    /**
     * @param participacion ventas del producto ÷ ventas totales
     * @param acumulado     participación acumulada hasta este producto (inclusive)
     */
    public record Item(RentabilidadProducto producto, BigDecimal participacion, BigDecimal acumulado, Clase clase) {
    }

    public AnalisisAbc {
        items = List.copyOf(items);
    }

    public static AnalisisAbc de(List<RentabilidadProducto> productos) {
        List<RentabilidadProducto> ordenados = new ArrayList<>(productos);
        ordenados.sort(Comparator.comparing(RentabilidadProducto::ventas).reversed()
                .thenComparing(RentabilidadProducto::nombre));
        BigDecimal total = ordenados.stream().map(RentabilidadProducto::ventas).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Item> items = new ArrayList<>();
        if (total.signum() <= 0) {
            return new AnalisisAbc(items, total);
        }
        BigDecimal acumulado = BigDecimal.ZERO;
        for (RentabilidadProducto p : ordenados) {
            BigDecimal participacion = p.ventas().divide(total, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
            Clase clase = acumulado.compareTo(LIMITE_A) < 0 ? Clase.A
                    : acumulado.compareTo(LIMITE_B) < 0 ? Clase.B : Clase.C;
            acumulado = acumulado.add(participacion);
            items.add(new Item(p, participacion, acumulado.min(BigDecimal.ONE), clase));
        }
        return new AnalisisAbc(items, total);
    }

    public long cantidad(Clase clase) {
        return items.stream().filter(i -> i.clase() == clase).count();
    }

    /** Parte de las ventas que aporta una clase. */
    public BigDecimal participacion(Clase clase) {
        return items.stream().filter(i -> i.clase() == clase)
                .map(Item::participacion).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
