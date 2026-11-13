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
 * (ver {@link #requiereActualizacion(String)}).
 */
public final class PasswordHasher {

    public static final int ITERACIONES_POR_DEFECTO = 600_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;

    private final int iteraciones;
    private final SecureRandom aleatorio = new SecureRandom();

    public PasswordHasher() {
        this(ITERACIONES_POR_DEFECTO);
    }

    /** Permite menos iteraciones solo para pruebas automatizadas rápidas. */
    public PasswordHasher(int iteraciones) {
        if (iteraciones < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        this.iteraciones = iteraciones;
    }

    public String hashear(char[] password) {
        byte[] sal = new byte[BYTES_SAL];
        aleatorio.nextBytes(sal);
        byte[] hash = derivar(password, sal, iteraciones, BITS_HASH);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIJO + "$" + iteraciones + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public boolean verificar(char[] password, String almacenado) {
        if (password == null || password.length == 0 || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteracionesGuardadas = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(password, sal, iteracionesGuardadas, esperado.length * 8);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** {@code true} si el hash se generó con otra configuración y conviene regenerarlo. */
    public boolean requiereActualizacion(String almacenado) {
        String[] partes = almacenado == null ? new String[0] : almacenado.split("\\$");
        return partes.length != 4 || !PREFIJO.equals(partes[0])
                || !String.valueOf(iteraciones).equals(partes[1]);
    }

    private static byte[] derivar(char[] password, byte[] sal, int iteraciones, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        } finally {
            spec.clearPassword();
        }
    }
}
