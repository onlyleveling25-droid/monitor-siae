package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.SnapshotInventario;

import java.util.List;
import java.util.Optional;

public interface RepositorioSnapshots {

    void guardar(SnapshotInventario snapshot);

    List<SnapshotInventario> historialDe(String clienteId);

    default Optional<SnapshotInventario> ultimoDe(String clienteId) {
        return historialDe(clienteId).stream().findFirst();
    }
}
