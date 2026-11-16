package com.businessanalyzer.analytics.agricultura;

import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Comparación de las campañas cerradas (cosechadas o perdidas) agrupadas por cultivo. Los
 * promedios se ponderan por la superficie: una campaña de 2 ha pesa el doble que una de 1 ha.
 *
 * @param rendimientoFGEM   kilos por hectárea promedio
 * @param avanceFGEM        rendimiento ÷ referencia promedio; {@code null} sin referencia
 * @param costoPorHaFGEM    costo por hectárea promedio
 * @param margenPorHaFGEM   margen por hectárea promedio
 * @param mejorFGEM         campaña con mayor rendimiento
 * @param peorFGEM          campaña con menor rendimiento
 * @param perdidasFGEM      campañas perdidas
 */
public record ComparacionCultivosFGEM(CultivoFGEM cultivoFGEM, int campaniasFGEM, int perdidasFGEM, BigDecimal areaFGEM,
                                  BigDecimal rendimientoFGEM, BigDecimal avanceFGEM, BigDecimal costoPorHaFGEM,
                                  BigDecimal margenPorHaFGEM, ResultadoCampaniaFGEM mejorFGEM, ResultadoCampaniaFGEM peorFGEM) {

    /** Una fila por cultivo con campañas cerradas, del de mayor margen por hectárea al de menor. */
    public static List<ComparacionCultivosFGEM> deFGEM(Collection<ResultadoCampaniaFGEM> resultadosFGEM) {
        Map<CultivoFGEM, List<ResultadoCampaniaFGEM>> porCultivoFGEM = new EnumMap<>(CultivoFGEM.class);
        for (ResultadoCampaniaFGEM rFGEM : resultadosFGEM) {
            if (rFGEM.campaniaFGEM().estadoFGEM().cerradaFGEM()) {
                porCultivoFGEM.computeIfAbsent(rFGEM.campaniaFGEM().cultivoFGEM(), kFGEM -> new ArrayList<>()).add(rFGEM);
            }
        }
        List<ComparacionCultivosFGEM> filasFGEM = new ArrayList<>();
        porCultivoFGEM.forEach((cultivoFGEM, listaFGEM) -> filasFGEM.add(resumirFGEM(cultivoFGEM, listaFGEM)));
        filasFGEM.sort(Comparator.comparing(ComparacionCultivosFGEM::margenPorHaFGEM).reversed());
        return filasFGEM;
    }

    private static ComparacionCultivosFGEM resumirFGEM(CultivoFGEM cultivoFGEM, List<ResultadoCampaniaFGEM> listaFGEM) {
        BigDecimal areaFGEM = BigDecimal.ZERO;
        BigDecimal kilosFGEM = BigDecimal.ZERO;
        BigDecimal costoFGEM = BigDecimal.ZERO;
        BigDecimal margenFGEM = BigDecimal.ZERO;
        BigDecimal referenciaPonderadaFGEM = BigDecimal.ZERO;
        BigDecimal areaConReferenciaFGEM = BigDecimal.ZERO;
        for (ResultadoCampaniaFGEM rFGEM : listaFGEM) {
            BigDecimal haFGEM = rFGEM.campaniaFGEM().areaFGEM();
            areaFGEM = areaFGEM.add(haFGEM);
            kilosFGEM = kilosFGEM.add(rFGEM.produccionFGEM());
            costoFGEM = costoFGEM.add(rFGEM.costoTotalFGEM());
            margenFGEM = margenFGEM.add(rFGEM.margenFGEM());
            if (rFGEM.campaniaFGEM().rendimientoReferenciaFGEM() != null) {
                referenciaPonderadaFGEM = referenciaPonderadaFGEM.add(rFGEM.campaniaFGEM().rendimientoReferenciaFGEM().multiply(haFGEM));
                areaConReferenciaFGEM = areaConReferenciaFGEM.add(haFGEM);
            }
        }
        BigDecimal rendimientoFGEM = kilosFGEM.divide(areaFGEM, 2, RoundingMode.HALF_UP);
        BigDecimal avanceFGEM = null;
        if (areaConReferenciaFGEM.signum() > 0) {
            BigDecimal referenciaFGEM = referenciaPonderadaFGEM.divide(areaConReferenciaFGEM, 6, RoundingMode.HALF_UP);
            avanceFGEM = rendimientoFGEM.divide(referenciaFGEM, 6, RoundingMode.HALF_UP);
        }
        Comparator<ResultadoCampaniaFGEM> porRendimientoFGEM = Comparator.comparing(
                rFGEM -> rFGEM.rendimientoFGEM() == null ? BigDecimal.ZERO : rFGEM.rendimientoFGEM());
        int perdidasFGEM = (int) listaFGEM.stream().filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.PERDIDA).count();
        return new ComparacionCultivosFGEM(cultivoFGEM, listaFGEM.size(), perdidasFGEM, areaFGEM, rendimientoFGEM, avanceFGEM,
                costoFGEM.divide(areaFGEM, 2, RoundingMode.HALF_UP), margenFGEM.divide(areaFGEM, 2, RoundingMode.HALF_UP),
                listaFGEM.stream().max(porRendimientoFGEM).orElseThrow(), listaFGEM.stream().min(porRendimientoFGEM).orElseThrow());
    }
}
