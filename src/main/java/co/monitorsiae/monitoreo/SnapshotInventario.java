package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;

import java.time.Instant;
import java.util.List;

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
}
