package co.monitorsiae.persistencia;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.monitoreo.EleccionArma;
import co.monitorsiae.monitoreo.MotivoEleccion;
import co.monitorsiae.monitoreo.SnapshotInventario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositorioSnapshotsJsonTest {

    @TempDir
    Path directorioTemporal;

    @Test
    void persisteSnapshotCompletoYLimitaElHistorialALosCienMasRecientes() throws Exception {
        Path archivo = directorioTemporal.resolve("inventarios.json");
        RepositorioSnapshotsJson repositorio = new RepositorioSnapshotsJson(archivo);
        ArmaInventario elegida = new ArmaInventario(2, "Pistola CZ P10", "Pistola");
        SnapshotInventario primero = snapshot(0, elegida);
        repositorio.guardar(primero);

        SnapshotInventario recuperado = new RepositorioSnapshotsJson(archivo)
                .ultimoDe("cliente-1").orElseThrow();

        assertEquals(primero, recuperado);

        for (int i = 1; i <= 101; i++) {
            repositorio.guardar(snapshot(i, elegida));
        }

        assertEquals(Instant.ofEpochSecond(101),
                repositorio.ultimoDe("cliente-1").orElseThrow().registradoEn());
        assertEquals(RepositorioSnapshotsJson.MAXIMO_SNAPSHOTS,
                new ObjectMapper().readTree(archivo.toFile()).size());
    }

    private static SnapshotInventario snapshot(int segundo, ArmaInventario arma) {
        return new SnapshotInventario(Instant.ofEpochSecond(segundo), "cliente-1", "Cliente de prueba",
                "Medellín", List.of(new ArmaInventario(0, "Pistola Glock", "Pistola"), arma),
                new EleccionArma(arma, MotivoEleccion.OBJETIVO, 0));
    }
}
