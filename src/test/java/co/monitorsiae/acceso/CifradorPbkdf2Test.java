package co.monitorsiae.acceso;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CifradorPbkdf2Test {

    private final CifradorPbkdf2 cifrador = new CifradorPbkdf2(1_000);

    @Test
    void reconoceLaContrasenaCorrectaYRechazaLasDemas() {
        String guardada = cifrador.derivar("secreta123".toCharArray());

        assertTrue(cifrador.coincide("secreta123".toCharArray(), guardada));
        assertFalse(cifrador.coincide("secreta124".toCharArray(), guardada));
        assertFalse(cifrador.coincide("".toCharArray(), guardada));
    }

    @Test
    void nuncaGuardaLaContrasenaEnTextoPlano() {
        String guardada = cifrador.derivar("secreta123".toCharArray());

        assertFalse(guardada.contains("secreta123"));
        assertTrue(guardada.startsWith("pbkdf2$1000$"));
    }

    @Test
    void laMismaContrasenaProduceValoresDistintosPorLaSal() {
        assertNotEquals(cifrador.derivar("secreta123".toCharArray()), cifrador.derivar("secreta123".toCharArray()));
    }

    @Test
    void usaLasIteracionesGuardadasAunqueElCifradorUseOtras() {
        String guardadaConPocas = new CifradorPbkdf2(10).derivar("secreta123".toCharArray());

        assertTrue(cifrador.coincide("secreta123".toCharArray(), guardadaConPocas));
    }

    @Test
    void valoresMalFormadosNoCoinciden() {
        assertFalse(cifrador.coincide("x".toCharArray(), null));
        assertFalse(cifrador.coincide("x".toCharArray(), ""));
        assertFalse(cifrador.coincide("x".toCharArray(), "md5$abc"));
        assertFalse(cifrador.coincide("x".toCharArray(), "pbkdf2$mil$a$b"));
        assertFalse(cifrador.coincide("x".toCharArray(), "pbkdf2$10$no-es-base64$!!"));
    }
}
