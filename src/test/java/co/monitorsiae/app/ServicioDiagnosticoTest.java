package co.monitorsiae.app;

import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import co.monitorsiae.dominio.ValidadorConfiguracion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static co.monitorsiae.app.Comprobacion.Nivel.AVISO;
import static co.monitorsiae.app.Comprobacion.Nivel.CORRECTO;
import static co.monitorsiae.app.Comprobacion.Nivel.PROBLEMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioDiagnosticoTest {

    // Lunes 2026-10-26 12:00 UTC = 07:00 en Bogotá.
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path datos;

    private Configuracion base = Configuracion.porDefecto().conClientes(List.of(ClientesDePrueba.valido("Ana")));
    private Predicate<NavegadorPreferido> navegador = n -> true;
    private boolean audio = true;
    private Optional<String> voz = Optional.of("Sabina");
    private Path carpetaDatos;

    private Diagnostico ejecutar() {
        return new ServicioDiagnostico(() -> base, new ValidadorConfiguracion(),
                carpetaDatos == null ? datos : carpetaDatos, RELOJ, navegador, () -> audio, () -> voz).ejecutar();
    }

    private static Comprobacion de(Diagnostico d, String nombre) {
        return d.comprobaciones().stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    @Test
    void todoEnOrdenDaVeredictoCorrecto() {
        Diagnostico d = ejecutar();

        assertEquals(5, d.comprobaciones().size());
        assertEquals(CORRECTO, d.veredicto());
        assertTrue(d.comprobaciones().stream().allMatch(c -> c.remedio().isEmpty()));
    }

    @Test
    void configuracionInvalidaEsProblemaYSinClientesEsAviso() {
        base = new Configuracion(1, null, null, 1, null, List.of());
        assertEquals(PROBLEMA, de(ejecutar(), "Configuración").nivel());

        base = Configuracion.porDefecto();
        Comprobacion sinClientes = de(ejecutar(), "Configuración");
        assertEquals(AVISO, sinClientes.nivel());
        assertFalse(sinClientes.remedio().isBlank());
    }

    @Test
    void navegadorAusenteEsProblemaConPortalRealYAvisoConSimulador() {
        navegador = n -> false;
        base = base.conEntornoPortal(EntornoPortal.PORTAL_REAL);
        assertEquals(PROBLEMA, de(ejecutar(), "Navegador").nivel());

        base = base.conEntornoPortal(EntornoPortal.SIMULADOR);
        assertEquals(AVISO, de(ejecutar(), "Navegador").nivel());
    }

    @Test
    void carpetaDeDatosNoEscribibleEsProblema() {
        carpetaDatos = Path.of("/dev/null/imposible");

        assertEquals(PROBLEMA, de(ejecutar(), "Almacenamiento").nivel());
    }

    @Test
    void carpetaDeDatosInexistenteIgualSePuedeCrearSiElPadreEsEscribible() {
        carpetaDatos = datos.resolve("aun/no/existe");

        assertEquals(CORRECTO, de(ejecutar(), "Almacenamiento").nivel());
    }

    @Test
    void audioYVozReportanCadaFalla() {
        audio = false;
        assertEquals(PROBLEMA, de(ejecutar(), "Audio y voz").nivel());

        audio = true;
        voz = Optional.empty();
        assertEquals(AVISO, de(ejecutar(), "Audio y voz").nivel());

        base = base.conPreferenciasAlertas(new PreferenciasAlertas(false, false, TonoAlarma.SIRENA));
        Comprobacion silenciadas = de(ejecutar(), "Audio y voz");
        assertEquals(AVISO, silenciadas.nivel());
        assertTrue(silenciadas.detalle().contains("desactivados"));
    }

    @Test
    void sinAudioNiVozNoHaceFaltaConsultarLasVoces() {
        base = base.conPreferenciasAlertas(new PreferenciasAlertas(false, false, TonoAlarma.SIRENA));
        voz = null;

        assertEquals(AVISO, de(ejecutar(), "Audio y voz").nivel());
    }

    @Test
    void horarioDentroFueraYSinFranjaValida() {
        HorarioOperacion dentro = new HorarioOperacion(true, List.of(DayOfWeek.MONDAY),
                LocalTime.of(6, 0), LocalTime.of(16, 0), HorarioOperacion.ZONA_COLOMBIA);
        base = conHorario(dentro);
        assertEquals(CORRECTO, de(ejecutar(), "Horario").nivel());

        base = conHorario(new HorarioOperacion(true, List.of(DayOfWeek.SUNDAY),
                LocalTime.of(6, 0), LocalTime.of(16, 0), HorarioOperacion.ZONA_COLOMBIA));
        assertEquals(AVISO, de(ejecutar(), "Horario").nivel());

        base = conHorario(new HorarioOperacion(true, List.of(),
                LocalTime.of(6, 0), LocalTime.of(16, 0), HorarioOperacion.ZONA_COLOMBIA));
        assertEquals(PROBLEMA, de(ejecutar(), "Horario").nivel());

        base = conHorario(new HorarioOperacion(true, List.of(DayOfWeek.MONDAY),
                LocalTime.of(16, 0), LocalTime.of(6, 0), HorarioOperacion.ZONA_COLOMBIA));
        assertEquals(PROBLEMA, de(ejecutar(), "Horario").nivel());
    }

    @Test
    void unaComprobacionQueFallaNoOcultaLasDemas() {
        voz = null; // provoca NullPointerException solo en Audio y voz

        Diagnostico d = ejecutar();

        assertEquals(5, d.comprobaciones().size());
        assertEquals(PROBLEMA, de(d, "Audio y voz").nivel());
        assertEquals(CORRECTO, de(d, "Horario").nivel());
        assertEquals(PROBLEMA, d.veredicto());
    }

    private Configuracion conHorario(HorarioOperacion horario) {
        return new Configuracion(base.version(), base.acceso(), base.navegador(), base.entornoPortal(),
                base.intervaloConsultaSegundos(), horario, base.clientes(), base.preferenciasAlertas());
    }

    @Test
    void unaSolaFranjaInvalidaBastaParaMarcarProblemaEnElHorario() {
        base = conHorario(new HorarioOperacion(true, java.util.Map.of(
                DayOfWeek.MONDAY, new co.monitorsiae.dominio.FranjaHoraria(LocalTime.of(6, 0), LocalTime.of(16, 0)),
                DayOfWeek.SATURDAY, new co.monitorsiae.dominio.FranjaHoraria(LocalTime.of(12, 0), LocalTime.of(9, 0))),
                HorarioOperacion.ZONA_COLOMBIA));

        assertEquals(PROBLEMA, de(ejecutar(), "Horario").nivel());
    }

    @Test
    void buscaElNavegadorEnLasRutasDeCadaSistema() {
        var mac = ServicioDiagnostico.rutasCandidatas(NavegadorPreferido.CHROME, "Mac OS X", "/Users/ana", n -> null);
        assertTrue(mac.contains(Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")));
        assertTrue(mac.contains(Path.of("/Users/ana/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")));

        var macEdge = ServicioDiagnostico.rutasCandidatas(NavegadorPreferido.EDGE, "Mac OS X", "/Users/ana", n -> null);
        assertTrue(macEdge.contains(Path.of("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge")));

        var windows = ServicioDiagnostico.rutasCandidatas(NavegadorPreferido.CHROME, "Windows 11", "C:\\Users\\ana",
                nombre -> nombre.equals("PROGRAMFILES") ? "C:\\Program Files" : null);
        assertEquals(1, windows.size());
        assertTrue(windows.getFirst().toString().endsWith("chrome.exe"));

        var linux = ServicioDiagnostico.rutasCandidatas(NavegadorPreferido.CHROME, "Linux", "/home/ana", n -> null);
        assertTrue(linux.contains(Path.of("/usr/bin/google-chrome")));
    }
}
