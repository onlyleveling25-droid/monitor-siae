package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.HorarioOperacion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VentanaHorariaTest {

    private static final HorarioOperacion LUNES_A_VIERNES = new HorarioOperacion(true,
            List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            LocalTime.of(8, 0), LocalTime.of(16, 0), HorarioOperacion.ZONA_COLOMBIA);

    @Test
    void usaLaHoraDeColombiaAunqueElRelojEsteEnOtraZona() {
        // Viernes 16 de octubre de 2026: 12:59 UTC son las 07:59 en Colombia.
        assertFalse(ventana("2026-10-16T12:59:00Z").permiteAhora());
        assertTrue(ventana("2026-10-16T13:00:00Z").permiteAhora());
        assertTrue(ventana("2026-10-16T20:59:00Z").permiteAhora());
        assertFalse(ventana("2026-10-16T21:00:00Z").permiteAhora());
    }

    @Test
    void respetaLosDias() {
        assertFalse(ventana("2026-10-17T15:00:00Z").permiteAhora());
    }

    @Test
    void sinRestriccionSiemprePermite() {
        Clock domingoDeMadrugada = Clock.fixed(Instant.parse("2026-10-18T07:00:00Z"), ZoneOffset.UTC);

        assertTrue(new VentanaHoraria(HorarioOperacion.porDefecto(), domingoDeMadrugada).permiteAhora());
    }

    private static VentanaHoraria ventana(String instante) {
        return new VentanaHoraria(LUNES_A_VIERNES, Clock.fixed(Instant.parse(instante), ZoneOffset.UTC));
    }
}
