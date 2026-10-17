package co.monitorsiae.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.List;

/**
 * Hace sonar la sirena en bucle mientras haya alertas activas. Si alguna es una oportunidad, suena
 * la sirena aguda; si solo hay problemas, la grave.
 */
public final class CanalSonido implements CanalAlerta {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CanalSonido.class);
    private static final AudioFormat FORMATO =
            new AudioFormat(GeneradorSirena.FRECUENCIA_MUESTREO, 16, 1, true, false);

    private final byte[] sirenaOportunidad = GeneradorSirena.ciclo(Gravedad.OPORTUNIDAD);
    private final byte[] sirenaProblema = GeneradorSirena.ciclo(Gravedad.PROBLEMA);

    private volatile List<Alerta> activas = List.of();
    private Thread hilo;

    @Override
    public synchronized void mostrar(List<Alerta> nuevas) {
        activas = List.copyOf(nuevas);
        if (!activas.isEmpty() && hilo == null) {
            hilo = Thread.ofPlatform().daemon().name("alarma-sonido").start(this::sonar);
        }
    }

    private void sonar() {
        try (SourceDataLine linea = AudioSystem.getSourceDataLine(FORMATO)) {
            linea.open(FORMATO);
            linea.start();
            while (!terminarSiNoHayAlertas()) {
                byte[] sirena = hayOportunidad() ? sirenaOportunidad : sirenaProblema;
                linea.write(sirena, 0, sirena.length);
            }
            linea.stop();
            linea.flush();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            REGISTRO.warn("No hay salida de audio disponible para la alarma: {}", e.getMessage());
            synchronized (this) {
                hilo = null;
            }
        }
    }

    private synchronized boolean terminarSiNoHayAlertas() {
        if (activas.isEmpty()) {
            hilo = null;
            return true;
        }
        return false;
    }

    private boolean hayOportunidad() {
        return activas.stream().anyMatch(alerta -> alerta.gravedad() == Gravedad.OPORTUNIDAD);
    }
}
