package co.monitorsiae.monitoreo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Criterios para consultar la bitácora. Un criterio nulo no filtra.
 *
 * @param clienteId solo eventos de ese cliente
 * @param tipo      solo eventos de ese tipo
 * @param periodo   solo eventos más recientes que este periodo, contado hasta ahora
 */
public record FiltroBitacora(String clienteId, TipoEvento tipo, Duration periodo) {

    public static final FiltroBitacora TODO = new FiltroBitacora(null, null, null);

    /** Eventos que cumplen el filtro, del más reciente al más antiguo. */
    public List<EventoOperativo> aplicar(List<EventoOperativo> eventos, Clock reloj) {
        Instant desde = periodo == null ? null : reloj.instant().minus(periodo);
        return eventos.stream()
                .filter(e -> clienteId == null || e.clienteId().equals(clienteId))
                .filter(e -> tipo == null || e.tipo() == tipo)
                .filter(e -> desde == null || !e.momento().isBefore(desde))
                .sorted(Comparator.comparing(EventoOperativo::momento).reversed())
                .toList();
    }
}
