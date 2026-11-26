package com.businessanalyzer.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DatosVisitaFGEM(LocalDate fechaFGEM, TipoVisitaFGEM tipoFGEM, String inspectorFGEM, String lugarFGEM, Double latitudFGEM,
                          Double longitudFGEM, String atendidoPorFGEM, String observacionesFGEM, ConclusionVisitaFGEM conclusionFGEM,
                          LocalDate proximaVisitaFGEM, Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM,
                          Map<AspectoInspeccionFGEM, String> notasFGEM, List<NuevoCompromisoFGEM> compromisosFGEM) {

    public record NuevoCompromisoFGEM(String descripcionFGEM, LocalDate plazoFGEM) {
    }
}
