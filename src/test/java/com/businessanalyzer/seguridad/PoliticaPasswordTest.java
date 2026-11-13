package com.businessanalyzer.seguridad;

import com.businessanalyzer.exception.ValidacionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaPasswordTest {

    @Test
    void aceptaUnaContrasenaValida() {
        assertDoesNotThrow(() -> PoliticaPassword.validar("Ventas2026", "Ventas2026", "maria"));
    }

    @Test
    void rechazaContrasenasDebiles() {
        assertMensaje("Ingresa la nueva contraseña.", null, null);
        assertMensaje("La contraseña debe tener al menos 8 caracteres.", "Ab1", "Ab1");
        assertMensaje("La contraseña debe combinar letras y números.", "solamenteletras", "solamenteletras");
        assertMensaje("La contraseña debe combinar letras y números.", "1234567890", "1234567890");
        assertMensaje("La contraseña no puede superar 128 caracteres.", "a1".repeat(65), "a1".repeat(65));
    }

    @Test
    void rechazaContrasenaIgualAlUsuario() {
        assertThrows(ValidacionException.class, () -> PoliticaPassword.validar("Maria2026", "Maria2026", "maria2026"));
    }

    @Test
    void exigeQueLaConfirmacionCoincida() {
        assertMensaje("Las contraseñas no coinciden.", "Ventas2026", "Ventas2027");
    }

    private static void assertMensaje(String esperado, String password, String confirmacion) {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> PoliticaPassword.validar(password, confirmacion, "usuario"));
        assertEquals(esperado, error.getMessage());
    }
}
