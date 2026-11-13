package com.businessanalyzer.model;

/**
 * Agrupación de productos y servicios dentro de una empresa.
 *
 * @param cantidadItems productos y servicios (incluidos los archivados) que la usan
 */
public record Categoria(long id, long empresaId, String nombre, String descripcion, int cantidadItems) {

    @Override
    public String toString() {
        return nombre;
    }
}
