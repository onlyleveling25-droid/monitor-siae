package co.monitorsiae.monitoreo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PausaTest {

    @Test
    void elJitterVariaDentroDelFactorPeroNoSiempreEsIgualALaBase() {
        Duration base = Duration.ofSeconds(10);
        boolean huboVariacion = false;
        for (int i = 0; i < 50; i++) {
            Duration conJitter = Pausa.conJitter(base, 0.2);
            assertTrue(conJitter.compareTo(base.multipliedBy(80).dividedBy(100)) >= 0);
            assertTrue(conJitter.compareTo(base.multipliedBy(120).dividedBy(100)) <= 0);
            if (!conJitter.equals(base)) {
                huboVariacion = true;
            }
        }
        assertTrue(huboVariacion, "El jitter nunca varió en 50 intentos.");
    }

    @Test
    void elJitterConFactorCeroNoAlteraLaDuracion() {
        Duration base = Duration.ofSeconds(10);
        assertFalse(Pausa.conJitter(base, 0).compareTo(base) != 0);
    }

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

    @Test
    void elJitterRespetaElFactorIndicadoYConCeroNoVaria() {
        Duration base = Duration.ofSeconds(180);

        for (int i = 0; i < 500; i++) {
            long segundos = Pausa.conJitter(base, 0.2).toSeconds();
            org.junit.jupiter.api.Assertions.assertTrue(segundos >= 143 && segundos <= 216, "20 %: " + segundos);
            long amplio = Pausa.conJitter(base, 0.5).toSeconds();
            org.junit.jupiter.api.Assertions.assertTrue(amplio >= 89 && amplio <= 270, "50 %: " + amplio);
        }
        org.junit.jupiter.api.Assertions.assertEquals(base, Pausa.conJitter(base, 0));
    }

    @Test
    void laPausaRealSinVariacionDuraLoPedido() throws InterruptedException {
        long inicio = System.nanoTime();
        Pausa.real(0).esperar(Duration.ofMillis(120));
        long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;

        org.junit.jupiter.api.Assertions.assertTrue(transcurridoMs >= 118 && transcurridoMs < 110 + 400,
                "duró " + transcurridoMs + " ms");
    }
}
