package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ModoReaccion;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.portal.simulado.Escena;
import co.monitorsiae.portal.simulado.PortalSimulado;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CicloMonitoreoTest {

    private static final String MEDELLIN = "05 - Medellín, Antioquia";

    private final CicloMonitoreo ciclo = new CicloMonitoreo(new SelectorSeccional(), new SelectorArma());

    private final Cliente cliente = ClientesDePrueba.conReglas("Ana", new ReglasSeleccion(Modalidad.PORTE,
            List.of(CriterioArma.de("glock", "g43")), PoliticaSinObjetivo.SEGUIR_ESPERANDO,
            List.of(), "", List.of(), 2));

    @Test
    void distingueTramiteNoDisponibleYSeccionalesCerradas() {
        assertInstanceOf(ResultadoCiclo.TramiteNoDisponible.class,
                ciclo.ejecutar(cliente, PortalSimulado.con(Escena.tramiteNoDisponible())));
        assertInstanceOf(ResultadoCiclo.SeccionalesCerradas.class,
                ciclo.ejecutar(cliente, PortalSimulado.con(Escena.seccionalesCerradas())));
    }

    @Test
    void noEnviaElPaso1SiNingunaSeccionalAbiertaEsObjetivo() {
        PortalSimulado portal = PortalSimulado.con(Escena.abiertasSinInventario("73 - Cali, Valle del Cauca"));

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal);

        assertInstanceOf(ResultadoCiclo.SinSeccionalObjetivo.class, resultado);
        assertTrue(portal.seccionalesEnviadas().isEmpty());
    }

    @Test
    void reportaSinInventarioCuandoGuardarNoAvanza() {
        PortalSimulado portal = PortalSimulado.con(Escena.abiertasSinInventario(MEDELLIN));

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal);

        assertEquals(new ResultadoCiclo.SinInventario(MEDELLIN), resultado);
        assertEquals(List.of(MEDELLIN), portal.seccionalesEnviadas());
    }

    @Test
    void noApartaSiElInventarioNoTieneArmasDeInteres() {
        PortalSimulado portal = PortalSimulado.con(Escena.conInventario(MEDELLIN,
                "Pistola Cordova Compacta Cal 9MM 9T 3P (Pequeña) CORDOVA 9MM"));

        assertInstanceOf(ResultadoCiclo.SinArmaDeInteres.class, ciclo.ejecutar(cliente, portal));
        assertTrue(portal.intentosDeApartar().isEmpty());
    }

    @Test
    void apartaElArmaObjetivo() {
        PortalSimulado portal = PortalSimulado.con(Escena.conInventario(MEDELLIN,
                "Revolver Martial Ultra Cal 38 4\" Cacha Madera INDUMIL 38L",
                "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM"));

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal);

        ResultadoCiclo.ArmaApartada apartada = assertInstanceOf(ResultadoCiclo.ArmaApartada.class, resultado);
        assertEquals(1, apartada.eleccion().arma().posicion());
        assertEquals(1, apartada.intentos());
        assertTrue(resultado.detieneMonitoreo());
    }

    @Test
    void reintentaApartarHastaElLimiteYLuegoSigue() {
        PortalSimulado portal = PortalSimulado.con(Escena.conInventario(MEDELLIN,
                "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM").conApartadoFallido());

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal);

        assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, resultado);
        assertEquals(2, portal.intentosDeApartar().size());
        assertTrue(!resultado.detieneMonitoreo());
    }

    @Test
    void enModoAlertaNoSeleccionaNada() {
        Cliente enAlerta = new Cliente(cliente.id(), cliente.nombre(), true, cliente.tramite(),
                ModoReaccion.ALERTA, cliente.seleccion());
        PortalSimulado portal = PortalSimulado.con(Escena.conInventario(MEDELLIN,
                "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM"));

        assertInstanceOf(ResultadoCiclo.ArmaEncontrada.class, ciclo.ejecutar(enAlerta, portal));
        assertTrue(portal.intentosDeApartar().isEmpty());
    }

    @Test
    void convierteLasFallasDelPortalEnResultados() {
        assertInstanceOf(ResultadoCiclo.SesionVencida.class,
                ciclo.ejecutar(cliente, PortalSimulado.con(Escena.conFalla(Escena.Falla.SESION_VENCIDA))));
        assertInstanceOf(ResultadoCiclo.Inesperado.class,
                ciclo.ejecutar(cliente, PortalSimulado.con(Escena.conFalla(Escena.Falla.ERROR_SERVIDOR))));
    }

    @Test
    void unaDescripcionInvalidaSeReportaComoInesperado() {
        Cliente sinDescripcion = ClientesDePrueba.conDescripcion("Ana", "corta");

        assertInstanceOf(ResultadoCiclo.Inesperado.class,
                ciclo.ejecutar(sinDescripcion, PortalSimulado.con(Escena.abiertasSinInventario(MEDELLIN))));
    }
}
