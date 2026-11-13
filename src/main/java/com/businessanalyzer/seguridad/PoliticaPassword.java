package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.ValidacionException;

/** Reglas que debe cumplir toda contraseña nueva. */
public final class PoliticaPassword {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 128;
    public static final String DESCRIPCION =
            "Mínimo " + LONGITUD_MINIMA + " caracteres, combinando letras y números.";

    private PoliticaPassword() {
    }

    /**
     * @param confirmacion la contraseña escrita por segunda vez
     * @param nombreUsuario para impedir que la contraseña sea igual al usuario
     */
    public static void validar(String password, String confirmacion, String nombreUsuario) {
        if (password == null || password.isEmpty()) {
            throw new ValidacionException("Ingresa la nueva contraseña.");
        }
        if (password.length() < LONGITUD_MINIMA) {
            throw new ValidacionException("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        if (password.length() > LONGITUD_MAXIMA) {
            throw new ValidacionException("La contraseña no puede superar " + LONGITUD_MAXIMA + " caracteres.");
        }
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            throw new ValidacionException("La contraseña debe combinar letras y números.");
        }
        if (nombreUsuario != null && password.equalsIgnoreCase(nombreUsuario.strip())) {
            throw new ValidacionException("La contraseña no puede ser igual al nombre de usuario.");
        }
        if (!password.equals(confirmacion)) {
            throw new ValidacionException("Las contraseñas no coinciden.");
        }
    }
}
