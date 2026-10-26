package co.monitorsiae.app;

import co.monitorsiae.monitoreo.EventoOperativo;
import co.monitorsiae.monitoreo.TipoEvento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportadorBitacoraCsvTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant MOMENTO = Instant.parse("2026-10-26T15:00:00Z");

    @TempDir
    Path carpeta;

    @Test
    void generaEncabezadoYUnaFilaPorEvento() {
        EventoOperativo evento = new EventoOperativo(MOMENTO, "c1", "Ana", TipoEvento.MONITOREO_DETENIDO,
                "Monitoreo detenido.", "", "", Duration.ofSeconds(312));

        List<String> lineas = ExportadorBitacoraCsv.generar(List.of(evento), BOGOTA).lines().toList();

        assertEquals("Fecha y hora;Cliente;Tipo;Seccional;Arma;Duración;Captura;Detalle", lineas.get(0));
        assertEquals("2026-10-26 10:00:00;Ana;Monitoreo detenido;;;5 min 12 s;;Monitoreo detenido.", lineas.get(1));
        assertEquals(2, lineas.size());
    }

    @Test
    void escapaSeparadoresYComillas() {
        EventoOperativo evento = new EventoOperativo(MOMENTO, "c1", "Pérez; Ana", TipoEvento.OPORTUNIDAD,
                "Arma \"Glock\" 9mm", "05 - Medellín", "Glock; G-43", null);

        String csv = ExportadorBitacoraCsv.generar(List.of(evento), BOGOTA);

        assertTrue(csv.contains("\"Pérez; Ana\""));
        assertTrue(csv.contains("\"Arma \"\"Glock\"\" 9mm\""));
        assertTrue(csv.contains("\"Glock; G-43\""));
    }

    @Test
    void neutralizaCeldasQueSePodrianEjecutarComoFormula() {
        EventoOperativo evento = new EventoOperativo(MOMENTO, "c1", "=HYPERLINK(\"http://x\")", TipoEvento.OPORTUNIDAD,
                "+cmd|' /C calc'!A0", "@SUMA(1)", "-2+3", null);

        String fila = ExportadorBitacoraCsv.generar(List.of(evento), BOGOTA).lines().toList().get(1);

        assertTrue(fila.contains("'=HYPERLINK"));
        assertTrue(fila.contains("'+cmd"));
        assertTrue(fila.contains("'@SUMA(1)"));
        assertTrue(fila.contains("'-2+3"));
    }

    @Test
    void elArchivoLlevaLaMarcaUtf8ParaExcel() throws IOException {
        Path destino = carpeta.resolve("bitacora.csv");

        ExportadorBitacoraCsv.escribir(destino, List.of(), BOGOTA);

        byte[] bytes = Files.readAllBytes(destino);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("Duración"));
    }
}
