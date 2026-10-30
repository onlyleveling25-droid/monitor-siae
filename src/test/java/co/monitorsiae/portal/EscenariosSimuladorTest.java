package co.monitorsiae.portal;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.EscenarioSimulador;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.monitoreo.CicloMonitoreo;
import co.monitorsiae.monitoreo.MonitorCliente;
import co.monitorsiae.monitoreo.MotivoDetencion;
import co.monitorsiae.monitoreo.MotivoEleccion;
import co.monitorsiae.monitoreo.OyenteMonitoreo;
import co.monitorsiae.monitoreo.ResultadoCiclo;
import co.monitorsiae.monitoreo.SelectorArma;
import co.monitorsiae.monitoreo.SelectorSeccional;
import co.monitorsiae.monitoreo.VentanaHoraria;
import co.monitorsiae.portal.simulado.CatalogoArmas;
import co.monitorsiae.portal.simulado.FabricaPortalSimulada;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cada escenario del simulador se recorre con el monitor real y se comprueba lo que vería la persona. */
class EscenariosSimuladorTest {

    private final Cliente cliente = ClientesDePrueba.valido("Ana");
    private final List<ResultadoCiclo> resultados = new ArrayList<>();
    private MotivoDetencion motivo;

    /** Corre el monitor hasta que termina solo o hasta pedir {@code maxEsperas} pausas. */
    private void correr(EscenarioSimulador escenario, int maxEsperas) {
        PortalDriver portal = new FabricaPortalSimulada(() -> escenario).apply(cliente);
        MonitorCliente[] monitor = new MonitorCliente[1];
        int[] esperas = {0};
        monitor[0] = new MonitorCliente(cliente, portal,
                new CicloMonitoreo(new SelectorSeccional(), new SelectorArma()),
                new VentanaHoraria(HorarioOperacion.porDefecto(), Clock.systemUTC()), Duration.ofSeconds(5),
                duracion -> {
                    if (++esperas[0] >= maxEsperas) {
                        monitor[0].detener();
                    }
                },
                new OyenteMonitoreo() {
                    @Override
                    public void alTerminarCiclo(Cliente c, ResultadoCiclo resultado) {
                        resultados.add(resultado);
                    }

                    @Override
                    public void alDetenerse(Cliente c, MotivoDetencion m) {
                        motivo = m;
                    }
                });
        monitor[0].run();
    }

    private List<String> tipos() {
        return resultados.stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.toList());
    }

    @Test
    void hallazgoInmediatoApartaElPrimerObjetivoDelCliente() {
        correr(EscenarioSimulador.HALLAZGO_INMEDIATO, 50);

        ResultadoCiclo.ArmaApartada apartada = assertInstanceOf(ResultadoCiclo.ArmaApartada.class, resultados.getFirst());
        assertEquals(MotivoEleccion.OBJETIVO, apartada.eleccion().motivo());
        assertTrue(apartada.eleccion().arma().descripcion().contains("G-43"));
        assertEquals(MotivoDetencion.ARMA_APARTADA, motivo);
        assertEquals(1, resultados.size());
    }

    @Test
    void sinInventarioYLuegoHayMuestraLaEsperaYDespuesLaSeleccion() {
        correr(EscenarioSimulador.SIN_INVENTARIO_Y_LUEGO_HAY, 50);

        assertEquals(List.of("SeccionalesCerradas", "SeccionalesCerradas", "SinInventario", "SinInventario",
                "ArmaApartada"), tipos());
        assertEquals(MotivoDetencion.ARMA_APARTADA, motivo);
    }

    @Test
    void inventarioSinObjetivosActivaLaAlternativa() {
        correr(EscenarioSimulador.INVENTARIO_SIN_OBJETIVOS, 50);

        ResultadoCiclo.ArmaApartada apartada = assertInstanceOf(ResultadoCiclo.ArmaApartada.class, resultados.getLast());
        assertEquals(MotivoEleccion.ALTERNATIVA, apartada.eleccion().motivo());
        assertTrue(apartada.eleccion().arma().descripcion().toLowerCase().contains("cordova"));
    }

    @Test
    void elApartadoFallidoSeReportaYDetieneElMonitorConLaTablaAbierta() {
        correr(EscenarioSimulador.APARTADO_FALLIDO, 50);

        assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, resultados.getLast());
        assertEquals(MotivoDetencion.APARTADO_FALLIDO, motivo);
    }

    @Test
    void tresFallosSeguidosDetienenElMonitorConAlarma() {
        correr(EscenarioSimulador.TRES_FALLOS_SEGUIDOS, 50);

        assertEquals(List.of("SeccionalesCerradas", "SinInventario", "Inesperado", "Inesperado", "Inesperado"),
                tipos());
        assertEquals(MotivoDetencion.FALLOS_REPETIDOS, motivo);
    }

    @Test
    void laSesionVencidaDetieneElMonitor() {
        correr(EscenarioSimulador.SESION_VENCIDA, 50);

        assertEquals(List.of("SeccionalesCerradas", "SinInventario", "SesionVencida"), tipos());
        assertEquals(MotivoDetencion.SESION_VENCIDA, motivo);
    }

    @Test
    void conLasSeccionalesSiempreCerradasElMonitorSoloEspera() {
        correr(EscenarioSimulador.SECCIONALES_CERRADAS, 5);

        assertTrue(resultados.stream().allMatch(r -> r instanceof ResultadoCiclo.SeccionalesCerradas));
        assertEquals(MotivoDetencion.SOLICITADO_POR_USUARIO, motivo);
    }

    @Test
    void elCatalogoTieneVariedadSuficienteParaProbarLaSeleccion() {
        assertTrue(CatalogoArmas.armas().size() >= 10);
        assertTrue(CatalogoArmas.armas().stream().anyMatch(a -> a.contains("G-43")));
        assertTrue(CatalogoArmas.armas().stream().anyMatch(a -> a.contains("22 LR")));
    }
}
