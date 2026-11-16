package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaPasswordTestFGEM {

    @Test
    void aceptaUnaContrasenaValidaFGEM() {
        assertDoesNotThrow(() -> PoliticaPasswordFGEM.validarFGEM("Ventas2026", "Ventas2026", "maria"));
    }

    @Test
    void rechazaContrasenasDebilesFGEM() {
        assertMensajeFGEM("Ingresa la nueva contraseña.", null, null);
        assertMensajeFGEM("La contraseña debe tener al menos 8 caracteres.", "Ab1", "Ab1");
        assertMensajeFGEM("La contraseña debe combinar letras y números.", "solamenteletras", "solamenteletras");
        assertMensajeFGEM("La contraseña debe combinar letras y números.", "1234567890", "1234567890");
        assertMensajeFGEM("La contraseña no puede superar 128 caracteres.", "a1".repeat(65), "a1".repeat(65));
    }

    @Test
    void rechazaContrasenaIgualAlUsuarioFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> PoliticaPasswordFGEM.validarFGEM("Maria2026", "Maria2026", "maria2026"));
    }

    @Test
    void exigeQueLaConfirmacionCoincidaFGEM() {
        assertMensajeFGEM("Las contraseñas no coinciden.", "Ventas2026", "Ventas2027");
    }

    private static void assertMensajeFGEM(String esperadoFGEM, String passwordFGEM, String confirmacionFGEM) {
        ValidacionExceptionFGEM errorFGEM = assertThrows(ValidacionExceptionFGEM.class,
                () -> PoliticaPasswordFGEM.validarFGEM(passwordFGEM, confirmacionFGEM, "usuario"));
        assertEquals(esperadoFGEM, errorFGEM.getMessage());
    }
}
