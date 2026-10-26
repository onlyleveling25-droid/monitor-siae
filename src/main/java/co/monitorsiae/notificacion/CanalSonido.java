package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.NivelEscalamiento;
import co.monitorsiae.dominio.PerfilEscalamiento;
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
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Hace sonar la sirena en bucle mientras haya alertas activas. Si alguna es una oportunidad, suena
 * la sirena aguda; si solo hay problemas, la grave.
 *
 * <p>Si una alerta silenciada lleva activa lo suficiente, el perfil de escalamiento puede forzar el
 * sonido igual (ver {@link PerfilEscalamiento}); por defecto el escalamiento esta deshabilitado y el
 * comportamiento es identico al previo.
 */
public final class CanalSonido implements CanalAlerta {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CanalSonido.class);
    private static final AudioFormat FORMATO =
            new AudioFormat(GeneradorSirena.FRECUENCIA_MUESTREO, 16, 1, true, false);

    private final Map<TonoAlarma, byte[]> sirenasOportunidad = new EnumMap<>(TonoAlarma.class);
    private final Map<TonoAlarma, byte[]> sirenasProblema = new EnumMap<>(TonoAlarma.class);
    private final Supplier<Configuracion> configuracion;
    private final MedidorTiempoActivo tiempoActivo;

    private volatile List<Alerta> activas = List.of();
    private volatile SourceDataLine lineaActiva;
    private volatile TonoAlarma tonoActual = TonoAlarma.SIRENA;
    private Thread hilo;

    public CanalSonido() {
        this(Configuracion::porDefecto);
    }

    public CanalSonido(Supplier<Configuracion> configuracion) {
        this(configuracion, MedidorTiempoActivo.SIN_TIEMPO);
    }

    public CanalSonido(Supplier<Configuracion> configuracion, MedidorTiempoActivo tiempoActivo) {
        this.configuracion = configuracion;
        this.tiempoActivo = tiempoActivo;
        for (TonoAlarma tono : TonoAlarma.values()) {
            sirenasOportunidad.put(tono, GeneradorSirena.ciclo(Gravedad.OPORTUNIDAD, tono));
            sirenasProblema.put(tono, GeneradorSirena.ciclo(Gravedad.PROBLEMA, tono));
        }
    }

    @Override
    public synchronized void mostrar(List<Alerta> nuevas) {
        Configuracion actual = configuracion.get();
        PerfilEscalamiento perfil = actual.preferenciasAlertas().perfilEscalamiento();
        List<Alerta> audibles = filtrarPorPreferencia(actual, nuevas);
        List<Alerta> forzadasPorEscalamiento = nuevas.stream()
                .filter(alerta -> !audibles.contains(alerta))
                .filter(alerta -> nivelVigente(perfil, alerta).map(NivelEscalamiento::forzarSonido).orElse(false))
                .toList();
        activas = forzadasPorEscalamiento.isEmpty() ? audibles
                : java.util.stream.Stream.concat(audibles.stream(), forzadasPorEscalamiento.stream()).toList();
        tonoActual = tonoMasUrgente(perfil, activas).orElse(actual.preferenciasAlertas().tono());
        if (activas.isEmpty() && lineaActiva != null) {
            lineaActiva.flush();
        }
        if (!activas.isEmpty() && hilo == null) {
            hilo = Thread.ofPlatform().daemon().name("alarma-sonido").start(this::sonar);
        }
    }

    /** Para las pruebas: que alertas estan sonando (o deberian sonar) en este momento. */
    synchronized List<Alerta> alertasAudiblesActualmente() {
        return activas;
    }

    private Optional<NivelEscalamiento> nivelVigente(PerfilEscalamiento perfil, Alerta alerta) {
        return perfil.nivelVigente(tiempoActivo.tiempoActivo(alerta.clienteId()));
    }

    /** El tono del nivel de escalamiento mas insistente entre las alertas activas, si alguno aplica. */
    private Optional<TonoAlarma> tonoMasUrgente(PerfilEscalamiento perfil, List<Alerta> activas) {
        return activas.stream()
                .map(alerta -> nivelVigente(perfil, alerta))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(java.util.Comparator.comparing(NivelEscalamiento::espera))
                .map(NivelEscalamiento::tono);
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
