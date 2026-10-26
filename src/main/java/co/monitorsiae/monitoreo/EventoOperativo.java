package co.monitorsiae.monitoreo;

import java.time.Duration;
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
 * @param seccional     seccional del hallazgo, si el evento la tiene
 * @param arma          arma del hallazgo, si el evento la tiene
 * @param duracion      cuánto duró el monitoreo, solo en los eventos de detención
 */
public record EventoOperativo(Instant momento, String clienteId, String clienteNombre, TipoEvento tipo,
                              String detalle, String seccional, String arma, Duration duracion) {

    public static final int LARGO_MAXIMO_DETALLE = 300;

    public EventoOperativo {
        Objects.requireNonNull(momento);
        Objects.requireNonNull(clienteId);
        Objects.requireNonNull(tipo);
        clienteNombre = clienteNombre == null ? "" : clienteNombre;
        detalle = acortar(detalle);
        seccional = seccional == null ? "" : acortar(seccional);
        arma = arma == null ? "" : acortar(arma);
    }

    /** Evento sin seccional, arma ni duración: el formato anterior de la bitácora. */
    public EventoOperativo(Instant momento, String clienteId, String clienteNombre, TipoEvento tipo,
                           String detalle) {
        this(momento, clienteId, clienteNombre, tipo, detalle, "", "", null);
    }

    /** Duración legible ("5 min 12 s"), o vacío si el evento no la tiene. */
    public String duracionTexto() {
        if (duracion == null) {
            return "";
        }
        long total = Math.max(0, duracion.toSeconds());
        long horas = total / 3600;
        long minutos = (total % 3600) / 60;
        long segundos = total % 60;
        if (horas > 0) {
            return horas + " h " + minutos + " min";
        }
        return minutos > 0 ? minutos + " min " + segundos + " s" : segundos + " s";
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
