package co.monitorsiae.persistencia;

import co.monitorsiae.dominio.AccesoAplicacion;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.dominio.PreferenciaSonidoCliente;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import co.monitorsiae.dominio.Cliente;
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
    void conservaElEntornoGlobalDelPortal() {
        Configuracion real = completa().conEntornoPortal(EntornoPortal.PORTAL_REAL);

        repositorio.guardar(real);

        assertEquals(EntornoPortal.PORTAL_REAL, repositorio.cargar().entornoPortal());
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
        assertEquals(EntornoPortal.SIMULADOR, configuracion.entornoPortal());
        assertEquals("Ana", configuracion.clientes().get(0).nombre());
        assertFalse(configuracion.clientes().get(0).id().isBlank());
    }

    @Test
    void migraVersionAnteriorConPreferenciasPredeterminadas() throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, """
                {
                  "version": 1,
                  "clientes": [{"id": "ana", "nombre": "Ana"}]
                }
                """);

        Configuracion configuracion = repositorio.cargar();

        assertEquals(Configuracion.VERSION_ACTUAL, configuracion.version());
        assertEquals(PreferenciasAlertas.porDefecto(), configuracion.preferenciasAlertas());
        assertEquals(PreferenciaSonidoCliente.HEREDAR,
                configuracion.clientes().get(0).preferenciaSonido());
    }

    @Test
    void conservaPreferenciasGlobalesYPorCliente() {
        var cliente = ClientesDePrueba.valido("Ana");
        var personalizado = new Cliente(
                cliente.id(), cliente.nombre(), cliente.activo(), cliente.tramite(), cliente.modo(),
                cliente.seleccion(), PreferenciaSonidoCliente.ACTIVADO);
        Configuracion original = completa().conClientes(List.of(personalizado))
                .conPreferenciasAlertas(new PreferenciasAlertas(false, false, TonoAlarma.DOBLE));

        repositorio.guardar(original);

        assertEquals(original, repositorio.cargar());
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

    @Test
    void migraElHorarioDeLaVersion2AUnaFranjaIdenticaPorDia() throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, """
                {
                  "version": 2,
                  "horario": {
                    "restringir": true,
                    "dias": ["MONDAY", "FRIDAY"],
                    "desde": "07:30:00",
                    "hasta": "17:00:00",
                    "zona": "America/Bogota"
                  }
                }
                """);

        HorarioOperacion horario = repositorio.cargar().horario();

        assertTrue(horario.restringir());
        assertEquals(List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY), horario.dias());
        var esperada = new co.monitorsiae.dominio.FranjaHoraria(java.time.LocalTime.of(7, 30),
                java.time.LocalTime.of(17, 0));
        assertEquals(esperada, horario.franja(java.time.DayOfWeek.MONDAY).orElseThrow());
        assertEquals(esperada, horario.franja(java.time.DayOfWeek.FRIDAY).orElseThrow());
        assertTrue(horario.franja(java.time.DayOfWeek.TUESDAY).isEmpty());
    }

    @Test
    void guardaYRecuperaHorasDistintasPorDia() throws IOException {
        var lunes = new co.monitorsiae.dominio.FranjaHoraria(java.time.LocalTime.of(8, 0), java.time.LocalTime.of(16, 0));
        var sabado = new co.monitorsiae.dominio.FranjaHoraria(java.time.LocalTime.of(9, 0), java.time.LocalTime.of(12, 30));
        Configuracion original = Configuracion.porDefecto().conHorario(new HorarioOperacion(true,
                java.util.Map.of(java.time.DayOfWeek.MONDAY, lunes, java.time.DayOfWeek.SATURDAY, sabado),
                HorarioOperacion.ZONA_COLOMBIA));

        repositorio.guardar(original);

        assertEquals(original, repositorio.cargar());
        assertTrue(Files.readString(archivo).contains("\"franjas\""));
    }

    @Test
    void unHorarioAntiguoConHoraInvalidaSeReportaComoArchivoIlegible() throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "{ \"version\": 2, \"horario\": { \"dias\": [\"MONDAY\"], \"desde\": \"25:99\" } }");

        assertThrows(ConfiguracionIlegibleException.class, repositorio::cargar);
    }
}
