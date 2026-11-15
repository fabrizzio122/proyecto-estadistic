package com.businessanalyzer.model;

/**
 * Foto de una visita.
 *
 * @param archivo ruta relativa a la carpeta de datos (por ejemplo {@code fotos-visitas/12/a1b2.jpg})
 */
public record FotoVisita(long id, long visitaId, String archivo, String descripcion) {
}
