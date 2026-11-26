package com.businessanalyzer.model;

public record CategoriaFGEM(long idFGEM, long empresaIdFGEM, String nombreFGEM, String descripcionFGEM, int cantidadItemsFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
