package com.businessanalyzer.model;

import java.time.LocalDateTime;

/**
 * Usuario del sistema. No incluye el hash de la contraseña: este nunca sale de la capa
 * de datos y servicios (ver {@code UsuarioRepository.Credenciales}).
 */
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

    /** Estado legible: Activo, Inactivo o Bloqueado. */
    public String estadoFGEM(LocalDateTime momentoFGEM) {
        if (!activoFGEM) {
            return "Inactivo";
        }
        return bloqueadoEnFGEM(momentoFGEM) ? "Bloqueado" : "Activo";
    }
}
