package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.TonoAlarma;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Hace sonar la sirena en bucle mientras haya alertas activas. Si alguna es una oportunidad, suena
 * la sirena aguda; si solo hay problemas, la grave.
 */
public final class CanalSonido implements CanalAlerta {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CanalSonido.class);
    private static final AudioFormat FORMATO =
            new AudioFormat(GeneradorSirena.FRECUENCIA_MUESTREO, 16, 1, true, false);

    private final Map<TonoAlarma, byte[]> sirenasOportunidad = new EnumMap<>(TonoAlarma.class);
    private final Map<TonoAlarma, byte[]> sirenasProblema = new EnumMap<>(TonoAlarma.class);
    private final Supplier<Configuracion> configuracion;

    private volatile List<Alerta> activas = List.of();
    private volatile SourceDataLine lineaActiva;
    private volatile TonoAlarma tonoActual = TonoAlarma.SIRENA;
    private Thread hilo;

    public CanalSonido() {
        this(Configuracion::porDefecto);
    }

    public CanalSonido(Supplier<Configuracion> configuracion) {
        this.configuracion = configuracion;
        for (TonoAlarma tono : TonoAlarma.values()) {
            sirenasOportunidad.put(tono, GeneradorSirena.ciclo(Gravedad.OPORTUNIDAD, tono));
            sirenasProblema.put(tono, GeneradorSirena.ciclo(Gravedad.PROBLEMA, tono));
        }
    }

    @Override
    public synchronized void mostrar(List<Alerta> nuevas) {
        Configuracion actual = configuracion.get();
        tonoActual = actual.preferenciasAlertas().tono();
        activas = filtrarPorPreferencia(actual, nuevas);
        if (activas.isEmpty() && lineaActiva != null) {
            lineaActiva.flush();
        }
        if (!activas.isEmpty() && hilo == null) {
            hilo = Thread.ofPlatform().daemon().name("alarma-sonido").start(this::sonar);
        }
    }

    private void sonar() {
        try (SourceDataLine linea = AudioSystem.getSourceDataLine(FORMATO)) {
            linea.open(FORMATO);
            linea.start();
            synchronized (this) {
                lineaActiva = linea;
            }
            while (!terminarSiNoHayAlertas()) {
                byte[] sirena = hayOportunidad()
                        ? sirenasOportunidad.get(tonoActual)
                        : sirenasProblema.get(tonoActual);
                linea.write(sirena, 0, sirena.length);
            }
            linea.stop();
            linea.flush();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            REGISTRO.warn("No hay salida de audio disponible para la alarma: {}", e.getMessage());
        } finally {
            synchronized (this) {
                lineaActiva = null;
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

    public static void probar(TonoAlarma tono) {
        Thread.ofPlatform().daemon().name("previsualizacion-alarma").start(() -> {
            try (SourceDataLine linea = AudioSystem.getSourceDataLine(FORMATO)) {
                linea.open(FORMATO);
                linea.start();
                byte[] audio = GeneradorSirena.previsualizacion(tono, 3);
                linea.write(audio, 0, audio.length);
                linea.stop();
            } catch (LineUnavailableException | IllegalArgumentException e) {
                REGISTRO.warn("No hay salida de audio disponible para previsualizar la alarma: {}", e.getMessage());
            }
        });
    }

    static List<Alerta> filtrarPorPreferencia(Configuracion actual, List<Alerta> alertas) {
        return alertas.stream().filter(alerta -> sonidoHabilitado(actual, alerta.clienteId())).toList();
    }

    private static boolean sonidoHabilitado(Configuracion actual, String clienteId) {
        return actual.clientes().stream()
                .filter(cliente -> cliente.id().equals(clienteId))
                .findFirst()
                .map(cliente -> cliente.preferenciaSonido().habilitado(
                        actual.preferenciasAlertas().sonidoHabilitado()))
                .orElse(actual.preferenciasAlertas().sonidoHabilitado());
    }
}
