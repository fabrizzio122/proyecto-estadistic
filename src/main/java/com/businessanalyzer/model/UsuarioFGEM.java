package com.businessanalyzer.model;

import java.time.LocalDateTime;

public record UsuarioFGEM(
        long idFGEM,
        String nombreUsuarioFGEM,
        String nombreCompletoFGEM,
        String emailFGEM,
        RolFGEM rolFGEM,
        boolean activoFGEM,
        boolean debeCambiarPasswordFGEM,
        int intentosFallidosFGEM,
        LocalDateTime bloqueadoHastaFGEM,
        LocalDateTime ultimoAccesoFGEM,
        LocalDateTime creadoEnFGEM) {

    public boolean bloqueadoEnFGEM(LocalDateTime momentoFGEM) {
        return bloqueadoHastaFGEM != null && bloqueadoHastaFGEM.isAfter(momentoFGEM);
    }

    public boolean tieneFGEM(PermisoFGEM permisoFGEM) {
        return rolFGEM.tieneFGEM(permisoFGEM);
    }

    public String estadoFGEM(LocalDateTime momentoFGEM) {
        if (!activoFGEM) {
            return "Inactivo";
        }
        return bloqueadoEnFGEM(momentoFGEM) ? "Bloqueado" : "Activo";
    }
}
