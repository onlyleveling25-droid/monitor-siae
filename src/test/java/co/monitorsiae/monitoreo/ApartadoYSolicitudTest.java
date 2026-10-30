package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ModoInicio;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalInesperadoException;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.ResultadoSolicitud;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reintentos que repasan el listado, solicitud fijada por número y cuadro «Ver armas» ya confirmado. */
class ApartadoYSolicitudTest {

    private static final String GLOCK = "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM";
    private static final String CORDOVA = "Pistola Cordova Compacta Cal 9MM 9T 3P CORDOVA 9MM";
    private static final String ESCOPETA = "Escopeta Winchester Mod SXP High Cal 12 7T+1 WINCHESTER 12";

    private final CicloMonitoreo ciclo = new CicloMonitoreo(new SelectorSeccional(), new SelectorArma());

    /** Objetivo Glock; si no está, la Cordova como alternativa. Tres reintentos. */
    private final Cliente cliente = ClientesDePrueba.conReglas("Ana", new ReglasSeleccion(Modalidad.PORTE,
            List.of(CriterioArma.de("glock")), PoliticaSinObjetivo.USAR_ALTERNATIVAS,
            List.of(CriterioArma.de("cordova")), "", List.of(), 3));

    @Test
    void siElObjetivoDesapareceAlReintentarTomaLaAlternativa() {
        PortalFalso portal = new PortalFalso(List.of(arma(0, GLOCK), arma(1, CORDOVA)));
        portal.alFallarQuitar = true;                 // otra persona se lleva el arma al fallar
        portal.exitosas = Set.of(CORDOVA);

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE));

        ResultadoCiclo.ArmaApartada apartada = assertInstanceOf(ResultadoCiclo.ArmaApartada.class, resultado);
        assertEquals(CORDOVA, apartada.eleccion().arma().descripcion());
        assertEquals(MotivoEleccion.ALTERNATIVA, apartada.eleccion().motivo());
        assertEquals(2, apartada.intentos());
        assertEquals(List.of(GLOCK, CORDOVA), portal.intentos);
    }

    @Test
    void siElArmaSigueEnElListadoSeReintentaConEllaHastaElLimite() {
        PortalFalso portal = new PortalFalso(List.of(arma(0, GLOCK)));

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE));

        ResultadoCiclo.ApartadoFallido fallido = assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, resultado);
        assertEquals(3, fallido.intentos());
        assertEquals(List.of(GLOCK, GLOCK, GLOCK), portal.intentos);
    }

    @Test
    void siYaNoQuedaNingunaQueCumplaNoSigueIntentando() {
        PortalFalso portal = new PortalFalso(List.of(arma(0, GLOCK), arma(1, ESCOPETA)));
        portal.alFallarQuitar = true;

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE));

        ResultadoCiclo.ApartadoFallido fallido = assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, resultado);
        assertEquals(1, fallido.intentos());
        assertEquals(List.of(GLOCK), portal.intentos);
    }

    @Test
    void siNoSePuedeRepasarElListadoSeAvisaElApartadoFallido() {
        PortalFalso portal = new PortalFalso(List.of(arma(0, GLOCK)));
        portal.relecturaFalla = true;

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE));

        assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, resultado);
    }

    @Test
    void siUnPasoDelPortalFallaAlApartarSeAvisaYaSinReintentar() {
        PortalFalso portal = new PortalFalso(List.of(arma(0, GLOCK)));
        portal.apartarFalla = true;

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE));

        ResultadoCiclo.ApartadoFallido fallido = assertInstanceOf(ResultadoCiclo.ApartadoFallido.class, resultado);
        assertEquals(List.of(GLOCK), portal.intentos, "no se repite un paso que falló a mitad del camino");
        assertTrue(fallido.causa().contains("cuadro desconocido"));
        assertTrue(fallido.descripcion().contains("Apártela a mano"));
        assertTrue(fallido.descripcion().contains("captura: fallo.png"), "la captura sigue en la bitácora");
    }

    @Test
    void desdeEstadoDeTramiteElCuadroVerArmasYaConfirmadoCuentaComoSellado() {
        PortalFalso portal = new PortalFalso(List.of(arma(0, GLOCK)));
        portal.exitosas = Set.of(GLOCK);
        portal.verArmasConfirmado = true;

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE));

        ResultadoCiclo.ArmaApartada apartada = assertInstanceOf(ResultadoCiclo.ArmaApartada.class, resultado);
        assertTrue(apartada.cierreSellado(), "no debe pedir pulsar FINALIZAR si «Ver armas» ya se confirmó");
        assertFalse(apartada.descripcion().contains("pulsa FINALIZAR"));
    }

    @Test
    void laPrimeraSolicitudEncontradaSinNumeroQuedaFijadaParaLosCiclosSiguientes() {
        PortalFalso portal = new PortalFalso(List.of());
        portal.numeroAbierto = "1-01-45-202610-4336";
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE);

        ResultadoCiclo primero = ciclo.ejecutar(cliente, portal, seguimiento);
        ciclo.ejecutar(cliente, portal, seguimiento);

        assertEquals("1-01-45-202610-4336", seguimiento.numeroSolicitud());
        assertEquals(List.of("", "1-01-45-202610-4336"), portal.numerosPedidos);
        assertTrue(primero.descripcion().contains("1-01-45-202610-4336"));
    }

    @Test
    void elNumeroQueIndicaLaPersonaNoSeReemplaza() {
        PortalFalso portal = new PortalFalso(List.of());
        portal.numeroAbierto = "1-01-45-202610-9999";
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud(ModoInicio.SOLICITUD_EXISTENTE);
        seguimiento.establecer(ModoInicio.SOLICITUD_EXISTENTE, "1-01-45-202610-4336");

        ciclo.ejecutar(cliente, portal, seguimiento);

        assertEquals("1-01-45-202610-4336", seguimiento.numeroSolicitud());
    }

    @Test
    void inventarioSinInteresSoloEsNuevoLaPrimeraVezOSiCambia() {
        InventarioNuevo detector = new InventarioNuevo();
        ResultadoCiclo escopeta = new ResultadoCiclo.SinArmaDeInteres("Estado de trámite", List.of(arma(0, ESCOPETA)));
        ResultadoCiclo otro = new ResultadoCiclo.SinArmaDeInteres("Estado de trámite",
                List.of(arma(0, ESCOPETA), arma(1, CORDOVA)));

        assertTrue(detector.registrar("c", escopeta));
        assertFalse(detector.registrar("c", escopeta), "mismo listado en el ciclo siguiente");
        assertFalse(detector.registrar("c", new ResultadoCiclo.PortalNoResponde("504")));
        assertFalse(detector.registrar("c", escopeta), "un fallo pasajero no lo vuelve nuevo");
        assertTrue(detector.registrar("c", otro), "el listado cambió");
        assertFalse(detector.registrar("c", new ResultadoCiclo.SinInventarioEnSolicitud()));
        assertTrue(detector.registrar("c", otro), "desapareció y volvió a aparecer");
        assertTrue(detector.registrar("otro-cliente", otro), "cada cliente lleva su cuenta");
    }

    private static ArmaInventario arma(int posicion, String descripcion) {
        return new ArmaInventario(posicion, descripcion, descripcion.substring(0, descripcion.indexOf(' ')));
    }

    /** Portal mínimo que se usa siempre desde "Estado de trámite". */
    private static final class PortalFalso implements PortalDriver {
        private final List<ArmaInventario> listado;
        private final List<String> intentos = new ArrayList<>();
        private final List<String> numerosPedidos = new ArrayList<>();
        private Set<String> exitosas = Set.of();
        private boolean alFallarQuitar;
        private boolean relecturaFalla;
        private boolean verArmasConfirmado;
        private boolean apartarFalla;
        private String numeroAbierto = "";
        private int lecturas;

        PortalFalso(List<ArmaInventario> listado) {
            this.listado = new ArrayList<>(listado);
        }

        @Override
        public ResultadoSolicitud abrirSolicitudExistente(ParametrosTramite tramite, String numeroSolicitud) {
            numerosPedidos.add(numeroSolicitud);
            return listado.isEmpty() ? ResultadoSolicitud.SIN_INVENTARIO : ResultadoSolicitud.CON_INVENTARIO;
        }

        @Override
        public String numeroSolicitudAbierta() {
            return numeroAbierto;
        }

        @Override
        public boolean cierreConfirmadoAlApartar() {
            return verArmasConfirmado;
        }

        @Override
        public List<ArmaInventario> leerInventario() {
            if (lecturas++ > 0 && relecturaFalla) {
                throw new PortalInesperadoException("El listado no respondió.");
            }
            return List.copyOf(listado);
        }

        @Override
        public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
            intentos.add(arma.descripcion());
            if (apartarFalla) {
                throw new PortalInesperadoException("Apareció un cuadro desconocido; no se confirma. (captura: fallo.png)");
            }
            if (exitosas.contains(arma.descripcion())) {
                return true;
            }
            if (alFallarQuitar) {
                listado.removeIf(a -> a.descripcion().equals(arma.descripcion()));
            }
            return false;
        }

        @Override
        public List<String> abrirFormulario(ParametrosTramite tramite) {
            throw new AssertionError("Desde Estado de trámite no se usa el paso 1.");
        }

        @Override
        public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
            throw new AssertionError("Desde Estado de trámite no se usa el paso 1.");
        }
    }
}
