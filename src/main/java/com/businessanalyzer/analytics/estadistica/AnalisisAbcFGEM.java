package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record AnalisisAbcFGEM(List<ItemFGEM> itemsFGEM, BigDecimal ventasTotalesFGEM) {

    public static final BigDecimal LIMITE_A_FGEM = new BigDecimal("0.80");
    public static final BigDecimal LIMITE_B_FGEM = new BigDecimal("0.95");

    public enum ClaseFGEM { A, B, C }

    public record ItemFGEM(RentabilidadProductoFGEM productoFGEM, BigDecimal participacionFGEM, BigDecimal acumuladoFGEM, ClaseFGEM claseFGEM) {
    }

    public AnalisisAbcFGEM {
        itemsFGEM = List.copyOf(itemsFGEM);
    }

    public static AnalisisAbcFGEM deFGEM(List<RentabilidadProductoFGEM> productosFGEM) {
        List<RentabilidadProductoFGEM> ordenadosFGEM = new ArrayList<>(productosFGEM);
        ordenadosFGEM.sort(Comparator.comparing(RentabilidadProductoFGEM::ventasFGEM).reversed()
                .thenComparing(RentabilidadProductoFGEM::nombreFGEM));
        BigDecimal totalFGEM = ordenadosFGEM.stream().map(RentabilidadProductoFGEM::ventasFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ItemFGEM> itemsFGEM = new ArrayList<>();
        if (totalFGEM.signum() <= 0) {
            return new AnalisisAbcFGEM(itemsFGEM, totalFGEM);
        }
        BigDecimal acumuladoFGEM = BigDecimal.ZERO;
        for (RentabilidadProductoFGEM pFGEM : ordenadosFGEM) {
            BigDecimal participacionFGEM = pFGEM.ventasFGEM().divide(totalFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
            ClaseFGEM claseFGEM = acumuladoFGEM.compareTo(LIMITE_A_FGEM) < 0 ? ClaseFGEM.A
                    : acumuladoFGEM.compareTo(LIMITE_B_FGEM) < 0 ? ClaseFGEM.B : ClaseFGEM.C;
            acumuladoFGEM = acumuladoFGEM.add(participacionFGEM);
            itemsFGEM.add(new ItemFGEM(pFGEM, participacionFGEM, acumuladoFGEM.min(BigDecimal.ONE), claseFGEM));
        }
        return new AnalisisAbcFGEM(itemsFGEM, totalFGEM);
    }

    public long cantidadFGEM(ClaseFGEM claseFGEM) {
        return itemsFGEM.stream().filter(iFGEM -> iFGEM.claseFGEM() == claseFGEM).count();
    }

    public BigDecimal participacionFGEM(ClaseFGEM claseFGEM) {
        return itemsFGEM.stream().filter(iFGEM -> iFGEM.claseFGEM() == claseFGEM)
                .map(ItemFGEM::participacionFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
