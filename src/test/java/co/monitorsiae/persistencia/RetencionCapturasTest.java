package co.monitorsiae.persistencia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetencionCapturasTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    @TempDir
    Path carpeta;

    private RetencionCapturas retencion() {
        return new RetencionCapturas(carpeta, Duration.ofDays(30), reloj);
    }

    private Path archivo(String nombre, Duration antiguedad) throws IOException {
        Path ruta = carpeta.resolve(nombre);
        Files.writeString(ruta, "x");
        Files.setLastModifiedTime(ruta, FileTime.from(AHORA.minus(antiguedad)));
        return ruta;
    }

    @Test
    void borraSoloLasCapturasConMasDeTreintaDias() throws IOException {
        Path vieja = archivo("fallo-20260901-101010.png", Duration.ofDays(31));
        Path limite = archivo("fallo-20260926-101010.png", Duration.ofDays(29));
        Path reciente = archivo("fallo-20261026-140348.png", Duration.ofMinutes(5));

        assertEquals(1, retencion().limpiar());

        assertFalse(Files.exists(vieja));
        assertTrue(Files.exists(limite));
        assertTrue(Files.exists(reciente));
    }

    @Test
    void noTocaArchivosQueNoSonCapturasDelPortal() throws IOException {
        Path otro = archivo("notas.txt", Duration.ofDays(400));
        Path casiIgual = archivo("fallo-2026.png", Duration.ofDays(400));
        Path otraExtension = archivo("fallo-20260101-101010.jpg", Duration.ofDays(400));
        Path subcarpeta = Files.createDirectory(carpeta.resolve("fallo-20260101-101010.png"));
        Path dentro = subcarpeta.resolve("fallo-20250101-101010.png");
        Files.writeString(dentro, "x");
        Files.setLastModifiedTime(dentro, FileTime.from(AHORA.minus(Duration.ofDays(400))));

        assertEquals(0, retencion().limpiar());

        assertTrue(Files.exists(otro) && Files.exists(casiIgual) && Files.exists(otraExtension));
        assertTrue(Files.isDirectory(subcarpeta));
        assertTrue(Files.exists(dentro));
    }

    @Test
    void sinCarpetaNoHaceNadaNiFalla() {
        assertEquals(0, new RetencionCapturas(carpeta.resolve("no-existe"), Duration.ofDays(30), reloj).limpiar());
    }

    @Test
    void repetirLaLimpiezaEsInofensivo() throws IOException {
        archivo("fallo-20260901-101010.png", Duration.ofDays(60));

        assertEquals(1, retencion().limpiar());
        assertEquals(0, retencion().limpiar());
    }
}
