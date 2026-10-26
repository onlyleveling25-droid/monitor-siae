package co.monitorsiae.monitoreo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Conteo de lo más importante de un periodo, para mostrar sin recorrer la tabla. */
public record ResumenBitacora(int oportunidades, int apartadas, int problemas) {

    public static ResumenBitacora de(List<EventoOperativo> eventos, Clock reloj, Duration periodo) {
        Instant desde = reloj.instant().minus(periodo);
        List<EventoOperativo> recientes = eventos.stream().filter(e -> !e.momento().isBefore(desde)).toList();
        return new ResumenBitacora(
                (int) recientes.stream().filter(e -> e.tipo() == TipoEvento.OPORTUNIDAD).count(),
                (int) recientes.stream().filter(e -> e.tipo() == TipoEvento.ARMA_APARTADA).count(),
                (int) recientes.stream().filter(e -> e.tipo().problema()).count());
    }
}
