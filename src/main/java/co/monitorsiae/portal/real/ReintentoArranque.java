package co.monitorsiae.portal.real;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Reintenta una vez la apertura del navegador cuando este arranca y se cierra solo de inmediato.
 *
 * <p>Pasa, por ejemplo, mientras Edge o Chrome aplican una actualización, o si se reabre el
 * navegador de un cliente justo después de cerrarlo. Otros errores (navegador no instalado,
 * perfil inválido) no se reintentan: un segundo intento no los arregla.
 */
final class ReintentoArranque {

    /** Espera antes del segundo intento: da tiempo a que el navegador termine lo que estaba haciendo. */
    static final Duration ESPERA = Duration.ofSeconds(5);

    @FunctionalInterface
    interface Pausa {
        void esperar(Duration tiempo) throws InterruptedException;
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ReintentoArranque.class);

    private ReintentoArranque() {
    }

    static <T> T ejecutar(Supplier<T> intento, Pausa pausa) {
        Objects.requireNonNull(intento);
        Objects.requireNonNull(pausa);
        try {
            return intento.get();
        } catch (RuntimeException primero) {
            if (!seCerroAlArrancar(primero)) {
                throw primero;
            }
            LOG.warn("El navegador se cerró al arrancar; se reintenta en {} s.", ESPERA.toSeconds());
            try {
                pausa.esperar(ESPERA);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw primero;
            }
            try {
                T resultado = intento.get();
                LOG.info("El navegador abrió en el segundo intento.");
                return resultado;
            } catch (RuntimeException segundo) {
                segundo.addSuppressed(primero);
                throw segundo;
            }
        }
    }

    /**
     * Verdadero si el error indica que el navegador arrancó y se cerró solo. Playwright lo reporta
     * como {@code TargetClosedError}, una clase interna suya; por eso se reconoce por su nombre.
     */
    static boolean seCerroAlArrancar(Throwable error) {
        // El límite de niveles evita un ciclo infinito si alguna causa se apunta a sí misma.
        Throwable actual = error;
        for (int nivel = 0; actual != null && nivel < 10; nivel++, actual = actual.getCause()) {
            if ("TargetClosedError".equals(actual.getClass().getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}
