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
}
