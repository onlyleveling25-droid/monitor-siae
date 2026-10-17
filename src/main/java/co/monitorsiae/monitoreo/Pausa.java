package co.monitorsiae.monitoreo;

import java.time.Duration;

/**
 * Espera entre ciclos. Es una interfaz para que las pruebas no tengan que esperar de verdad.
 */
@FunctionalInterface
public interface Pausa {

    void esperar(Duration duracion) throws InterruptedException;

    static Pausa real() {
        return duracion -> Thread.sleep(duracion.toMillis());
    }
}
