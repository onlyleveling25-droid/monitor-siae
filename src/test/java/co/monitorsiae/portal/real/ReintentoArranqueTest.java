package co.monitorsiae.portal.real;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReintentoArranqueTest {

    /** Mismo nombre simple que el error interno de Playwright cuando el navegador se cierra solo. */
    static final class TargetClosedError extends RuntimeException {
        TargetClosedError() {
            super("Target page, context or browser has been closed");
        }
    }

    private final List<Duration> esperas = new ArrayList<>();
    private final ReintentoArranque.Pausa pausa = esperas::add;

    @Test
    void siAbreALaPrimeraNoEspera() {
        assertEquals("ok", ReintentoArranque.ejecutar(() -> "ok", pausa));
        assertTrue(esperas.isEmpty());
    }

    @Test
    void siSeCierraAlArrancarEsperaYReintentaUnaVez() {
        AtomicInteger intentos = new AtomicInteger();

        String resultado = ReintentoArranque.ejecutar(() -> {
            if (intentos.incrementAndGet() == 1) {
                throw new TargetClosedError();
            }
            return "ok";
        }, pausa);

        assertEquals("ok", resultado);
        assertEquals(2, intentos.get());
        assertEquals(List.of(ReintentoArranque.ESPERA), esperas);
    }

    @Test
    void siFallaDosVecesNoHayTercerIntento() {
        AtomicInteger intentos = new AtomicInteger();

        TargetClosedError error = assertThrows(TargetClosedError.class,
                () -> ReintentoArranque.ejecutar(() -> {
                    intentos.incrementAndGet();
                    throw new TargetClosedError();
                }, pausa));

        assertEquals(2, intentos.get());
        assertEquals(1, error.getSuppressed().length, "conserva el primer fallo para el registro");
    }

    @Test
    void otrosErroresNoSeReintentan() {
        AtomicInteger intentos = new AtomicInteger();
        IllegalStateException noInstalado = new IllegalStateException("Executable doesn't exist");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ReintentoArranque.ejecutar(() -> {
                    intentos.incrementAndGet();
                    throw noInstalado;
                }, pausa));

        assertSame(noInstalado, error);
        assertEquals(1, intentos.get());
        assertTrue(esperas.isEmpty());
    }

    @Test
    void siLaEsperaSeInterrumpeNoReintentaYConservaLaMarca() {
        AtomicInteger intentos = new AtomicInteger();
        ReintentoArranque.Pausa interrumpida = tiempo -> {
            throw new InterruptedException();
        };

        try {
            assertThrows(TargetClosedError.class, () -> ReintentoArranque.ejecutar(() -> {
                intentos.incrementAndGet();
                throw new TargetClosedError();
            }, interrumpida));
            assertEquals(1, intentos.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void reconoceElCierreAunqueVengaEnvueltoEnOtraExcepcion() {
        assertTrue(ReintentoArranque.seCerroAlArrancar(new RuntimeException("envoltura", new TargetClosedError())));
        assertFalse(ReintentoArranque.seCerroAlArrancar(new RuntimeException("otro")));
        assertFalse(ReintentoArranque.seCerroAlArrancar(null));
    }
}
