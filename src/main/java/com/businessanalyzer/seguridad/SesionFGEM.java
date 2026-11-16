package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.UsuarioFGEM;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Usuario que tiene la sesión abierta y empresa con la que está trabajando.
 * <p>
 * Los servicios la consultan para comprobar permisos antes de cada operación protegida
 * (la interfaz oculta opciones, pero la verificación real ocurre en los servicios).
 * Desde la Fase 4, los datos de negocio se registran siempre sobre la empresa activa.
 */
public final class SesionFGEM {

    private volatile UsuarioFGEM usuarioFGEM;
    private volatile EmpresaFGEM empresaActivaFGEM;
    private volatile Consumer<Optional<EmpresaFGEM>> oyenteEmpresaFGEM;

    public void iniciarFGEM(UsuarioFGEM usuarioFGEM) {
        this.usuarioFGEM = usuarioFGEM;
        this.empresaActivaFGEM = null;
    }

    /** Refresca los datos del usuario de la sesión tras modificarlos. */
    public void actualizarFGEM(UsuarioFGEM actualizadoFGEM) {
        UsuarioFGEM actualFGEM = usuarioFGEM;
        if (actualFGEM != null && actualFGEM.idFGEM() == actualizadoFGEM.idFGEM()) {
            usuarioFGEM = actualizadoFGEM;
        }
    }

    public void cerrarFGEM() {
        oyenteEmpresaFGEM = null;
        usuarioFGEM = null;
        empresaActivaFGEM = null;
    }

    public boolean activaFGEM() {
        return usuarioFGEM != null;
    }

    public Optional<UsuarioFGEM> usuarioActualFGEM() {
        return Optional.ofNullable(usuarioFGEM);
    }

    public UsuarioFGEM usuarioFGEM() {
        UsuarioFGEM actualFGEM = usuarioFGEM;
        if (actualFGEM == null) {
            throw new AccesoDenegadoExceptionFGEM("No hay una sesión iniciada.");
        }
        return actualFGEM;
    }

    public boolean tienePermisoFGEM(PermisoFGEM permisoFGEM) {
        UsuarioFGEM actualFGEM = usuarioFGEM;
        return actualFGEM != null && actualFGEM.tieneFGEM(permisoFGEM);
    }

    public void requerirFGEM(PermisoFGEM permisoFGEM) {
        if (!tienePermisoFGEM(permisoFGEM)) {
            throw new AccesoDenegadoExceptionFGEM("No tienes permiso para: " + permisoFGEM.descripcionFGEM().toLowerCase() + ".");
        }
    }

    // ------------------------------------------------------------------ empresa activa

    public Optional<EmpresaFGEM> empresaActivaFGEM() {
        return Optional.ofNullable(empresaActivaFGEM);
    }

    /** Para las operaciones que solo tienen sentido con una empresa seleccionada. */
    public EmpresaFGEM empresaActivaRequeridaFGEM() {
        EmpresaFGEM actualFGEM = empresaActivaFGEM;
        if (actualFGEM == null) {
            throw new ValidacionExceptionFGEM("Selecciona una empresa en la barra superior antes de continuar.");
        }
        return actualFGEM;
    }

    /** Lo usa {@code EmpresaService} después de comprobar el acceso; {@code null} deja la sesión sin empresa. */
    public void seleccionarEmpresaFGEM(EmpresaFGEM empresaFGEM) {
        empresaActivaFGEM = empresaFGEM;
        notificarFGEM();
    }

    /** Refresca la empresa activa si fue modificada. */
    public void actualizarEmpresaFGEM(EmpresaFGEM modificadaFGEM) {
        EmpresaFGEM actualFGEM = empresaActivaFGEM;
        if (actualFGEM != null && actualFGEM.idFGEM() == modificadaFGEM.idFGEM()) {
            empresaActivaFGEM = modificadaFGEM.activaFGEM() ? modificadaFGEM : null;
            notificarFGEM();
        }
    }

    /** Un único oyente (la ventana principal) que se entera cuando cambia la empresa activa. */
    public void alCambiarEmpresaFGEM(Consumer<Optional<EmpresaFGEM>> oyenteFGEM) {
        this.oyenteEmpresaFGEM = oyenteFGEM;
    }

    private void notificarFGEM() {
        Consumer<Optional<EmpresaFGEM>> oyenteFGEM = oyenteEmpresaFGEM;
        if (oyenteFGEM != null) {
            oyenteFGEM.accept(empresaActivaFGEM());
        }
    }
}
