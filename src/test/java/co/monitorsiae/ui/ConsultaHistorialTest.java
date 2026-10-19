package co.monitorsiae.ui;

import co.monitorsiae.persistencia.RepositorioSnapshotsJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaHistorialTest {
    @TempDir
    Path carpeta;

    @Test
    void historialDanadoSeDevuelveComoAvisoSinModificarElArchivo() throws Exception {
        Path archivo = carpeta.resolve("inventarios.json");
        String danado = "{ archivo dañado";
        Files.writeString(archivo, danado);
        var repositorio = new RepositorioSnapshotsJson(archivo);

        ConsultaHistorial resultado = ConsultaHistorial.leer(() -> repositorio.historialDe("cliente"));

        assertTrue(resultado.snapshots().isEmpty());
        assertNotNull(resultado.error());
        assertEquals(danado, Files.readString(archivo));

        Files.writeString(archivo, "[]");
        assertNull(ConsultaHistorial.leer(() -> repositorio.historialDe("cliente")).error());
    }

    @Test
    void sinArchivoNoInventaUnErrorNiCreaArchivos() {
        Path archivo = carpeta.resolve("inventarios.json");
        var repositorio = new RepositorioSnapshotsJson(archivo);

        ConsultaHistorial resultado = ConsultaHistorial.leer(() -> repositorio.historialDe("cliente"));

        assertEquals(List.of(), resultado.snapshots());
        assertNull(resultado.error());
        assertFalse(Files.exists(archivo));
    }
}
