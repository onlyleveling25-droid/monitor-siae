package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.monitoreo.EleccionArma;
import co.monitorsiae.monitoreo.MotivoDetencion;
import co.monitorsiae.monitoreo.MotivoEleccion;
import co.monitorsiae.monitoreo.ResultadoCiclo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraductorAlertasTest {

    private static final String SECCIONAL = "05 - Medellín, Antioquia";

    private final TraductorAlertas traductor = new TraductorAlertas();
    private final Cliente cliente = ClientesDePrueba.valido("Ana Pérez");
    private final ArmaInventario glock = new ArmaInventario(0, "Pistola Glock Mod G-43 RAIL CAL 9mm", "");

    @Test
    void armaApartadaEsUnaOportunidadConElNombreDelCliente() {
        Alerta alerta = traductor.deResultado(cliente, new ResultadoCiclo.ArmaApartada(SECCIONAL,
                new EleccionArma(glock, MotivoEleccion.OBJETIVO, 0), 1)).orElseThrow();

        assertEquals(cliente.id(), alerta.clienteId());
        assertEquals(Gravedad.OPORTUNIDAD, alerta.gravedad());
        assertTrue(alerta.titulo().contains("Ana Pérez"));
        assertTrue(alerta.detalle().contains("G-43"));
        assertTrue(alerta.textoVoz().contains("Ana Pérez"));
        assertFalse(alerta.textoVoz().contains("G-43"));
    }

    @Test
    void avisaCuandoSeTomoUnReemplazo() {
        Alerta alerta = traductor.deResultado(cliente, new ResultadoCiclo.ArmaApartada(SECCIONAL,
                new EleccionArma(glock, MotivoEleccion.ALTERNATIVA, 0), 1)).orElseThrow();

        assertTrue(alerta.detalle().contains("alternativa"));
    }

    @Test
    void armaNoApartadaSigueSiendoUnaOportunidadQueDebeAlertar() {
        Alerta alerta = traductor.deResultado(cliente, new ResultadoCiclo.ApartadoFallido(SECCIONAL,
                new EleccionArma(glock, MotivoEleccion.OBJETIVO, 0), 2)).orElseThrow();

        assertEquals(Gravedad.OPORTUNIDAD, alerta.gravedad());
        assertTrue(alerta.titulo().contains("Ana Pérez"));
    }

    @Test
    void sesionVencidaYFallosRepetidosSonProblemas() {
        assertEquals(Gravedad.PROBLEMA, traductor.deResultado(cliente,
                new ResultadoCiclo.SesionVencida("login")).orElseThrow().gravedad());
        assertEquals(Gravedad.PROBLEMA, traductor.deDetencion(cliente,
                MotivoDetencion.FALLOS_REPETIDOS).orElseThrow().gravedad());
    }

    @Test
    void siLaSolicitudNoApareceSeAvisaUnaVezConLosPasosASeguir() {
        Alerta alerta = traductor.deResultado(cliente, new ResultadoCiclo.SolicitudNoEncontrada(
                "Monitoreo detenido.", "Qué hacer: 1. Abre Estado de trámite.")).orElseThrow();
        assertEquals(Gravedad.PROBLEMA, alerta.gravedad());
        assertTrue(alerta.detalle().contains("Qué hacer"));
        assertTrue(traductor.deDetencion(cliente, MotivoDetencion.SOLICITUD_NO_ENCONTRADA).isEmpty(),
                "no se repite la alerta al detenerse");
    }

    @Test
    void laSolicitudSinInventarioNoSuena() {
        assertTrue(traductor.deResultado(cliente, new ResultadoCiclo.SinInventarioEnSolicitud()).isEmpty());
    }

    @Test
    void inventarioSinArmaDeInteresAvisaConLaCantidadYSinSeleccionarNada() {
        Alerta alerta = traductor.deResultado(cliente, new ResultadoCiclo.SinArmaDeInteres(SECCIONAL, List.of(
                new co.monitorsiae.dominio.ArmaInventario(0, "Revolver Martial Cal 38", "Revolver"),
                new co.monitorsiae.dominio.ArmaInventario(1, "Escopeta Pump 12", "Escopeta")))).orElseThrow();

        assertEquals(Gravedad.OPORTUNIDAD, alerta.gravedad());
        assertTrue(alerta.detalle().contains("2 armas"));
        assertTrue(alerta.detalle().contains("no se seleccionó nada"));
    }

    @Test
    void losEstadosNormalesNoGeneranAlerta() {
        assertTrue(traductor.deResultado(cliente, new ResultadoCiclo.SeccionalesCerradas()).isEmpty());
        assertTrue(traductor.deResultado(cliente, new ResultadoCiclo.SinInventario(SECCIONAL)).isEmpty());
        assertTrue(traductor.deResultado(cliente, new ResultadoCiclo.Inesperado("503")).isEmpty());
        assertTrue(traductor.deDetencion(cliente, MotivoDetencion.ARMA_APARTADA).isEmpty());
        assertTrue(traductor.deDetencion(cliente, MotivoDetencion.SOLICITADO_POR_USUARIO).isEmpty());
    }
}
