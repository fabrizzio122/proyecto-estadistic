package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;

import java.util.List;

/**
 * Información agropecuaria para el diagnóstico de un productor (últimos 12 meses).
 *
 * @param especiesFGEM              indicadores de cada especie con animales
 * @param campaniasFGEM             campañas cerradas (cosechadas o perdidas) en los últimos 12 meses
 * @param compromisosCumplidosFGEM  compromisos de inspección cumplidos
 * @param compromisosFallidosFGEM   no cumplidos o pendientes con el plazo vencido
 */
public record DatosAgroFGEM(List<IndicadoresEspecieFGEM> especiesFGEM, List<ResultadoCampaniaFGEM> campaniasFGEM,
                        int compromisosCumplidosFGEM, int compromisosFallidosFGEM) {

    public DatosAgroFGEM {
        especiesFGEM = List.copyOf(especiesFGEM);
        campaniasFGEM = List.copyOf(campaniasFGEM);
    }
}
