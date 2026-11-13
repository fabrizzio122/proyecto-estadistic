package com.businessanalyzer.model;

import java.time.LocalDateTime;

/**
 * Usuario del sistema. No incluye el hash de la contraseña: este nunca sale de la capa
 * de datos y servicios (ver {@code UsuarioRepository.Credenciales}).
 */
public record Usuario(
        long id,
        String nombreUsuario,
        String nombreCompleto,
        String email,
        Rol rol,
        boolean activo,
        boolean debeCambiarPassword,
        int intentosFallidos,
        LocalDateTime bloqueadoHasta,
        LocalDateTime ultimoAcceso,
        LocalDateTime creadoEn) {

    public boolean bloqueadoEn(LocalDateTime momento) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(momento);
    }

    public boolean tiene(Permiso permiso) {
        return rol.tiene(permiso);
    }

    /** Estado legible: Activo, Inactivo o Bloqueado. */
    public String estado(LocalDateTime momento) {
        if (!activo) {
            return "Inactivo";
        }
        return bloqueadoEn(momento) ? "Bloqueado" : "Activo";
    }
}
