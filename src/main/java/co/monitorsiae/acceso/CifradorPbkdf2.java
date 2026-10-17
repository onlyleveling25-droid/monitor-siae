package co.monitorsiae.acceso;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Deriva contrasenas con PBKDF2-HMAC-SHA256 y una sal aleatoria.
 *
 * <p>Formato guardado: {@code pbkdf2$<iteraciones>$<sal base64>$<derivada base64>}. Las
 * iteraciones viajan con el valor, asi que aumentarlas en el futuro no invalida las contrasenas
 * existentes.
 */
public final class CifradorPbkdf2 implements CifradorContrasenas {

    /** Recomendacion de OWASP para PBKDF2-HMAC-SHA256. */
    public static final int ITERACIONES_POR_DEFECTO = 600_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2";
    private static final int BYTES_SAL = 16;
    private static final int BITS_DERIVADA = 256;

    private final int iteraciones;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorPbkdf2() {
        this(ITERACIONES_POR_DEFECTO);
    }

    /** Permite menos iteraciones en las pruebas para que no tarden. */
    public CifradorPbkdf2(int iteraciones) {
        if (iteraciones < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas.");
        }
        this.iteraciones = iteraciones;
    }

    @Override
    public String derivar(char[] contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        aleatorio.nextBytes(sal);
        byte[] derivada = calcular(contrasena, sal, iteraciones);

        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIJO + "$" + iteraciones + "$" + base64.encodeToString(sal) + "$" + base64.encodeToString(derivada);
    }

    @Override
    public boolean coincide(char[] intento, String guardada) {
        String[] partes = guardada == null ? new String[0] : guardada.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteracionesGuardadas = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperada = Base64.getDecoder().decode(partes[3]);
            byte[] obtenida = calcular(intento, sal, iteracionesGuardadas);
            // Comparacion de tiempo constante: no revela cuantos bytes coinciden.
            return MessageDigest.isEqual(esperada, obtenida);
        } catch (IllegalArgumentException formatoInvalido) {
            return false;
        }
    }

    private static byte[] calcular(char[] contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, iteraciones, BITS_DERIVADA);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("La plataforma Java no soporta " + ALGORITMO, e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
