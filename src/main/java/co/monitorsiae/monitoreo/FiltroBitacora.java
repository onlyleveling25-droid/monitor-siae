package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Texto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Criterios para consultar la bitácora. Un criterio nulo o vacío no filtra.
 *
 * @param clienteId solo eventos de ese cliente
 * @param tipo      solo eventos de ese tipo
 * @param periodo   solo eventos más recientes que este periodo, contado hasta ahora
 * @param texto     palabras que deben aparecer en el cliente, tipo, detalle, seccional o arma; no distingue
 *                  mayúsculas ni tildes y "g43" coincide con "G-43". Alternativas con "o", coma o barra:
 *                  "medellin o bogota" muestra los eventos de cualquiera de las dos
 */
public record FiltroBitacora(String clienteId, TipoEvento tipo, Duration periodo, String texto) {

    public static final FiltroBitacora TODO = new FiltroBitacora(null, null, null, null);

    public FiltroBitacora(String clienteId, TipoEvento tipo, Duration periodo) {
        this(clienteId, tipo, periodo, null);
    }

    /** Indica si hay algún criterio activo. */
    public boolean activo() {
        return clienteId != null || tipo != null || periodo != null || !palabras().isEmpty();
    }

    /** Eventos que cumplen el filtro, del más reciente al más antiguo. */
    public List<EventoOperativo> aplicar(List<EventoOperativo> eventos, Clock reloj) {
        Instant desde = periodo == null ? null : reloj.instant().minus(periodo);
        List<List<String>> palabras = palabras();
        return eventos.stream()
                .filter(e -> clienteId == null || e.clienteId().equals(clienteId))
                .filter(e -> tipo == null || e.tipo() == tipo)
                .filter(e -> desde == null || !e.momento().isBefore(desde))
                .filter(e -> coincide(e, palabras))
                .sorted(Comparator.comparing(EventoOperativo::momento).reversed())
                .toList();
    }

    /** Cada alternativa es una lista de palabras que deben aparecer todas; basta con que cumpla una. */
    private List<List<String>> palabras() {
        if (texto == null) {
            return List.of();
        }
        return java.util.Arrays.stream(texto.split("(?iu)\\s*[,;|]\\s*|\\s+o\\s+"))
                .map(Texto::normalizar)
                .filter(alternativa -> !alternativa.isEmpty())
                .map(alternativa -> List.of(alternativa.split(" ")))
                .toList();
    }

    private static boolean coincide(EventoOperativo evento, List<List<String>> alternativas) {
        if (alternativas.isEmpty()) {
            return true;
        }
        String pajar = Texto.normalizar(String.join(" ", evento.clienteNombre(), evento.tipo().etiqueta(),
                evento.detalle(), evento.seccional(), evento.arma()));
        return alternativas.stream().anyMatch(palabras -> palabras.stream().allMatch(pajar::contains));
    }
}
