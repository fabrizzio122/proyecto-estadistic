package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;

public final class PoliticaPasswordFGEM {

    public static final int LONGITUD_MINIMA_FGEM = 8;
    public static final int LONGITUD_MAXIMA_FGEM = 128;
    public static final String DESCRIPCION_FGEM =
            "Mínimo " + LONGITUD_MINIMA_FGEM + " caracteres, combinando letras y números.";

    private PoliticaPasswordFGEM() {
    }

    public static void validarFGEM(String passwordFGEM, String confirmacionFGEM, String nombreUsuarioFGEM) {
        if (passwordFGEM == null || passwordFGEM.isEmpty()) {
            throw new ValidacionExceptionFGEM("Ingresa la nueva contraseña.");
        }
        if (passwordFGEM.length() < LONGITUD_MINIMA_FGEM) {
            throw new ValidacionExceptionFGEM("La contraseña debe tener al menos " + LONGITUD_MINIMA_FGEM + " caracteres.");
        }
        if (passwordFGEM.length() > LONGITUD_MAXIMA_FGEM) {
            throw new ValidacionExceptionFGEM("La contraseña no puede superar " + LONGITUD_MAXIMA_FGEM + " caracteres.");
        }
        if (passwordFGEM.chars().noneMatch(Character::isLetter) || passwordFGEM.chars().noneMatch(Character::isDigit)) {
            throw new ValidacionExceptionFGEM("La contraseña debe combinar letras y números.");
        }
        if (nombreUsuarioFGEM != null && passwordFGEM.equalsIgnoreCase(nombreUsuarioFGEM.strip())) {
            throw new ValidacionExceptionFGEM("La contraseña no puede ser igual al nombre de usuario.");
        }
        if (!passwordFGEM.equals(confirmacionFGEM)) {
            throw new ValidacionExceptionFGEM("Las contraseñas no coinciden.");
        }
    }
}
