package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.agricultura.ResultadoCampania;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecie;

import java.util.List;

/**
 * Información agropecuaria para el diagnóstico de un productor (últimos 12 meses).
 *
 * @param especies              indicadores de cada especie con animales
 * @param campanias             campañas cerradas (cosechadas o perdidas) en los últimos 12 meses
 * @param compromisosCumplidos  compromisos de inspección cumplidos
 * @param compromisosFallidos   no cumplidos o pendientes con el plazo vencido
 */
public record DatosAgro(List<IndicadoresEspecie> especies, List<ResultadoCampania> campanias,
                        int compromisosCumplidos, int compromisosFallidos) {

    public DatosAgro {
        especies = List.copyOf(especies);
        campanias = List.copyOf(campanias);
    }
}
