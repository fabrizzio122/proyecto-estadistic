package com.businessanalyzer.model;

/** Datos editables de un usuario, tal como llegan desde un formulario. */
public record DatosUsuarioFGEM(String nombreUsuarioFGEM, String nombreCompletoFGEM, String emailFGEM, long rolIdFGEM) {

    public DatosUsuarioFGEM conRolFGEM(long nuevoRolIdFGEM) {
        return new DatosUsuarioFGEM(nombreUsuarioFGEM, nombreCompletoFGEM, emailFGEM, nuevoRolIdFGEM);
    }
}
