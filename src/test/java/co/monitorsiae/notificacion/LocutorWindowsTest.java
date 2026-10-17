package co.monitorsiae.notificacion;

import org.junit.jupiter.api.Test;

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
}
