package co.monitorsiae.ui;

import co.monitorsiae.monitoreo.SnapshotInventario;

import java.util.List;
import java.util.function.Supplier;

/** El historial es opcional para dibujar el panel; sus errores se presentan como aviso. */
record ConsultaHistorial(List<SnapshotInventario> snapshots, RuntimeException error) {

    static ConsultaHistorial leer(Supplier<List<SnapshotInventario>> consulta) {
        try {
            return new ConsultaHistorial(List.copyOf(consulta.get()), null);
        } catch (RuntimeException error) {
            return new ConsultaHistorial(List.of(), error);
        }
    }
}
