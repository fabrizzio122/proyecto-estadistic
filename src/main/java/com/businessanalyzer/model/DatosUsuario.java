package com.businessanalyzer.model;

/** Datos editables de un usuario, tal como llegan desde un formulario. */
public record DatosUsuario(String nombreUsuario, String nombreCompleto, String email, long rolId) {

    public DatosUsuario conRol(long nuevoRolId) {
        return new DatosUsuario(nombreUsuario, nombreCompleto, email, nuevoRolId);
    }
}
