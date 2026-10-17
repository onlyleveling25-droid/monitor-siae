package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.SnapshotInventario;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public final class RepositorioSnapshotsMemoria implements RepositorioSnapshots {

    private static final int MAXIMO_SNAPSHOTS = 100;
    private final Deque<SnapshotInventario> snapshots = new ArrayDeque<>();

    @Override
    public synchronized void guardar(SnapshotInventario snapshot) {
        snapshots.addFirst(snapshot);
        while (snapshots.size() > MAXIMO_SNAPSHOTS) {
            snapshots.removeLast();
        }
    }

    @Override
    public synchronized List<SnapshotInventario> historialDe(String clienteId) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.clienteId().equals(clienteId))
                .collect(Collectors.toUnmodifiableList());
    }
}
