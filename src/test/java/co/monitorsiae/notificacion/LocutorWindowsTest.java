package co.monitorsiae.notificacion;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocutorWindowsTest {

    @Test
    void elTextoViajaPorVariableDeEntornoYNoDentroDelComando() {
        String nombrePeligroso = "Ana'; Remove-Item C:\\ -Recurse; '";

        ProcessBuilder proceso = new LocutorWindows().construir(nombrePeligroso);

        assertTrue(proceso.command().stream().noneMatch(parte -> parte.contains("Remove-Item")));
        assertEquals(nombrePeligroso, proceso.environment().get(LocutorWindows.VARIABLE_TEXTO));
    }

    @Test
    void elGuionLeeLaVariableYEsAscii() throws IOException {
        try (InputStream guion = LocutorWindows.class.getResourceAsStream(LocutorWindows.RECURSO_GUION)) {
            byte[] contenido = guion.readAllBytes();

            assertTrue(new String(contenido, StandardCharsets.US_ASCII).contains("$env:" + LocutorWindows.VARIABLE_TEXTO));
            for (byte b : contenido) {
                assertTrue(b >= 0, "PowerShell 5 lee los guiones sin BOM como ANSI; debe ser ASCII");
            }
        }
    }
}
