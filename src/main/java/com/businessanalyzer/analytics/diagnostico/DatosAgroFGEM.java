package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;

import java.util.List;

public record DatosAgroFGEM(List<IndicadoresEspecieFGEM> especiesFGEM, List<ResultadoCampaniaFGEM> campaniasFGEM,
                        int compromisosCumplidosFGEM, int compromisosFallidosFGEM) {

    public DatosAgroFGEM {
        especiesFGEM = List.copyOf(especiesFGEM);
        campaniasFGEM = List.copyOf(campaniasFGEM);
    }
}
