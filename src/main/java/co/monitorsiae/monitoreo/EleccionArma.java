package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;

/**
 * Arma elegida del inventario y por que.
 *
 * @param arma      fila elegida
 * @param motivo    regla que la eligio
 * @param prioridad posicion del criterio que coincidio dentro de su lista, empezando en 0
 * @param criterio  palabras clave o tipo que produjo la coincidencia
 */
public record EleccionArma(ArmaInventario arma, MotivoEleccion motivo, int prioridad, String criterio) {

    public EleccionArma {
        criterio = criterio == null ? "" : criterio;
    }

    public EleccionArma(ArmaInventario arma, MotivoEleccion motivo, int prioridad) {
        this(arma, motivo, prioridad, "");
    }
}
