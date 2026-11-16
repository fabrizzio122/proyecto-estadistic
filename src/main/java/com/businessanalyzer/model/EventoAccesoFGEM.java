package com.businessanalyzer.model;

/** Eventos de seguridad que se guardan en la tabla {@code registro_acceso}. */
public enum EventoAccesoFGEM {

    INICIO_SESION("Inicio de sesión"),
    CIERRE_SESION("Cierre de sesión"),
    CREDENCIALES_INVALIDAS("Credenciales inválidas"),
    CUENTA_BLOQUEADA("Cuenta bloqueada por intentos fallidos"),
    INTENTO_CUENTA_BLOQUEADA("Intento de acceso con cuenta bloqueada"),
    INTENTO_CUENTA_INACTIVA("Intento de acceso con cuenta inactiva"),
    CUENTA_CREADA("Cuenta creada"),
    CUENTA_ACTUALIZADA("Datos de la cuenta modificados"),
    CUENTA_ACTIVADA("Cuenta activada"),
    CUENTA_DESACTIVADA("Cuenta desactivada"),
    CUENTA_DESBLOQUEADA("Cuenta desbloqueada"),
    CAMBIO_PASSWORD("Cambio de contraseña"),
    PASSWORD_RESTABLECIDA("Contraseña restablecida por un administrador");

    private final String descripcionFGEM;

    EventoAccesoFGEM(String descripcionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }
}
