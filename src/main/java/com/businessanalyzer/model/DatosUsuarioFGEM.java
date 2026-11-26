package com.businessanalyzer.model;

public record DatosUsuarioFGEM(String nombreUsuarioFGEM, String nombreCompletoFGEM, String emailFGEM, long rolIdFGEM) {

    public DatosUsuarioFGEM conRolFGEM(long nuevoRolIdFGEM) {
        return new DatosUsuarioFGEM(nombreUsuarioFGEM, nombreCompletoFGEM, emailFGEM, nuevoRolIdFGEM);
    }
}
