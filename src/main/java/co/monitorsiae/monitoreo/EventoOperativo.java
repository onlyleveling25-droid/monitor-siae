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
 * @param captura       nombre del archivo de la captura de pantalla del fallo, si hubo; solo el nombre, nunca una
 *                      ruta: la carpeta de capturas la conoce la aplicación
 */
public record EventoOperativo(Instant momento, String clienteId, String clienteNombre, TipoEvento tipo,
                              String detalle, String seccional, String arma, Duration duracion, String captura) {

    public static final int LARGO_MAXIMO_DETALLE = 300;

    /** Forma exacta del nombre que da el portal real a sus capturas; cualquier otra cosa se descarta. */
    private static final java.util.regex.Pattern NOMBRE_CAPTURA =
            java.util.regex.Pattern.compile("fallo-\\d{8}-\\d{6}\\.png");
    private static final java.util.regex.Pattern CAPTURA_EN_TEXTO =
            java.util.regex.Pattern.compile("\\(captura: (fallo-\\d{8}-\\d{6}\\.png)\\)");

    public EventoOperativo {
        Objects.requireNonNull(momento);
        Objects.requireNonNull(clienteId);
        Objects.requireNonNull(tipo);
        clienteNombre = clienteNombre == null ? "" : clienteNombre;
        detalle = acortar(detalle);
        seccional = seccional == null ? "" : acortar(seccional);
        arma = arma == null ? "" : acortar(arma);
        captura = captura != null && NOMBRE_CAPTURA.matcher(captura).matches() ? captura : "";
    }

    /** Evento sin seccional, arma ni duración: el formato anterior de la bitácora. */
    public EventoOperativo(Instant momento, String clienteId, String clienteNombre, TipoEvento tipo,
                           String detalle) {
        this(momento, clienteId, clienteNombre, tipo, detalle, "", "", null, "");
    }

    /** Evento sin captura de pantalla: el formato anterior a guardarla. */
    public EventoOperativo(Instant momento, String clienteId, String clienteNombre, TipoEvento tipo,
                           String detalle, String seccional, String arma, Duration duracion) {
        this(momento, clienteId, clienteNombre, tipo, detalle, seccional, arma, duracion, "");
    }

    /** Nombre de la captura que menciona un texto como "(captura: fallo-20261026-140348.png)", o vacío. */
    public static String capturaEn(String texto) {
        if (texto == null) {
            return "";
        }
        java.util.regex.Matcher coincidencia = CAPTURA_EN_TEXTO.matcher(texto);
        return coincidencia.find() ? coincidencia.group(1) : "";
    }

    /** Duración legible ("5 min 12 s"), o vacío si el evento no la tiene. */
    public String duracionTexto() {
        if (duracion == null) {
            return "";
        }
        return TextoDuracion.texto(duracion.toSeconds());
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
