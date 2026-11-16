package com.businessanalyzer.model;

/**
 * Agrupación de productos y servicios dentro de una empresa.
 *
 * @param cantidadItemsFGEM productos y servicios (incluidos los archivados) que la usan
 */
public record CategoriaFGEM(long idFGEM, long empresaIdFGEM, String nombreFGEM, String descripcionFGEM, int cantidadItemsFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
