package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.persistencia.RepositorioBitacora;
import co.monitorsiae.persistencia.RepositorioBitacoraMemoria;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OyenteBitacoraTest {

    private final Cliente ana = ClientesDePrueba.valido("Ana");
    private final RepositorioBitacoraMemoria bitacora = new RepositorioBitacoraMemoria();
    private final OyenteBitacora oyente = new OyenteBitacora(bitacora,
            Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void registraInicioHallazgosYDetencion() {
        ArmaInventario arma = new ArmaInventario(1, "Pistola Glock G-43", "Pistola");
        oyente.alIniciar(ana);
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.ArmaEncontrada("Medellín",
                new EleccionArma(arma, MotivoEleccion.OBJETIVO, 0)));
        oyente.alDetenerse(ana, MotivoDetencion.ARMA_ENCONTRADA);

        List<TipoEvento> tipos = bitacora.eventos().stream().map(EventoOperativo::tipo).toList();
        assertEquals(List.of(TipoEvento.MONITOREO_DETENIDO, TipoEvento.OPORTUNIDAD,
                TipoEvento.MONITOREO_INICIADO), tipos);
        assertEquals("Ana", bitacora.eventos().getFirst().clienteNombre());
        assertTrue(bitacora.eventos().get(1).detalle().contains("Pistola Glock G-43"));
    }

    @Test
    void noRegistraLosCiclosSinNovedad() {
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.SeccionalesCerradas());
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.SinInventario("Medellín"));
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.SinSeccionalObjetivo(List.of("Cali")));

        assertTrue(bitacora.eventos().isEmpty());
    }

    @Test
    void unMismoHallazgoRepetidoSeAnotaUnaSolaVezHastaQueCambieElContexto() {
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.Inesperado("pantalla desconocida"));
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.Inesperado("pantalla desconocida"));
        assertEquals(1, bitacora.eventos().size());

        oyente.alTerminarCiclo(ana, new ResultadoCiclo.Inesperado("otra pantalla"));
        assertEquals(2, bitacora.eventos().size());

        oyente.alIniciar(ana);
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.Inesperado("otra pantalla"));
        assertEquals(4, bitacora.eventos().size());
    }

    @Test
    void unFalloAlEscribirNoInterrumpeElMonitoreo() {
        RepositorioBitacora roto = new RepositorioBitacora() {
            @Override
            public void registrar(EventoOperativo evento) {
                throw new IllegalStateException("disco lleno");
            }

            @Override
            public List<EventoOperativo> eventos() {
                return List.of();
            }
        };
        OyenteBitacora conFallo = new OyenteBitacora(roto, Clock.systemUTC());

        assertDoesNotThrow(() -> conFallo.alIniciar(ana));
        assertDoesNotThrow(() -> conFallo.alTerminarCiclo(ana, new ResultadoCiclo.SesionVencida("x")));
    }

    @Test
    void elDetalleLargoSeAcortaYNoConservaSaltosDeLinea() {
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.SesionVencida("a\n".repeat(400)));

        String detalle = bitacora.eventos().getFirst().detalle();
        assertTrue(detalle.length() <= EventoOperativo.LARGO_MAXIMO_DETALLE);
        assertTrue(!detalle.contains("\n"));
    }
}
