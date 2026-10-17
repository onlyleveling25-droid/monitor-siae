package co.monitorsiae.monitoreo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PausaTest {

    @Test
    void despertarInterrumpeUnaEsperaActiva() throws InterruptedException {
        Pausa pausa = Pausa.real();
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch terminada = new CountDownLatch(1);
        Thread hilo = Thread.ofPlatform().start(() -> {
            iniciada.countDown();
            try {
                pausa.esperar(Duration.ofHours(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                terminada.countDown();
            }
        });

        assertTrue(iniciada.await(1, TimeUnit.SECONDS));
        pausa.despertar();

        assertTrue(terminada.await(1, TimeUnit.SECONDS));
        hilo.join();
    }
}
