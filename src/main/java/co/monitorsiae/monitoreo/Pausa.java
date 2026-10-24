package co.monitorsiae.monitoreo;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Espera entre ciclos. Es una interfaz para que las pruebas no tengan que esperar de verdad.
 */
@FunctionalInterface
public interface Pausa {

    void esperar(Duration duracion) throws InterruptedException;

    /** Despierta una espera activa, si la implementacion admite cancelacion. */
    default void despertar() {
    }

    /**
     * Variacion aleatoria de hasta el {@code factor} indicado (por ejemplo 0.2 = ±20%) sobre la
     * duracion base, para que las esperas entre ciclos y reintentos no sigan un patron
     * predecible frente a la plataforma.
     */
    static Duration conJitter(Duration base, double factor) {
        if (base.isZero() || base.isNegative() || factor <= 0) {
            return base;
        }
        double variacion = ThreadLocalRandom.current().nextDouble(-factor, factor);
        long nanosConJitter = Math.round(base.toNanos() * (1.0 + variacion));
        return Duration.ofNanos(Math.max(0, nanosConJitter));
    }

    static Pausa real() {
        return new Pausa() {
            private static final double FACTOR_JITTER = 0.2;

            private final Object candado = new Object();
            private boolean despertarSolicitado;

            @Override
            public void esperar(Duration duracionBase) throws InterruptedException {
                Duration duracion = conJitter(duracionBase, FACTOR_JITTER);
                synchronized (candado) {
                    if (despertarSolicitado) {
                        despertarSolicitado = false;
                        return;
                    }
                    long limite = System.nanoTime() + duracion.toNanos();
                    long restante = limite - System.nanoTime();
                    while (!despertarSolicitado && restante > 0) {
                        long milisegundos = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(restante);
                        int nanosegundos = (int) (restante
                                - java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(milisegundos));
                        candado.wait(milisegundos, nanosegundos);
                        restante = limite - System.nanoTime();
                    }
                    despertarSolicitado = false;
                }
            }

            @Override
            public void despertar() {
                synchronized (candado) {
                    despertarSolicitado = true;
                    candado.notifyAll();
                }
            }
        };
    }
}
