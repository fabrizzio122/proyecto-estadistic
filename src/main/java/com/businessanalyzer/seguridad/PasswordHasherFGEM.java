package com.businessanalyzer.seguridad;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 (incluido en Java, sin dependencias externas).
 * <p>
 * Formato almacenado: {@code pbkdf2_sha256$<iteraciones>$<sal Base64>$<hash Base64>}.
 * Cada contraseña tiene su propia sal aleatoria de 16 bytes, por lo que dos usuarios con la
 * misma contraseña tienen hashes distintos. La comparación se hace en tiempo constante.
 * <p>
 * Las iteraciones siguen la recomendación de OWASP para PBKDF2-HMAC-SHA256. Si en el futuro
 * se aumentan, los hashes antiguos se actualizan solos en el siguiente inicio de sesión
 * (ver {@link #requiereActualizacionFGEM(String)}).
 */
public final class PasswordHasherFGEM {

    public static final int ITERACIONES_POR_DEFECTO_FGEM = 600_000;

    private static final String ALGORITMO_FGEM = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO_FGEM = "pbkdf2_sha256";
    private static final int BYTES_SAL_FGEM = 16;
    private static final int BITS_HASH_FGEM = 256;

    private final int iteracionesFGEM;
    private final SecureRandom aleatorioFGEM = new SecureRandom();

    public PasswordHasherFGEM() {
        this(ITERACIONES_POR_DEFECTO_FGEM);
    }

    /** Permite menos iteraciones solo para pruebas automatizadas rápidas. */
    public PasswordHasherFGEM(int iteracionesFGEM) {
        if (iteracionesFGEM < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        this.iteracionesFGEM = iteracionesFGEM;
    }

    public String hashearFGEM(char[] passwordFGEM) {
        byte[] salFGEM = new byte[BYTES_SAL_FGEM];
        aleatorioFGEM.nextBytes(salFGEM);
        byte[] hashFGEM = derivarFGEM(passwordFGEM, salFGEM, iteracionesFGEM, BITS_HASH_FGEM);
        Base64.Encoder b64FGEM = Base64.getEncoder();
        return PREFIJO_FGEM + "$" + iteracionesFGEM + "$" + b64FGEM.encodeToString(salFGEM) + "$" + b64FGEM.encodeToString(hashFGEM);
    }

    public boolean verificarFGEM(char[] passwordFGEM, String almacenadoFGEM) {
        if (passwordFGEM == null || passwordFGEM.length == 0 || almacenadoFGEM == null) {
            return false;
        }
        String[] partesFGEM = almacenadoFGEM.split("\\$");
        if (partesFGEM.length != 4 || !PREFIJO_FGEM.equals(partesFGEM[0])) {
            return false;
        }
        try {
            int iteracionesGuardadasFGEM = Integer.parseInt(partesFGEM[1]);
            byte[] salFGEM = Base64.getDecoder().decode(partesFGEM[2]);
            byte[] esperadoFGEM = Base64.getDecoder().decode(partesFGEM[3]);
            byte[] calculadoFGEM = derivarFGEM(passwordFGEM, salFGEM, iteracionesGuardadasFGEM, esperadoFGEM.length * 8);
            return MessageDigest.isEqual(esperadoFGEM, calculadoFGEM);
        } catch (IllegalArgumentException eFGEM) {
            return false;
        }
    }

    /** {@code true} si el hash se generó con otra configuración y conviene regenerarlo. */
    public boolean requiereActualizacionFGEM(String almacenadoFGEM) {
        String[] partesFGEM = almacenadoFGEM == null ? new String[0] : almacenadoFGEM.split("\\$");
        return partesFGEM.length != 4 || !PREFIJO_FGEM.equals(partesFGEM[0])
                || !String.valueOf(iteracionesFGEM).equals(partesFGEM[1]);
    }

    private static byte[] derivarFGEM(char[] passwordFGEM, byte[] salFGEM, int iteracionesFGEM, int bitsFGEM) {
        PBEKeySpec specFGEM = new PBEKeySpec(passwordFGEM, salFGEM, iteracionesFGEM, bitsFGEM);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO_FGEM).generateSecret(specFGEM).getEncoded();
        } catch (GeneralSecurityException eFGEM) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", eFGEM);
        } finally {
            specFGEM.clearPassword();
        }
    }
}
