package co.monitorsiae.dominio;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NivelEscalamientoTest {

    @Test
    void esperaNegativaSeNormalizaACero() {
        NivelEscalamiento nivel = new NivelEscalamiento(Duration.ofSeconds(-5), true, true, TonoAlarma.SIRENA);

        assertEquals(Duration.ZERO, nivel.espera());
    }

    @Test
    void esperaNulaSeNormalizaACero() {
        NivelEscalamiento nivel = new NivelEscalamiento(null, false, false, TonoAlarma.SIRENA);

        assertEquals(Duration.ZERO, nivel.espera());
    }

    @Test
    void tonoNuloUsaSirenaPorDefecto() {
        NivelEscalamiento nivel = new NivelEscalamiento(Duration.ofMinutes(1), true, false, null);

        assertEquals(TonoAlarma.SIRENA, nivel.tono());
    }

    @Test
    void conservaForzarSonidoYForzarVoz() {
        NivelEscalamiento nivel = new NivelEscalamiento(Duration.ofMinutes(2), true, false, TonoAlarma.DOBLE);

        assertTrue(nivel.forzarSonido());
        assertFalse(nivel.forzarVoz());
        assertEquals(TonoAlarma.DOBLE, nivel.tono());
    }
}
