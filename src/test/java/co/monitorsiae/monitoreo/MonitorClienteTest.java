package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.portal.simulado.Escena;
import co.monitorsiae.portal.simulado.PortalSimulado;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MonitorClienteTest {

    private static final String MEDELLIN = "05 - Medellín, Antioquia";
    private static final Duration INTERVALO = Duration.ofSeconds(5);

    private final Cliente cliente = ClientesDePrueba.valido("Ana");
    private final List<Duration> esperas = new ArrayList<>();
    private final OyenteDePrueba oyente = new OyenteDePrueba();

    @Test
    void repiteHastaApartarYSeDetieneSinCerrarNada() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.seccionalesCerradas(),
                Escena.abiertasSinInventario(MEDELLIN),
                Escena.conInventario(MEDELLIN, "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM"));

        monitor(portal, siempreAbierto()).run();

        assertEquals(3, portal.formulariosAbiertos());
        assertEquals(List.of(INTERVALO, INTERVALO), esperas);
        assertInstanceOf(ResultadoCiclo.ArmaApartada.class, oyente.resultados.get(2));
        assertEquals(MotivoDetencion.ARMA_APARTADA, oyente.motivo);
    }

    @Test
    void seDetieneSiLaSesionVence() {
        PortalSimulado portal = PortalSimulado.con(Escena.conFalla(Escena.Falla.SESION_VENCIDA));

        monitor(portal, siempreAbierto()).run();

        assertEquals(1, portal.formulariosAbiertos());
        assertEquals(MotivoDetencion.SESION_VENCIDA, oyente.motivo);
    }

    @Test
    void toleraFallasAisladasConEsperaCrecienteYSeDetieneSiSeRepiten() {
        PortalSimulado portal = PortalSimulado.con(Escena.conFalla(Escena.Falla.ERROR_SERVIDOR));

        monitor(portal, siempreAbierto()).run();

        assertEquals(MonitorCliente.FALLOS_TOLERADOS, portal.formulariosAbiertos());
        assertEquals(List.of(INTERVALO.multipliedBy(2), INTERVALO.multipliedBy(3)), esperas);
        assertEquals(MotivoDetencion.FALLOS_REPETIDOS, oyente.motivo);
    }

    @Test
    void unPortalQueNoContestaNoDetieneElMonitorPorFallosRepetidos() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conFalla(Escena.Falla.PORTAL_NO_RESPONDE),
                Escena.conFalla(Escena.Falla.PORTAL_NO_RESPONDE),
                Escena.conFalla(Escena.Falla.PORTAL_NO_RESPONDE),
                Escena.conFalla(Escena.Falla.PORTAL_NO_RESPONDE),
                Escena.conFalla(Escena.Falla.PORTAL_NO_RESPONDE),
                Escena.conInventario(MEDELLIN, "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM"));

        monitor(portal, siempreAbierto()).run();

        assertEquals(MotivoDetencion.ARMA_APARTADA, oyente.motivo);
        assertEquals(List.of(INTERVALO, INTERVALO, INTERVALO, INTERVALO, INTERVALO), esperas);
    }

    @Test
    void unaFallaAisladaNoAcumulaSiLuegoSeRecupera() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conFalla(Escena.Falla.ERROR_SERVIDOR),
                Escena.seccionalesCerradas(),
                Escena.conFalla(Escena.Falla.ERROR_SERVIDOR),
                Escena.conFalla(Escena.Falla.ERROR_SERVIDOR),
                Escena.conInventario(MEDELLIN, "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM"));

        monitor(portal, siempreAbierto()).run();

        assertEquals(MotivoDetencion.ARMA_APARTADA, oyente.motivo);
    }

    @Test
    void fueraDeHorarioNoConsultaLaPlataforma() {
        // Sabado 17 de octubre de 2026 a las 10:00 en Colombia.
        Clock sabado = Clock.fixed(Instant.parse("2026-10-17T15:00:00Z"), HorarioOperacion.ZONA_COLOMBIA);
        HorarioOperacion entreSemana = new HorarioOperacion(true, List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(8, 0), LocalTime.of(16, 0), HorarioOperacion.ZONA_COLOMBIA);
        PortalSimulado portal = PortalSimulado.con(Escena.seccionalesCerradas());
        MonitorCliente[] monitor = new MonitorCliente[1];
        Pausa detenerAlSegundaEspera = duracion -> {
            esperas.add(duracion);
            if (esperas.size() == 2) {
                monitor[0].detener();
            }
        };
        monitor[0] = new MonitorCliente(cliente, portal, nuevoCiclo(), new VentanaHoraria(entreSemana, sabado),
                INTERVALO, detenerAlSegundaEspera, oyente);

        monitor[0].run();

        assertEquals(0, portal.formulariosAbiertos());
        assertEquals(2, oyente.fueraDeHorario);
        assertEquals(MotivoDetencion.SOLICITADO_POR_USUARIO, oyente.motivo);
    }

    @Test
    void notificaCuandoEntraAlHorarioYReanudaLasConsultas() {
        AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2026-10-17T15:00:00Z"));
        Clock reloj = new Clock() {
            @Override
            public ZoneId getZone() {
                return HorarioOperacion.ZONA_COLOMBIA;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return ahora.get();
            }
        };
        HorarioOperacion horario = new HorarioOperacion(true, List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                LocalTime.of(8, 0), LocalTime.of(16, 0), HorarioOperacion.ZONA_COLOMBIA);
        PortalSimulado portal = PortalSimulado.con(Escena.seccionalesCerradas());
        MonitorCliente[] monitor = new MonitorCliente[1];
        Pausa avanzarRelojYDetener = duracion -> {
            esperas.add(duracion);
            if (esperas.size() == 1) {
                ahora.set(Instant.parse("2026-10-19T14:00:00Z"));
            } else {
                monitor[0].detener();
            }
        };
        monitor[0] = new MonitorCliente(cliente, portal, nuevoCiclo(),
                new VentanaHoraria(horario, reloj), INTERVALO, avanzarRelojYDetener, oyente);

        monitor[0].run();

        assertEquals(1, oyente.fueraDeHorario);
        assertEquals(1, oyente.dentroDeHorario);
        assertEquals(1, portal.formulariosAbiertos());
    }

    private MonitorCliente monitor(PortalSimulado portal, VentanaHoraria ventana) {
        return new MonitorCliente(cliente, portal, nuevoCiclo(), ventana, INTERVALO, esperas::add, oyente);
    }

    private static CicloMonitoreo nuevoCiclo() {
        return new CicloMonitoreo(new SelectorSeccional(), new SelectorArma());
    }

    private static VentanaHoraria siempreAbierto() {
        return new VentanaHoraria(HorarioOperacion.porDefecto(), Clock.systemUTC());
    }

    private static final class OyenteDePrueba implements OyenteMonitoreo {
        final List<ResultadoCiclo> resultados = new ArrayList<>();
        int fueraDeHorario;
        int dentroDeHorario;
        MotivoDetencion motivo;

        @Override
        public void fueraDeHorario(Cliente cliente) {
            fueraDeHorario++;
        }

        @Override
        public void dentroDeHorario(Cliente cliente) {
            dentroDeHorario++;
        }

        @Override
        public void alTerminarCiclo(Cliente cliente, ResultadoCiclo resultado) {
            resultados.add(resultado);
        }

        @Override
        public void alDetenerse(Cliente cliente, MotivoDetencion motivo) {
            this.motivo = motivo;
        }
    }
}
