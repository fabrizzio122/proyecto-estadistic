package com.businessanalyzer.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    void verificaLaContrasenaCorrectaYRechazaLaIncorrecta() {
        String hash = hasher.hashear("Secreta123".toCharArray());

        assertTrue(hasher.verificar("Secreta123".toCharArray(), hash));
        assertFalse(hasher.verificar("secreta123".toCharArray(), hash));
        assertFalse(hasher.verificar("".toCharArray(), hash));
    }

    @Test
    void noGuardaLaContrasenaEnTextoPlanoYUsaSalDistinta() {
        String primero = hasher.hashear("Secreta123".toCharArray());
        String segundo = hasher.hashear("Secreta123".toCharArray());

        assertFalse(primero.contains("Secreta123"));
        assertTrue(primero.startsWith("pbkdf2_sha256$1000$"));
        assertNotEquals(primero, segundo, "Cada hash debe tener su propia sal");
    }

    @Test
    void unHashMalformadoNuncaEsValido() {
        assertFalse(hasher.verificar("x1234567".toCharArray(), null));
        assertFalse(hasher.verificar("x1234567".toCharArray(), "texto-plano"));
        assertFalse(hasher.verificar("x1234567".toCharArray(), "pbkdf2_sha256$abc$!!$!!"));
        assertFalse(hasher.verificar("x1234567".toCharArray(), "md5$1$a$b"));
    }

    @Test
    void detectaHashesQueDebenActualizarse() {
        String hashViejo = new PasswordHasher(500).hashear("Secreta123".toCharArray());

        assertTrue(hasher.requiereActualizacion(hashViejo));
        assertFalse(hasher.requiereActualizacion(hasher.hashear("Secreta123".toCharArray())));
        assertTrue(hasher.verificar("Secreta123".toCharArray(), hashViejo),
                "Un hash con otras iteraciones debe seguir siendo verificable");
    }

    @Test
    void laConfiguracionPorDefectoSigueLaRecomendacionDeOwasp() {
        assertTrue(PasswordHasher.ITERACIONES_POR_DEFECTO >= 600_000);
    }
}
