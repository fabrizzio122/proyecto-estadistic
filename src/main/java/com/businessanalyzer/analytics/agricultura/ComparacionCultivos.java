package com.businessanalyzer.analytics.agricultura;

import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.EstadoCampania;

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
 * @param rendimiento   kilos por hectárea promedio
 * @param avance        rendimiento ÷ referencia promedio; {@code null} sin referencia
 * @param costoPorHa    costo por hectárea promedio
 * @param margenPorHa   margen por hectárea promedio
 * @param mejor         campaña con mayor rendimiento
 * @param peor          campaña con menor rendimiento
 * @param perdidas      campañas perdidas
 */
public record ComparacionCultivos(Cultivo cultivo, int campanias, int perdidas, BigDecimal area,
                                  BigDecimal rendimiento, BigDecimal avance, BigDecimal costoPorHa,
                                  BigDecimal margenPorHa, ResultadoCampania mejor, ResultadoCampania peor) {

    /** Una fila por cultivo con campañas cerradas, del de mayor margen por hectárea al de menor. */
    public static List<ComparacionCultivos> de(Collection<ResultadoCampania> resultados) {
        Map<Cultivo, List<ResultadoCampania>> porCultivo = new EnumMap<>(Cultivo.class);
        for (ResultadoCampania r : resultados) {
            if (r.campania().estado().cerrada()) {
                porCultivo.computeIfAbsent(r.campania().cultivo(), k -> new ArrayList<>()).add(r);
            }
        }
        List<ComparacionCultivos> filas = new ArrayList<>();
        porCultivo.forEach((cultivo, lista) -> filas.add(resumir(cultivo, lista)));
        filas.sort(Comparator.comparing(ComparacionCultivos::margenPorHa).reversed());
        return filas;
    }

    private static ComparacionCultivos resumir(Cultivo cultivo, List<ResultadoCampania> lista) {
        BigDecimal area = BigDecimal.ZERO;
        BigDecimal kilos = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal margen = BigDecimal.ZERO;
        BigDecimal referenciaPonderada = BigDecimal.ZERO;
        BigDecimal areaConReferencia = BigDecimal.ZERO;
        for (ResultadoCampania r : lista) {
            BigDecimal ha = r.campania().area();
            area = area.add(ha);
            kilos = kilos.add(r.produccion());
            costo = costo.add(r.costoTotal());
            margen = margen.add(r.margen());
            if (r.campania().rendimientoReferencia() != null) {
                referenciaPonderada = referenciaPonderada.add(r.campania().rendimientoReferencia().multiply(ha));
                areaConReferencia = areaConReferencia.add(ha);
            }
        }
        BigDecimal rendimiento = kilos.divide(area, 2, RoundingMode.HALF_UP);
        BigDecimal avance = null;
        if (areaConReferencia.signum() > 0) {
            BigDecimal referencia = referenciaPonderada.divide(areaConReferencia, 6, RoundingMode.HALF_UP);
            avance = rendimiento.divide(referencia, 6, RoundingMode.HALF_UP);
        }
        Comparator<ResultadoCampania> porRendimiento = Comparator.comparing(
                r -> r.rendimiento() == null ? BigDecimal.ZERO : r.rendimiento());
        int perdidas = (int) lista.stream().filter(r -> r.campania().estado() == EstadoCampania.PERDIDA).count();
        return new ComparacionCultivos(cultivo, lista.size(), perdidas, area, rendimiento, avance,
                costo.divide(area, 2, RoundingMode.HALF_UP), margen.divide(area, 2, RoundingMode.HALF_UP),
                lista.stream().max(porRendimiento).orElseThrow(), lista.stream().min(porRendimiento).orElseThrow());
    }
}
