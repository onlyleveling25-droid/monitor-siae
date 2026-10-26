package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.EventoOperativo;
import co.monitorsiae.monitoreo.TipoEvento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioBitacoraJsonTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T12:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    @TempDir
    Path carpeta;

    @Test
    void conservaLosEventosEntreReinicios() {
        Path archivo = carpeta.resolve("bitacora.json");
        EventoOperativo evento = evento(Duration.ofMinutes(1), "uno");
        new RepositorioBitacoraJson(archivo, reloj).registrar(evento);

        assertEquals(java.util.List.of(evento), new RepositorioBitacoraJson(archivo, reloj).eventos());
    }

    @Test
    void sinArchivoLaBitacoraEstaVacia() {
        assertTrue(new RepositorioBitacoraJson(carpeta.resolve("no-existe.json"), reloj).eventos().isEmpty());
    }

    @Test
    void descartaLoQueSuperaLaRetencion() {
        RepositorioBitacoraJson repositorio = new RepositorioBitacoraJson(carpeta.resolve("b.json"), reloj);
        repositorio.registrar(evento(RepositorioBitacoraJson.RETENCION.plusDays(1), "caducado"));
        repositorio.registrar(evento(Duration.ofDays(1), "vigente"));

        assertEquals(java.util.List.of("vigente"),
                repositorio.eventos().stream().map(EventoOperativo::detalle).toList());
    }

    @Test
    void limitaElNumeroDeEventosAlMasReciente() {
        RepositorioBitacoraJson repositorio = new RepositorioBitacoraJson(carpeta.resolve("b.json"), reloj);
        for (int i = 0; i < RepositorioBitacoraJson.MAXIMO_EVENTOS + 5; i++) {
            repositorio.registrar(evento(Duration.ofSeconds(1), "evento " + i));
        }

        assertEquals(RepositorioBitacoraJson.MAXIMO_EVENTOS, repositorio.eventos().size());
        assertEquals("evento " + (RepositorioBitacoraJson.MAXIMO_EVENTOS + 4),
                repositorio.eventos().getFirst().detalle());
    }

    @Test
    void unArchivoDanadoSeReportaSinSobrescribirlo() throws Exception {
        Path archivo = carpeta.resolve("b.json");
        Files.writeString(archivo, "{no es json");
        RepositorioBitacoraJson repositorio = new RepositorioBitacoraJson(archivo, reloj);

        assertThrows(UncheckedIOException.class, repositorio::eventos);
        assertThrows(UncheckedIOException.class, () -> repositorio.registrar(evento(Duration.ZERO, "x")));
        assertEquals("{no es json", Files.readString(archivo));
    }

    private EventoOperativo evento(Duration antiguedad, String detalle) {
        return new EventoOperativo(AHORA.minus(antiguedad), "c1", "Ana", TipoEvento.OPORTUNIDAD, detalle);
    }

    @Test
    void leeEventosGuardadosAntesDeSeccionalArmaYDuracion() throws Exception {
        Path archivo = carpeta.resolve("bitacora.json");
        Files.writeString(archivo, """
                [ { "momento": "2026-10-26T11:00:00Z", "clienteId": "c1", "clienteNombre": "Ana",
                    "tipo": "OPORTUNIDAD", "detalle": "Arma disponible" } ]
                """);

        EventoOperativo leido = new RepositorioBitacoraJson(archivo, reloj).eventos().getFirst();

        assertEquals("Arma disponible", leido.detalle());
        assertEquals("", leido.seccional());
        assertEquals("", leido.arma());
        assertEquals(null, leido.duracion());
    }

    @Test
    void conservaSeccionalArmaYDuracion() {
        Path archivo = carpeta.resolve("b.json");
        EventoOperativo evento = new EventoOperativo(AHORA.minusSeconds(5), "c1", "Ana",
                TipoEvento.MONITOREO_DETENIDO, "Detenido", "05 - Medellín", "Glock G-43", Duration.ofSeconds(312));
        new RepositorioBitacoraJson(archivo, reloj).registrar(evento);

        assertEquals(evento, new RepositorioBitacoraJson(archivo, reloj).eventos().getFirst());
    }

    @Test
    void conservaElNombreDeLaCaptura() {
        Path archivo = carpeta.resolve("c.json");
        EventoOperativo evento = new EventoOperativo(AHORA.minusSeconds(5), "c1", "Ana",
                TipoEvento.RESPUESTA_INESPERADA, "No se abrió (captura: fallo-20261026-140348.png)", "", "", null,
                "fallo-20261026-140348.png");
        new RepositorioBitacoraJson(archivo, reloj).registrar(evento);

        assertEquals("fallo-20261026-140348.png",
                new RepositorioBitacoraJson(archivo, reloj).eventos().getFirst().captura());
    }
}
