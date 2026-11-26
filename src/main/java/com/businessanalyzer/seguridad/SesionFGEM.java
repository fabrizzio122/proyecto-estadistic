package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.UsuarioFGEM;

import java.util.Optional;
import java.util.function.Consumer;

public final class SesionFGEM {

    private volatile UsuarioFGEM usuarioFGEM;
    private volatile EmpresaFGEM empresaActivaFGEM;
    private volatile Consumer<Optional<EmpresaFGEM>> oyenteEmpresaFGEM;

    public void iniciarFGEM(UsuarioFGEM usuarioFGEM) {
        this.usuarioFGEM = usuarioFGEM;
        this.empresaActivaFGEM = null;
    }

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

    public Optional<EmpresaFGEM> empresaActivaFGEM() {
        return Optional.ofNullable(empresaActivaFGEM);
    }

    public EmpresaFGEM empresaActivaRequeridaFGEM() {
        EmpresaFGEM actualFGEM = empresaActivaFGEM;
        if (actualFGEM == null) {
            throw new ValidacionExceptionFGEM("Selecciona una empresa en la barra superior antes de continuar.");
        }
        return actualFGEM;
    }

    public void seleccionarEmpresaFGEM(EmpresaFGEM empresaFGEM) {
        empresaActivaFGEM = empresaFGEM;
        notificarFGEM();
    }

    public void actualizarEmpresaFGEM(EmpresaFGEM modificadaFGEM) {
        EmpresaFGEM actualFGEM = empresaActivaFGEM;
        if (actualFGEM != null && actualFGEM.idFGEM() == modificadaFGEM.idFGEM()) {
            empresaActivaFGEM = modificadaFGEM.activaFGEM() ? modificadaFGEM : null;
            notificarFGEM();
        }
    }

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
