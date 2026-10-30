package co.monitorsiae.monitoreo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distingue un inventario sin arma de interés que acaba de aparecer del mismo listado visto otra
 * vez en el ciclo siguiente. Así la bitácora, el historial y la alarma lo atienden una sola vez por
 * inventario, y vuelven a hacerlo si el listado cambia o si desaparece y reaparece.
 *
 * <p>Los fallos pasajeros (portal sin respuesta, respuesta inesperada) no cuentan como "el inventario
 * desapareció": un error entre dos lecturas iguales no repite el aviso.
 */
public final class InventarioNuevo {

    private final Map<String, String> ultimaFirma = new ConcurrentHashMap<>();

    /**
     * Toma nota del resultado del ciclo.
     *
     * @return verdadero solo si es un inventario sin arma de interés distinto del último visto
     */
    public boolean registrar(String clienteId, ResultadoCiclo resultado) {
        if (resultado instanceof ResultadoCiclo.SinArmaDeInteres sinInteres) {
            boolean distinto = !sinInteres.firma().equals(ultimaFirma.put(clienteId, sinInteres.firma()));
            // Lo que la persona ya vio (y decidió no tomar) no se vuelve a anunciar.
            return distinto && !sinInteres.yaVisto();
        }
        if (!(resultado instanceof ResultadoCiclo.PortalNoResponde)
                && !(resultado instanceof ResultadoCiclo.Inesperado)) {
            ultimaFirma.remove(clienteId);
        }
        return false;
    }

    /** Olvida lo visto del cliente, por ejemplo al iniciar de nuevo su monitoreo. */
    public void olvidar(String clienteId) {
        ultimaFirma.remove(clienteId);
    }
}
