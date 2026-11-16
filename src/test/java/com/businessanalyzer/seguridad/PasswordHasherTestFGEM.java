package com.businessanalyzer.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTestFGEM {

    private final PasswordHasherFGEM hasherFGEM = new PasswordHasherFGEM(1_000);

    @Test
    void verificaLaContrasenaCorrectaYRechazaLaIncorrectaFGEM() {
        String hashFGEM = hasherFGEM.hashearFGEM("Secreta123".toCharArray());

        assertTrue(hasherFGEM.verificarFGEM("Secreta123".toCharArray(), hashFGEM));
        assertFalse(hasherFGEM.verificarFGEM("secreta123".toCharArray(), hashFGEM));
        assertFalse(hasherFGEM.verificarFGEM("".toCharArray(), hashFGEM));
    }

    @Test
    void noGuardaLaContrasenaEnTextoPlanoYUsaSalDistintaFGEM() {
        String primeroFGEM = hasherFGEM.hashearFGEM("Secreta123".toCharArray());
        String segundoFGEM = hasherFGEM.hashearFGEM("Secreta123".toCharArray());

        assertFalse(primeroFGEM.contains("Secreta123"));
        assertTrue(primeroFGEM.startsWith("pbkdf2_sha256$1000$"));
        assertNotEquals(primeroFGEM, segundoFGEM, "Cada hash debe tener su propia sal");
    }

    @Test
    void unHashMalformadoNuncaEsValidoFGEM() {
        assertFalse(hasherFGEM.verificarFGEM("x1234567".toCharArray(), null));
        assertFalse(hasherFGEM.verificarFGEM("x1234567".toCharArray(), "texto-plano"));
        assertFalse(hasherFGEM.verificarFGEM("x1234567".toCharArray(), "pbkdf2_sha256$abc$!!$!!"));
        assertFalse(hasherFGEM.verificarFGEM("x1234567".toCharArray(), "md5$1$a$b"));
    }

    @Test
    void detectaHashesQueDebenActualizarseFGEM() {
        String hashViejoFGEM = new PasswordHasherFGEM(500).hashearFGEM("Secreta123".toCharArray());

        assertTrue(hasherFGEM.requiereActualizacionFGEM(hashViejoFGEM));
        assertFalse(hasherFGEM.requiereActualizacionFGEM(hasherFGEM.hashearFGEM("Secreta123".toCharArray())));
        assertTrue(hasherFGEM.verificarFGEM("Secreta123".toCharArray(), hashViejoFGEM),
                "Un hash con otras iteraciones debe seguir siendo verificable");
    }

    @Test
    void laConfiguracionPorDefectoSigueLaRecomendacionDeOwaspFGEM() {
        assertTrue(PasswordHasherFGEM.ITERACIONES_POR_DEFECTO_FGEM >= 600_000);
    }
}
