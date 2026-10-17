package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.SnapshotInventario;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class RepositorioSnapshotsJson implements RepositorioSnapshots {

    public static final int MAXIMO_SNAPSHOTS = 100;

    private static final TypeReference<List<SnapshotInventario>> TIPO_SNAPSHOTS = new TypeReference<>() {
    };

    private final Path archivo;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public RepositorioSnapshotsJson(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public synchronized void guardar(SnapshotInventario snapshot) {
        List<SnapshotInventario> snapshots = cargar();
        snapshots.addFirst(snapshot);
        List<SnapshotInventario> recientes = snapshots.stream()
                .limit(MAXIMO_SNAPSHOTS)
                .toList();
        escribir(recientes);
    }

    @Override
    public synchronized List<SnapshotInventario> historialDe(String clienteId) {
        return cargar().stream()
                .filter(snapshot -> snapshot.clienteId().equals(clienteId))
                .toList();
    }

    private List<SnapshotInventario> cargar() {
        if (Files.notExists(archivo)) {
            return new ArrayList<>();
        }
        try {
            List<SnapshotInventario> snapshots = json.readValue(archivo.toFile(), TIPO_SNAPSHOTS);
            if (snapshots == null) {
                throw new IllegalStateException("El historial de inventarios está vacío o dañado: " + archivo);
            }
            return new ArrayList<>(snapshots);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el historial de inventarios en " + archivo, e);
        }
    }

    private void escribir(List<SnapshotInventario> snapshots) {
        Path temporal = null;
        try {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            temporal = Files.createTempFile(archivo.toAbsolutePath().getParent(), "inventarios-", ".tmp");
            json.writeValue(temporal.toFile(), snapshots);
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
            temporal = null;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el historial de inventarios en " + archivo, e);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException e) {
                    throw new UncheckedIOException("No se pudo eliminar el archivo temporal " + temporal, e);
                }
            }
        }
    }
}
