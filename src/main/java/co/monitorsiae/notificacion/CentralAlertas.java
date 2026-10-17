package co.monitorsiae.notificacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lleva las alertas que aun no se reconocen y las reparte a todos los canales. Hay como maximo una
 * alerta por cliente: la mas reciente reemplaza a la anterior.
 *
 * <p>Segura entre hilos: la llaman los monitores de cada cliente y la interfaz grafica.
 */
public final class CentralAlertas {

    private final List<CanalAlerta> canales;
    private final Map<String, Alerta> activas = new LinkedHashMap<>();

    public CentralAlertas(List<CanalAlerta> canales) {
        this.canales = List.copyOf(canales);
    }

    public synchronized void emitir(Alerta alerta) {
        activas.remove(alerta.clienteId());
        activas.put(alerta.clienteId(), alerta);
        difundir();
    }

    /** La persona vio la alerta de este cliente: deja de sonar. */
    public synchronized void reconocer(String clienteId) {
        if (activas.remove(clienteId) != null) {
            difundir();
        }
    }

    public synchronized void reconocerTodas() {
        if (!activas.isEmpty()) {
            activas.clear();
            difundir();
        }
    }

    public synchronized List<Alerta> activas() {
        return List.copyOf(activas.values());
    }

    private void difundir() {
        List<Alerta> copia = List.copyOf(new ArrayList<>(activas.values()));
        for (CanalAlerta canal : canales) {
            canal.mostrar(copia);
        }
    }
}
