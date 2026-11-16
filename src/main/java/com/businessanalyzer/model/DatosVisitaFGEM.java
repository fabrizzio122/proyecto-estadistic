package com.businessanalyzer.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Datos de una visita tal como llegan desde el formulario.
 *
 * @param compromisosFGEM nuevos compromisos acordados en la visita (descripción y plazo)
 */
public record DatosVisitaFGEM(LocalDate fechaFGEM, TipoVisitaFGEM tipoFGEM, String inspectorFGEM, String lugarFGEM, Double latitudFGEM,
                          Double longitudFGEM, String atendidoPorFGEM, String observacionesFGEM, ConclusionVisitaFGEM conclusionFGEM,
                          LocalDate proximaVisitaFGEM, Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM,
                          Map<AspectoInspeccionFGEM, String> notasFGEM, List<NuevoCompromisoFGEM> compromisosFGEM) {

    /** Compromiso acordado en la visita. */
    public record NuevoCompromisoFGEM(String descripcionFGEM, LocalDate plazoFGEM) {
    }
}
