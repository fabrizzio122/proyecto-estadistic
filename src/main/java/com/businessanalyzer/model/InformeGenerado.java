package com.businessanalyzer.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro de un informe generado (historial del módulo Informes). */
public record InformeGenerado(long id, long empresaId, TipoInforme tipo, LocalDate desde, LocalDate hasta,
                              Path archivo, String generadoPor, LocalDateTime generadoEn) {
}
