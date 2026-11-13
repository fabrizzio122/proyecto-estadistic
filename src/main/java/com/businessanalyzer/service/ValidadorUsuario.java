package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.util.Validaciones;

import java.util.regex.Pattern;

/** Valida y normaliza los datos de un usuario (compartido por los servicios de usuarios). */
final class ValidadorUsuario {

    private static final Pattern FORMATO_USUARIO = Pattern.compile("[A-Za-z0-9._-]+");

    private ValidadorUsuario() {
    }

    static DatosUsuario normalizar(DatosUsuario datos) {
        if (datos == null) {
            throw new ValidacionException("Faltan los datos del usuario.");
        }
        String nombreUsuario = Validaciones.requerido(datos.nombreUsuario(), "Usuario", 3, 30);
        if (!FORMATO_USUARIO.matcher(nombreUsuario).matches()) {
            throw new ValidacionException(
                    "El usuario solo puede contener letras, números, punto, guion y guion bajo (sin espacios).");
        }
        String nombreCompleto = Validaciones.requerido(datos.nombreCompleto(), "Nombre completo", 3, 100);
        String email = Validaciones.email(datos.email(), "Correo electrónico");
        return new DatosUsuario(nombreUsuario, nombreCompleto, email, datos.rolId());
    }
}
