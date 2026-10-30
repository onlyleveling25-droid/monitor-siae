package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;

import java.time.Instant;
import java.util.List;

/**
 * Inventario que vio el monitor en un momento dado, para revisarlo después.
 *
 * @param eleccion arma que eligió el sistema; nula cuando hubo inventario pero ninguna arma cumplía
 *                 los objetivos ni las alternativas del cliente
 */
public record SnapshotInventario(
        Instant registradoEn,
        String clienteId,
        String clienteNombre,
        String seccional,
        List<ArmaInventario> inventario,
        EleccionArma eleccion) {

    public SnapshotInventario {
        inventario = List.copyOf(inventario);
    }

    /** Verdadero si el sistema eligió un arma de este inventario. */
    public boolean conEleccion() {
        return eleccion != null;
    }
}
