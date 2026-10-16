package co.monitorsiae.persistencia;

import co.monitorsiae.dominio.AccesoAplicacion;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.HorarioOperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioConfiguracionJsonTest {

    @TempDir
    Path carpeta;

    private Path archivo;
    private RepositorioConfiguracionJson repositorio;

    @BeforeEach
    void preparar() {
        archivo = carpeta.resolve("MonitorSIAE").resolve("config.json");
        repositorio = new RepositorioConfiguracionJson(archivo);
    }

    @Test
    void sinArchivoDevuelveConfiguracionPorDefectoSinCrearlo() {
        assertEquals(Configuracion.porDefecto(), repositorio.cargar());
        assertFalse(Files.exists(archivo));
    }

    @Test
    void loGuardadoSeLeeIgual() {
        Configuracion original = completa();

        repositorio.guardar(original);

        assertEquals(original, repositorio.cargar());
    }

    @Test
    void conservaTildesYCaracteresEspeciales() throws IOException {
        repositorio.guardar(completa());

        String contenido = Files.readString(archivo);
        assertTrue(contenido.contains("Medellín"));
        assertTrue(contenido.contains("Adquisición de arma"));
    }

    @Test
    void alGuardarDeNuevoConservaLaVersionAnteriorComoRespaldo() {
        Configuracion primera = completa();
        Configuracion segunda = primera.conAcceso(new AccesoAplicacion(false, ""));

        repositorio.guardar(primera);
        repositorio.guardar(segunda);

        assertTrue(repositorio.existeRespaldo());
        assertEquals(segunda, repositorio.cargar());
        assertEquals(primera, repositorio.restaurarRespaldo());
        assertEquals(primera, repositorio.cargar());
    }

    @Test
    void noDejaArchivosTemporales() throws IOException {
        repositorio.guardar(completa());
        repositorio.guardar(completa());

        try (var archivos = Files.list(archivo.getParent())) {
            assertTrue(archivos.noneMatch(ruta -> ruta.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void archivoDanadoLanzaExcepcionRecuperable() throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "{ esto no es json");

        ConfiguracionIlegibleException error =
                assertThrows(ConfiguracionIlegibleException.class, repositorio::cargar);
        assertEquals(archivo, error.archivo());
    }

    @Test
    void configuracionDeUnaVersionMasNuevaNoSeAbre() throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "{ \"version\": " + (Configuracion.VERSION_ACTUAL + 1) + " }");

        assertThrows(ConfiguracionIlegibleException.class, repositorio::cargar);
    }

    @Test
    void camposFaltantesTomanValoresPorDefecto() throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "{ \"clientes\": [ { \"nombre\": \"Ana\" } ] }");

        Configuracion configuracion = repositorio.cargar();

        assertEquals(Configuracion.INTERVALO_POR_DEFECTO_SEGUNDOS, configuracion.intervaloConsultaSegundos());
        assertEquals("Ana", configuracion.clientes().get(0).nombre());
        assertFalse(configuracion.clientes().get(0).id().isBlank());
    }

    @Test
    void restaurarSinRespaldoLanzaExcepcion() {
        assertThrows(ConfiguracionIlegibleException.class, repositorio::restaurarRespaldo);
    }

    private static Configuracion completa() {
        return new Configuracion(Configuracion.VERSION_ACTUAL, new AccesoAplicacion(true, "pbkdf2$1$a$b"),
                null, 7, HorarioOperacion.porDefecto(),
                List.of(ClientesDePrueba.valido("Ana"), ClientesDePrueba.valido("Luis")));
    }
}
