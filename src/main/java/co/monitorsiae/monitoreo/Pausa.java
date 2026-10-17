package co.monitorsiae.monitoreo;

import java.time.Duration;

/**
 * Espera entre ciclos. Es una interfaz para que las pruebas no tengan que esperar de verdad.
 */
@FunctionalInterface
public interface Pausa {

    void esperar(Duration duracion) throws InterruptedException;

    /** Despierta una espera activa, si la implementacion admite cancelacion. */
    default void despertar() {
    }

    static Pausa real() {
        return new Pausa() {
            private final Object candado = new Object();
            private boolean despertarSolicitado;

            @Override
            public void esperar(Duration duracion) throws InterruptedException {
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
