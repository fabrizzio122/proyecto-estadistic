package com.businessanalyzer.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro de un informe generado (historial del módulo Informes). */
public record InformeGeneradoFGEM(long idFGEM, long empresaIdFGEM, TipoInformeFGEM tipoFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                              Path archivoFGEM, String generadoPorFGEM, LocalDateTime generadoEnFGEM) {
}
