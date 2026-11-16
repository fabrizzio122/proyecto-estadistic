package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;

import java.util.regex.Pattern;

/**
 * Validaciones de texto reutilizables por todos los servicios. Cada método devuelve el
 * valor normalizado (sin espacios al inicio ni al final) o lanza {@link ValidacionExceptionFGEM}
 * con un mensaje listo para mostrarse al usuario.
 */
public final class ValidacionesFGEM {

    private static final Pattern EMAIL_FGEM = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int LONGITUD_MAXIMA_EMAIL_FGEM = 120;

    private ValidacionesFGEM() {
    }

    public static String requeridoFGEM(String valorFGEM, String campoFGEM, int minimoFGEM, int maximoFGEM) {
        String textoFGEM = valorFGEM == null ? "" : valorFGEM.strip();
        if (textoFGEM.isEmpty()) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" es obligatorio.");
        }
        if (textoFGEM.length() < minimoFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe tener al menos " + minimoFGEM + " caracteres.");
        }
        if (textoFGEM.length() > maximoFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" no puede superar " + maximoFGEM + " caracteres.");
        }
        return textoFGEM;
    }

    /** @return el texto normalizado, o {@code null} si está vacío. */
    public static String opcionalFGEM(String valorFGEM, String campoFGEM, int maximoFGEM) {
        String textoFGEM = valorFGEM == null ? "" : valorFGEM.strip();
        if (textoFGEM.isEmpty()) {
            return null;
        }
        if (textoFGEM.length() > maximoFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" no puede superar " + maximoFGEM + " caracteres.");
        }
        return textoFGEM;
    }

    public static String emailFGEM(String valorFGEM, String campoFGEM) {
        String textoFGEM = opcionalFGEM(valorFGEM, campoFGEM, LONGITUD_MAXIMA_EMAIL_FGEM);
        if (textoFGEM != null && !EMAIL_FGEM.matcher(textoFGEM).matches()) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" no tiene un formato de correo válido.");
        }
        return textoFGEM;
    }
}
