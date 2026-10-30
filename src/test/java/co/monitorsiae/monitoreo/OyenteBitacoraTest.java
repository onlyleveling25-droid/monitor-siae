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
    void inventarioSinInteresSeAnotaUnaVezConLaListaCompleta() {
        ResultadoCiclo sinInteres = new ResultadoCiclo.SinArmaDeInteres("Estado de trámite", List.of(
                new ArmaInventario(0, "Revolver Martial Cal 38", "Revolver"),
                new ArmaInventario(1, "Escopeta Winchester SXP Cal 12", "Escopeta")));

        oyente.alTerminarCiclo(ana, sinInteres);
        oyente.alTerminarCiclo(ana, sinInteres);

        assertEquals(1, bitacora.eventos().size());
        EventoOperativo evento = bitacora.eventos().getFirst();
        assertEquals(TipoEvento.INVENTARIO_SIN_INTERES, evento.tipo());
        assertTrue(evento.detalle().contains("no se seleccionó nada"));
        assertTrue(evento.detalle().contains("Revolver Martial Cal 38"));
        assertTrue(evento.detalle().contains("Escopeta Winchester SXP Cal 12"));
        assertTrue(evento.tipo().conInventario(), "debe poder abrirse el inventario desde la bitácora");
    }

    @Test
    void anotaQueElPortalNoRespondeUnaSolaVezMientrasDure() {
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.PortalNoResponde("GUARDAR no tuvo respuesta."));
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.PortalNoResponde("GUARDAR no tuvo respuesta."));

        assertEquals(1, bitacora.eventos().size());
        assertEquals(TipoEvento.PORTAL_SIN_RESPUESTA, bitacora.eventos().getFirst().tipo());
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

    @Test
    void elHallazgoGuardaSeccionalYArmaPorSeparado() {
        ArmaInventario arma = new ArmaInventario(1, "Pistola Glock G-43", "Pistola");
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.ArmaApartada("05 - Medellín",
                new EleccionArma(arma, MotivoEleccion.OBJETIVO, 0), 1));

        EventoOperativo evento = bitacora.eventos().getFirst();
        assertEquals("05 - Medellín", evento.seccional());
        assertEquals("Pistola Glock G-43", evento.arma());
        assertEquals(TipoEvento.ARMA_APARTADA, evento.tipo());
    }

    @Test
    void laDetencionRegistraCuantoDuroElMonitoreo() {
        java.util.concurrent.atomic.AtomicReference<Instant> ahora =
                new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-10-26T12:00:00Z"));
        Clock reloj = new Clock() {
            public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId zona) { return this; }
            public Instant instant() { return ahora.get(); }
        };
        OyenteBitacora conReloj = new OyenteBitacora(bitacora, reloj);

        conReloj.alIniciar(ana);
        ahora.set(Instant.parse("2026-10-26T12:05:12Z"));
        conReloj.alDetenerse(ana, MotivoDetencion.SOLICITADO_POR_USUARIO);

        EventoOperativo detenido = bitacora.eventos().getFirst();
        assertEquals(java.time.Duration.ofSeconds(312), detenido.duracion());
        assertTrue(detenido.detalle().contains("Duró 5 min 12 s"));
    }

    @Test
    void unaDetencionSinInicioPrevioNoInventaDuracion() {
        oyente.alDetenerse(ana, MotivoDetencion.ERROR_INESPERADO);

        assertEquals(null, bitacora.eventos().getFirst().duracion());
    }

    @Test
    void elInicioRegistraLasSeccionalesQueBuscaElCliente() {
        oyente.alIniciar(ana);

        EventoOperativo inicio = bitacora.eventos().getFirst();
        assertEquals("Medellín, Bogotá", inicio.seccional());
        assertTrue(inicio.detalle().contains("Seccionales que busca: Medellín, Bogotá"));
        assertTrue(new FiltroBitacora(null, null, null, "medellin").aplicar(bitacora.eventos(),
                Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC)).contains(inicio));
    }

    @Test
    void laRespuestaInesperadaGuardaElNombreDeLaCaptura() {
        oyente.alTerminarCiclo(ana, new ResultadoCiclo.Inesperado(
                "No se abrió el formulario del paso 1. (captura: fallo-20261026-140348.png)"));

        EventoOperativo evento = bitacora.eventos().getFirst();
        assertEquals(TipoEvento.RESPUESTA_INESPERADA, evento.tipo());
        assertEquals("fallo-20261026-140348.png", evento.captura());
    }

    @Test
    void soloSeAceptaElNombreExactoDeUnaCapturaDelPortal() {
        assertEquals("", EventoOperativo.capturaEn("sin captura"));
        assertEquals("", EventoOperativo.capturaEn("(captura: ../../etc/passwd)"));
        assertEquals("", EventoOperativo.capturaEn("(captura: fallo-1.png)"));
        assertEquals("", EventoOperativo.capturaEn(null));
        assertEquals("", new EventoOperativo(Instant.now(), "c", "Ana", TipoEvento.RESPUESTA_INESPERADA, "d",
                "", "", null, "../fallo-20261026-140348.png").captura());
        assertEquals("", new EventoOperativo(Instant.now(), "c", "Ana", TipoEvento.RESPUESTA_INESPERADA, "d",
                "", "", null, "C:\\Windows\\fallo-20261026-140348.png").captura());
    }
}
