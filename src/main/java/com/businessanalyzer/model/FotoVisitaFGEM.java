package com.businessanalyzer.model;

/**
 * Foto de una visita.
 *
 * @param archivoFGEM ruta relativa a la carpeta de datos (por ejemplo {@code fotos-visitas/12/a1b2.jpg})
 */
public record FotoVisitaFGEM(long idFGEM, long visitaIdFGEM, String archivoFGEM, String descripcionFGEM) {
}
