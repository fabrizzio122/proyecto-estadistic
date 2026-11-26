package com.businessanalyzer.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InformeGeneradoFGEM(long idFGEM, long empresaIdFGEM, TipoInformeFGEM tipoFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                              Path archivoFGEM, String generadoPorFGEM, LocalDateTime generadoEnFGEM) {
}
