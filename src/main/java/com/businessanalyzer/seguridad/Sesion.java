package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Usuario;

import java.util.Optional;

/**
 * Usuario que tiene la sesión abierta. Los servicios la consultan para comprobar permisos
 * antes de cada operación protegida (la interfaz oculta opciones, pero la verificación real
 * ocurre en los servicios).
 */
public final class Sesion {

    private volatile Usuario usuario;

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
    }

    /** Refresca los datos del usuario de la sesión tras modificarlos. */
    public void actualizar(Usuario actualizado) {
        Usuario actual = usuario;
        if (actual != null && actual.id() == actualizado.id()) {
            usuario = actualizado;
        }
    }

    public void cerrar() {
        usuario = null;
    }

    public boolean activa() {
        return usuario != null;
    }

    public Optional<Usuario> usuarioActual() {
        return Optional.ofNullable(usuario);
    }

    public Usuario usuario() {
        Usuario actual = usuario;
        if (actual == null) {
            throw new AccesoDenegadoException("No hay una sesión iniciada.");
        }
        return actual;
    }

    public boolean tienePermiso(Permiso permiso) {
        Usuario actual = usuario;
        return actual != null && actual.tiene(permiso);
    }

    public void requerir(Permiso permiso) {
        if (!tienePermiso(permiso)) {
            throw new AccesoDenegadoException("No tienes permiso para: " + permiso.descripcion().toLowerCase() + ".");
        }
    }
}
