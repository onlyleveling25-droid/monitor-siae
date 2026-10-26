package co.monitorsiae.monitoreo;

import java.time.Instant;
import java.util.Objects;

/**
 * Algo que vale la pena recordar de la operación de un cliente.
 *
 * @param momento       cuándo ocurrió
 * @param clienteId     identificador del cliente
 * @param clienteNombre nombre del cliente al momento del evento
 * @param tipo          clase de evento
 * @param detalle       texto breve y accionable; nunca contiene credenciales ni datos de sesión
 */
public record EventoOperativo(Instant momento, String clienteId, String clienteNombre, TipoEvento tipo,
                              String detalle) {

    public static final int LARGO_MAXIMO_DETALLE = 300;

    public EventoOperativo {
        Objects.requireNonNull(momento);
        Objects.requireNonNull(clienteId);
        Objects.requireNonNull(tipo);
        clienteNombre = clienteNombre == null ? "" : clienteNombre;
        detalle = acortar(detalle);
    }

    private static String acortar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.strip().replaceAll("\\s+", " ");
        return limpio.length() <= LARGO_MAXIMO_DETALLE
                ? limpio
                : limpio.substring(0, LARGO_MAXIMO_DETALLE - 1) + "…";
    }
}
