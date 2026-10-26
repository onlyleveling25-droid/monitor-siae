package co.monitorsiae.notificacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lleva las alertas que aun no se reconocen y las reparte a todos los canales. Hay como maximo una
 * alerta por cliente: la mas reciente reemplaza a la anterior.
 *
 * <p>Segura entre hilos: la llaman los monitores de cada cliente y la interfaz grafica.
 *
 * <p>Tambien lleva, como {@link MedidorTiempoActivo}, desde cuando esta activa sin reconocerse la
 * alerta de cada cliente, para que los canales puedan aplicar el escalamiento de alarmas.
 */
public final class CentralAlertas implements MedidorTiempoActivo {

    private final List<CanalAlerta> canales;
    private final Map<String, Alerta> activas = new LinkedHashMap<>();
    private final Map<String, Instant> activadaEn = new LinkedHashMap<>();
    private final Clock reloj;

    public CentralAlertas(List<CanalAlerta> canales) {
        this(canales, Clock.systemDefaultZone());
    }

    public CentralAlertas(List<CanalAlerta> canales, Clock reloj) {
        this.canales = List.copyOf(canales);
        this.reloj = reloj;
    }

    public synchronized void emitir(Alerta alerta) {
        activas.remove(alerta.clienteId());
        activas.put(alerta.clienteId(), alerta);
        activadaEn.putIfAbsent(alerta.clienteId(), Instant.now(reloj));
        difundir();
    }

    /** La persona vio la alerta de este cliente: deja de sonar. */
    public synchronized void reconocer(String clienteId) {
        activadaEn.remove(clienteId);
        if (activas.remove(clienteId) != null) {
            difundir();
        }
    }

    public synchronized void reconocerTodas() {
        activadaEn.clear();
        if (!activas.isEmpty()) {
            activas.clear();
            difundir();
        }
    }

    /** Tiempo transcurrido desde que la alerta de este cliente quedo activa sin reconocerse. */
    @Override
    public synchronized Duration tiempoActivo(String clienteId) {
        Instant desde = activadaEn.get(clienteId);
        return desde == null ? Duration.ZERO : Duration.between(desde, Instant.now(reloj));
    }

    public synchronized List<Alerta> activas() {
        return List.copyOf(activas.values());
    }

    public synchronized void actualizarPreferencias() {
        difundir();
    }

    private void difundir() {
        List<Alerta> copia = List.copyOf(new ArrayList<>(activas.values()));
        for (CanalAlerta canal : canales) {
            canal.mostrar(copia);
        }
    }
}
