package com.businessanalyzer.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Datos de una visita tal como llegan desde el formulario.
 *
 * @param compromisos nuevos compromisos acordados en la visita (descripción y plazo)
 */
public record DatosVisita(LocalDate fecha, TipoVisita tipo, String inspector, String lugar, Double latitud,
                          Double longitud, String atendidoPor, String observaciones, ConclusionVisita conclusion,
                          LocalDate proximaVisita, Map<AspectoInspeccion, ResultadoAspecto> aspectos,
                          Map<AspectoInspeccion, String> notas, List<NuevoCompromiso> compromisos) {

    /** Compromiso acordado en la visita. */
    public record NuevoCompromiso(String descripcion, LocalDate plazo) {
    }
}
