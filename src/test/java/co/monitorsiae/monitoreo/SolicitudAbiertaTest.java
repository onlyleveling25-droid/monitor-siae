package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ModoInicio;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.portal.simulado.Escena;
import co.monitorsiae.portal.simulado.PortalSimulado;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cuando el paso 1 guarda la solicitud y hay inventario pero ninguna arma de interes, la solicitud
 * queda abierta en "Estado de tramite": el monitor deja de repetir el paso 1 y busca desde alli.
 */
class SolicitudAbiertaTest {

    private static final String MEDELLIN = "05 - Medellín, Antioquia";
    private static final String GLOCK = "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM";
    private static final String ESCOPETA = "Escopeta Winchester Mod SXP High Cal 12 7T+1 WINCHESTER 12";

    private final CicloMonitoreo ciclo = new CicloMonitoreo(new SelectorSeccional(), new SelectorArma());

    private final Cliente cliente = ClientesDePrueba.conReglas("Ana", new ReglasSeleccion(Modalidad.PORTE,
            List.of(CriterioArma.de("glock", "g43")), PoliticaSinObjetivo.SEGUIR_ESPERANDO,
            List.of(), "", List.of(), 2));

    @Test
    void conInventarioSinArmaDeInteresLaSolicitudQuedaAbiertaYYaNoSeRepiteElPaso1() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conInventario(MEDELLIN, ESCOPETA),      // paso 1: hay inventario pero no sirve
                Escena.abiertasSinInventario(MEDELLIN),        // luego, desde la solicitud: sin inventario
                Escena.abiertasSinInventario(MEDELLIN));
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        assertInstanceOf(ResultadoCiclo.SinArmaDeInteres.class, ciclo.ejecutar(cliente, portal, seguimiento));
        assertTrue(seguimiento.siguiendoSolicitud());

        assertInstanceOf(ResultadoCiclo.SinInventarioEnSolicitud.class, ciclo.ejecutar(cliente, portal, seguimiento));
        assertInstanceOf(ResultadoCiclo.SinInventarioEnSolicitud.class, ciclo.ejecutar(cliente, portal, seguimiento));

        assertEquals(List.of(MEDELLIN), portal.seccionalesEnviadas(), "GUARDAR se pulsa una sola vez");
        assertEquals(1, portal.formulariosAbiertos());
        assertEquals(2, portal.consultasDeSolicitud());
    }

    @Test
    void desdeLaSolicitudApartaElArmaCuandoAparece() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conInventario(MEDELLIN, ESCOPETA),
                Escena.conInventario(MEDELLIN, ESCOPETA, GLOCK));
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        ciclo.ejecutar(cliente, portal, seguimiento);
        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, seguimiento);

        ResultadoCiclo.ArmaApartada apartada = assertInstanceOf(ResultadoCiclo.ArmaApartada.class, resultado);
        assertEquals(GLOCK, apartada.eleccion().arma().descripcion());
        assertEquals(List.of(MEDELLIN), portal.seccionalesEnviadas());
    }

    @Test
    void siHayInventarioSinObjetivoDesdeLaSolicitudSigueVigilandola() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conInventario(MEDELLIN, ESCOPETA),
                Escena.conInventario(MEDELLIN, ESCOPETA));
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        ciclo.ejecutar(cliente, portal, seguimiento);
        assertInstanceOf(ResultadoCiclo.SinArmaDeInteres.class, ciclo.ejecutar(cliente, portal, seguimiento));

        assertTrue(seguimiento.siguiendoSolicitud());
    }

    @Test
    void siAlguienSeLlevaElArmaLaSolicitudSigueAbierta() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conInventario(MEDELLIN, GLOCK).conApartadoFallido());
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, ciclo.ejecutar(cliente, portal, seguimiento));

        assertTrue(seguimiento.siguiendoSolicitud());
    }

    @Test
    void pedirLaSolicitudExistenteSinTenerlaDetieneElMonitorYNoHaceElPaso1() {
        PortalSimulado portal = PortalSimulado.con(Escena.abiertasSinInventario(MEDELLIN));
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE);

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, seguimiento);

        ResultadoCiclo.SolicitudNoEncontrada noEncontrada =
                assertInstanceOf(ResultadoCiclo.SolicitudNoEncontrada.class, resultado);
        assertTrue(noEncontrada.detieneMonitoreo());
        assertTrue(noEncontrada.textoCompleto().contains("Nueva solicitud (paso 1)"));
        assertTrue(noEncontrada.textoCompleto().contains("Qué hacer"));
        assertTrue(portal.seccionalesEnviadas().isEmpty(), "no se improvisa el paso 1");
        assertEquals(0, portal.formulariosAbiertos());
        assertFalse(seguimiento.siguiendoSolicitud());
    }

    @Test
    void siLaSolicitudQueCreoElPaso1NoApareceExplicaQueHacerYVuelveAlPaso1() {
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();
        seguimiento.seguirSolicitud(MEDELLIN);   // el paso 1 encontró inventario sin armas de interés

        ResultadoCiclo.SolicitudNoEncontrada noEncontrada =
                CicloMonitoreo.solicitudNoEncontrada(cliente, seguimiento);

        assertTrue(noEncontrada.descripcion().contains("el paso 1 encontró inventario en " + MEDELLIN));
        String texto = noEncontrada.textoCompleto();
        assertTrue(texto.contains("Por qué importa"));
        assertTrue(texto.contains("Solicitud existente"), "si la ve, cómo seguir vigilándola");
        assertTrue(texto.contains("Nueva solicitud (paso 1)"), "si no está, cómo volver al paso 1");
        assertTrue(texto.contains("Iniciar / reanudar"));
        assertTrue(texto.contains(cliente.nombre()));
    }

    @Test
    void elMensajeDeSolicitudNoEncontradaNombraElNumeroPedido() {
        PortalSimulado portal = PortalSimulado.con(Escena.abiertasSinInventario(MEDELLIN));
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();
        seguimiento.establecer(ModoInicio.SOLICITUD_EXISTENTE, " 1-01-45-202610-4336 ");

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, seguimiento);

        assertTrue(assertInstanceOf(ResultadoCiclo.SolicitudNoEncontrada.class, resultado)
                .descripcion().contains("1-01-45-202610-4336"));
    }

    @Test
    void elNumeroSeConservaMientrasSeVigilaYSeOlvidaAlVolverAlPaso1() {
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        seguimiento.establecer(ModoInicio.SOLICITUD_EXISTENTE, "1-01-45-202610-4336");
        assertEquals("1-01-45-202610-4336", seguimiento.numeroSolicitud());

        seguimiento.seguirSolicitud("Medellín");   // otro ciclo sin arma de interés en la misma solicitud
        assertEquals("1-01-45-202610-4336", seguimiento.numeroSolicitud(), "no se pierde el número");
        assertFalse(seguimiento.recienCreada());

        seguimiento.volverAlPaso1();
        seguimiento.seguirSolicitud("Medellín");   // un paso 1 nuevo crea otra solicitud
        assertTrue(seguimiento.siguiendoSolicitud());
        assertTrue(seguimiento.recienCreada());
        assertEquals("", seguimiento.numeroSolicitud());

        seguimiento.establecer(ModoInicio.NUEVA_SOLICITUD, "1-01-45-202610-4336");
        assertFalse(seguimiento.siguiendoSolicitud());
        assertEquals("", seguimiento.numeroSolicitud());
    }

    @Test
    void sinSeguimientoElCicloSigueEmpezandoSiempreEnElPaso1() {
        PortalSimulado portal = PortalSimulado.con(
                Escena.conInventario(MEDELLIN, ESCOPETA),
                Escena.conInventario(MEDELLIN, ESCOPETA));

        ciclo.ejecutar(cliente, portal);
        ciclo.ejecutar(cliente, portal);

        assertEquals(2, portal.formulariosAbiertos());
        assertEquals(0, portal.consultasDeSolicitud());
    }
}
