package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;

/**
 * Recibe lo que pasa en el monitor de un cliente. La pantalla, las alarmas y el registro son
 * oyentes distintos; el monitor no sabe cuales hay.
 *
 * <p>Se invoca desde el hilo del monitor: las implementaciones que tocan la interfaz grafica deben
 * pasar al hilo de JavaFX.
 */
public interface OyenteMonitoreo {

    default void alIniciar(Cliente cliente) {
    }

    default void fueraDeHorario(Cliente cliente) {
    }

    default void dentroDeHorario(Cliente cliente) {
    }

    default void alTerminarCiclo(Cliente cliente, ResultadoCiclo resultado) {
    }

    default void alDetenerse(Cliente cliente, MotivoDetencion motivo) {
    }

    /**
     * El portal lleva varios fallos seguidos. El monitor no se detiene: sigue intentando, recargando
     * la página y con esperas más largas. Se avisa una sola vez por racha.
     *
     * @param ultimo descripción del último fallo, con la captura si la hubo
     */
    default void fallosPersistentes(Cliente cliente, int fallos, String ultimo) {
    }

    /** Tras una racha de fallos avisada, un ciclo volvió a salir bien. */
    default void portalRecuperado(Cliente cliente, int fallos) {
    }

    /** Un error que no se pudo manejar detuvo el monitor; se informa la causa para el diagnóstico. */
    default void alFallarGravemente(Cliente cliente, RuntimeException error) {
    }
}
