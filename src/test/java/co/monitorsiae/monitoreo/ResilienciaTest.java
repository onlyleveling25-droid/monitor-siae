package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.OyenteAlertas;
import co.monitorsiae.notificacion.TraductorAlertas;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.ResultadoGuardar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lo que hace que el monitor no se detenga ni suene por cosas pasajeras de la página. */
class ResilienciaTest {

    private static final String MEDELLIN = "05 - Medellín, Antioquia";

    private final CicloMonitoreo ciclo = new CicloMonitoreo(new SelectorSeccional(), new SelectorArma());
    private final Cliente cliente = ClientesDePrueba.valido("Ana");

    @Test
    void siGuardarAbreElInventarioPeroLlegaVacioNoSuenaYPasaAVigilarLaSolicitud() {
        PortalFalso portal = new PortalFalso();
        portal.inventario = List.of();
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, seguimiento);

        assertInstanceOf(ResultadoCiclo.SinInventarioEnSolicitud.class, resultado,
                "un listado vacío es «sin inventario», nunca «sin arma de interés»");
        assertFalse(resultado.detieneMonitoreo());
        assertTrue(seguimiento.siguiendoSolicitud(), "la solicitud ya quedó creada: se vigila en trámite");
    }

    @Test
    void siLaLecturaFallaTrasGuardarIgualSeVigilaLaSolicitudYNoSeCreaOtra() {
        PortalFalso portal = new PortalFalso();
        portal.lecturaFalla = true;
        SeguimientoSolicitud seguimiento = new SeguimientoSolicitud();

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, seguimiento);

        assertInstanceOf(ResultadoCiclo.Inesperado.class, resultado);
        assertTrue(seguimiento.siguiendoSolicitud());
    }

    @Test
    void unErrorDeLaPaginaNoDetieneElMonitorYPideRecargar() {
        PortalFalso portal = new PortalFalso();
        portal.formularioFalla = new IllegalStateException("Timeout 6000ms exceeded");

        ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, new SeguimientoSolicitud());

        assertInstanceOf(ResultadoCiclo.Inesperado.class, resultado);
        assertTrue(resultado.descripcion().contains("Timeout"));
        assertEquals(1, portal.recargasPedidas, "el siguiente ciclo recarga la página");
    }

    @Test
    void elPortalInestableNoHaceSonarNada() {
        CentralAlertas central = new CentralAlertas(List.of());
        OyenteAlertas oyente = new OyenteAlertas(new TraductorAlertas(), central);

        oyente.fallosPersistentes(cliente, 3, "El portal no respondió.");
        oyente.portalRecuperado(cliente, 3);

        assertTrue(central.activas().isEmpty(), "solo queda en la bitácora y en la fila del Panel");
    }

    @Test
    void laEsperaTrasFallosCreceConTopeSalvoQueElIntervaloSeaMayor() {
        MonitorCliente rapido = monitorCon(java.time.Duration.ofSeconds(30));
        assertEquals(java.time.Duration.ofSeconds(60), rapido.esperaTrasFallos(1));
        assertEquals(MonitorCliente.ESPERA_MAXIMA_TRAS_FALLOS, rapido.esperaTrasFallos(5));

        MonitorCliente lento = monitorCon(java.time.Duration.ofMinutes(5));
        assertEquals(java.time.Duration.ofMinutes(5), lento.esperaTrasFallos(3), "nunca menos que el intervalo");
    }

    private MonitorCliente monitorCon(java.time.Duration intervalo) {
        return new MonitorCliente(cliente, new PortalFalso(), ciclo,
                new VentanaHoraria(co.monitorsiae.dominio.HorarioOperacion.porDefecto(), java.time.Clock.systemUTC()),
                intervalo, espera -> { }, new OyenteMonitoreo() { });
    }

    /** Paso 1 con Medellín abierta y GUARDAR que avanza al inventario. */
    private static final class PortalFalso implements PortalDriver {
        List<ArmaInventario> inventario = List.of(new ArmaInventario(0, "Escopeta Pump 12", "Escopeta"));
        boolean lecturaFalla;
        RuntimeException formularioFalla;
        int recargasPedidas;

        @Override
        public List<String> abrirFormulario(ParametrosTramite tramite) {
            if (formularioFalla != null) {
                throw formularioFalla;
            }
            return List.of(MEDELLIN);
        }

        @Override
        public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
            return ResultadoGuardar.AVANZO_PASO_2;
        }

        @Override
        public List<ArmaInventario> leerInventario() {
            if (lecturaFalla) {
                throw new co.monitorsiae.portal.PortalInesperadoException("El paso 2 no mostró filas.");
            }
            return inventario;
        }

        @Override
        public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
            return false;
        }

        @Override
        public void recargarEnElSiguienteCiclo() {
            recargasPedidas++;
        }
    }
}
