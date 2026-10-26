package co.monitorsiae.dominio;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenciasAlertasTest {

    @Test
    void porDefectoTraeElEscalamientoDeshabilitado() {
        PreferenciasAlertas preferencias = PreferenciasAlertas.porDefecto();

        assertFalse(preferencias.perfilEscalamiento().habilitado());
        assertEquals(TonoAlarma.SIRENA, preferencias.tono());
    }

    @Test
    void elConstructorDeCompatibilidadDejaElEscalamientoDeshabilitado() {
        PreferenciasAlertas preferencias = new PreferenciasAlertas(true, false, TonoAlarma.DOBLE);

        assertFalse(preferencias.perfilEscalamiento().habilitado());
        assertEquals(TonoAlarma.DOBLE, preferencias.tono());
        assertTrue(preferencias.sonidoHabilitado());
        assertFalse(preferencias.vozHabilitada());
    }

    @Test
    void unPerfilDeEscalamientoNuloSeReemplazaPorElDeshabilitado() {
        PreferenciasAlertas preferencias = new PreferenciasAlertas(true, true, TonoAlarma.SIRENA, null);

        assertFalse(preferencias.perfilEscalamiento().habilitado());
    }

    @Test
    void unTonoNuloSeReemplazaPorSirena() {
        PreferenciasAlertas preferencias = new PreferenciasAlertas(true, true, null);

        assertEquals(TonoAlarma.SIRENA, preferencias.tono());
    }

    @Test
    void conservaUnPerfilDeEscalamientoExplicito() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(new NivelEscalamiento(Duration.ofMinutes(2), true, true, TonoAlarma.DOBLE)));
        PreferenciasAlertas preferencias = new PreferenciasAlertas(true, true, TonoAlarma.SIRENA, perfil);

        assertEquals(perfil, preferencias.perfilEscalamiento());
    }
}
