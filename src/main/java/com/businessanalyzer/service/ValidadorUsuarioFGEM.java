package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.util.regex.Pattern;

final class ValidadorUsuarioFGEM {

    private static final Pattern FORMATO_USUARIO_FGEM = Pattern.compile("[A-Za-z0-9._-]+");

    private ValidadorUsuarioFGEM() {
    }

    static DatosUsuarioFGEM normalizarFGEM(DatosUsuarioFGEM datosFGEM) {
        if (datosFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos del usuario.");
        }
        String nombreUsuarioFGEM = ValidacionesFGEM.requeridoFGEM(datosFGEM.nombreUsuarioFGEM(), "Usuario", 3, 30);
        if (!FORMATO_USUARIO_FGEM.matcher(nombreUsuarioFGEM).matches()) {
            throw new ValidacionExceptionFGEM(
                    "El usuario solo puede contener letras, números, punto, guion y guion bajo (sin espacios).");
        }
        String nombreCompletoFGEM = ValidacionesFGEM.requeridoFGEM(datosFGEM.nombreCompletoFGEM(), "Nombre completo", 3, 100);
        String emailFGEM = ValidacionesFGEM.emailFGEM(datosFGEM.emailFGEM(), "Correo electrónico");
        return new DatosUsuarioFGEM(nombreUsuarioFGEM, nombreCompletoFGEM, emailFGEM, datosFGEM.rolIdFGEM());
    }
}
