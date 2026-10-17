package co.monitorsiae.notificacion;

import co.monitorsiae.monitoreo.Pausa;

import java.time.Duration;
import java.util.List;

/**
 * Repite en voz alta cada alerta activa, una tras otra, hasta que todas se reconozcan.
 */
public final class CanalVoz implements CanalAlerta {

    public static final Duration PAUSA_ENTRE_RONDAS = Duration.ofSeconds(3);

    private final Locutor locutor;
    private final Pausa pausa;

    private volatile List<Alerta> activas = List.of();
    private Thread hilo;

    public CanalVoz(Locutor locutor, Pausa pausa) {
        this.locutor = locutor;
        this.pausa = pausa;
    }

    @Override
    public synchronized void mostrar(List<Alerta> nuevas) {
        activas = List.copyOf(nuevas);
        if (!activas.isEmpty() && hilo == null) {
            hilo = Thread.ofPlatform().daemon().name("alarma-voz").start(this::repetir);
        }
    }

    private void repetir() {
        try {
            while (true) {
                List<Alerta> ronda = activas;
                if (ronda.isEmpty()) {
                    if (terminarSiSigueVacio()) {
                        return;
                    }
                    continue;
                }
                for (Alerta alerta : ronda) {
                    // Se revisa antes de cada frase para callar en cuanto la persona reconozca la alerta.
                    if (activas.contains(alerta)) {
                        locutor.decir(alerta.textoVoz());
                    }
                }
                pausa.esperar(PAUSA_ENTRE_RONDAS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (this) {
                hilo = null;
            }
        }
    }

    /** Evita que una alerta que llega justo al terminar quede sin voz. */
    private synchronized boolean terminarSiSigueVacio() {
        if (activas.isEmpty()) {
            hilo = null;
            return true;
        }
        return false;
    }

    /** Para las pruebas: espera a que el hilo de voz termine. */
    void esperarSilencio(Duration limite) throws InterruptedException {
        Thread actual;
        synchronized (this) {
            actual = hilo;
        }
        if (actual != null) {
            actual.join(limite);
        }
    }
}
