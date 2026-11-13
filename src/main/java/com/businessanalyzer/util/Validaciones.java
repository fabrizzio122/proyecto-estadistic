package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionException;

import java.util.regex.Pattern;

/**
 * Validaciones de texto reutilizables por todos los servicios. Cada método devuelve el
 * valor normalizado (sin espacios al inicio ni al final) o lanza {@link ValidacionException}
 * con un mensaje listo para mostrarse al usuario.
 */
public final class Validaciones {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int LONGITUD_MAXIMA_EMAIL = 120;

    private Validaciones() {
    }

    public static String requerido(String valor, String campo, int minimo, int maximo) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.isEmpty()) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
        if (texto.length() < minimo) {
            throw new ValidacionException("El campo \"" + campo + "\" debe tener al menos " + minimo + " caracteres.");
        }
        if (texto.length() > maximo) {
            throw new ValidacionException("El campo \"" + campo + "\" no puede superar " + maximo + " caracteres.");
        }
        return texto;
    }

    /** @return el texto normalizado, o {@code null} si está vacío. */
    public static String opcional(String valor, String campo, int maximo) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() > maximo) {
            throw new ValidacionException("El campo \"" + campo + "\" no puede superar " + maximo + " caracteres.");
        }
        return texto;
    }

    public static String email(String valor, String campo) {
        String texto = opcional(valor, campo, LONGITUD_MAXIMA_EMAIL);
        if (texto != null && !EMAIL.matcher(texto).matches()) {
            throw new ValidacionException("El campo \"" + campo + "\" no tiene un formato de correo válido.");
        }
        return texto;
    }
}
