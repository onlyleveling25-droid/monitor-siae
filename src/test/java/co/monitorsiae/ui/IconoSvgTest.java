package co.monitorsiae.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class IconoSvgTest {

    @Test
    void todosLosIconosDeLaInterfazSePuedenCargarComoSvg() {
        for (String nombre : List.of("monitor", "dashboard", "settings", "activity", "bell",
                "diagnostics", "more", "check", "info", "alert", "close")) {
            assertNotNull(IconoSvg.crear(nombre, 18, "#1D4ED8"), nombre);
        }
    }
}
