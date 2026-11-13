package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Usuario;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Usuario que tiene la sesión abierta y empresa con la que está trabajando.
 * <p>
 * Los servicios la consultan para comprobar permisos antes de cada operación protegida
 * (la interfaz oculta opciones, pero la verificación real ocurre en los servicios).
 * Desde la Fase 4, los datos de negocio se registran siempre sobre la empresa activa.
 */
public final class Sesion {

    private volatile Usuario usuario;
    private volatile Empresa empresaActiva;
    private volatile Consumer<Optional<Empresa>> oyenteEmpresa;

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
        this.empresaActiva = null;
    }

    /** Refresca los datos del usuario de la sesión tras modificarlos. */
    public void actualizar(Usuario actualizado) {
        Usuario actual = usuario;
        if (actual != null && actual.id() == actualizado.id()) {
            usuario = actualizado;
        }
    }

    public void cerrar() {
        oyenteEmpresa = null;
        usuario = null;
        empresaActiva = null;
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

    // ------------------------------------------------------------------ empresa activa

    public Optional<Empresa> empresaActiva() {
        return Optional.ofNullable(empresaActiva);
    }

    /** Para las operaciones que solo tienen sentido con una empresa seleccionada. */
    public Empresa empresaActivaRequerida() {
        Empresa actual = empresaActiva;
        if (actual == null) {
            throw new ValidacionException("Selecciona una empresa en la barra superior antes de continuar.");
        }
        return actual;
    }

    /** Lo usa {@code EmpresaService} después de comprobar el acceso; {@code null} deja la sesión sin empresa. */
    public void seleccionarEmpresa(Empresa empresa) {
        empresaActiva = empresa;
        notificar();
    }

    /** Refresca la empresa activa si fue modificada. */
    public void actualizarEmpresa(Empresa modificada) {
        Empresa actual = empresaActiva;
        if (actual != null && actual.id() == modificada.id()) {
            empresaActiva = modificada.activa() ? modificada : null;
            notificar();
        }
    }

    /** Un único oyente (la ventana principal) que se entera cuando cambia la empresa activa. */
    public void alCambiarEmpresa(Consumer<Optional<Empresa>> oyente) {
        this.oyenteEmpresa = oyente;
    }

    private void notificar() {
        Consumer<Optional<Empresa>> oyente = oyenteEmpresa;
        if (oyente != null) {
            oyente.accept(empresaActiva());
        }
    }
}
