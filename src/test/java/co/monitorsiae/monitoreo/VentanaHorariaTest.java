package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.HorarioOperacion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void cadaDiaTieneSuPropioRango() {
        HorarioOperacion distinto = new HorarioOperacion(true, java.util.Map.of(
                DayOfWeek.MONDAY, new co.monitorsiae.dominio.FranjaHoraria(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                DayOfWeek.SATURDAY, new co.monitorsiae.dominio.FranjaHoraria(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                HorarioOperacion.ZONA_COLOMBIA);
        // Lunes 2026-10-26 y sábado 2026-10-31; 10:00 en Colombia = 15:00 UTC, 13:00 = 18:00 UTC.
        VentanaHoraria lunes10 = new VentanaHoraria(distinto, Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneOffset.UTC));
        VentanaHoraria lunes13 = new VentanaHoraria(distinto, Clock.fixed(Instant.parse("2026-10-26T18:00:00Z"), ZoneOffset.UTC));
        VentanaHoraria sabado10 = new VentanaHoraria(distinto, Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneOffset.UTC));
        VentanaHoraria sabado13 = new VentanaHoraria(distinto, Clock.fixed(Instant.parse("2026-10-31T18:00:00Z"), ZoneOffset.UTC));
        VentanaHoraria martes10 = new VentanaHoraria(distinto, Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZoneOffset.UTC));

        assertTrue(lunes10.permiteAhora());
        assertTrue(lunes13.permiteAhora());
        assertTrue(sabado10.permiteAhora());
        assertFalse(sabado13.permiteAhora());
        assertFalse(martes10.permiteAhora());
        assertTrue(sabado13.motivoFueraDeHorario().contains("hoy sábado: 09:00 a. m. a 12:00 p. m."));
        assertTrue(sabado13.motivoFueraDeHorario().contains("el lunes a las 08:00 a. m."));
        assertTrue(martes10.motivoFueraDeHorario().contains("hoy martes: sin consulta"));
    }

    @Test
    void anunciaCuandoSeRetomaLaConsulta() {
        HorarioOperacion horario = new HorarioOperacion(true, java.util.Map.of(
                DayOfWeek.MONDAY, new co.monitorsiae.dominio.FranjaHoraria(LocalTime.of(7, 0), LocalTime.of(10, 0)),
                DayOfWeek.TUESDAY, new co.monitorsiae.dominio.FranjaHoraria(LocalTime.of(7, 0), LocalTime.of(19, 0))),
                HorarioOperacion.ZONA_COLOMBIA);
        // Lunes 2026-10-26 10:30 en Colombia = 15:30 UTC; a las 05:00 (10:00 UTC) aún no abre.
        VentanaHoraria despuesDeCerrar = new VentanaHoraria(horario,
                Clock.fixed(Instant.parse("2026-10-26T15:30:00Z"), ZoneOffset.UTC));
        VentanaHoraria antesDeAbrir = new VentanaHoraria(horario,
                Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC));

        assertFalse(despuesDeCerrar.permiteAhora());
        assertTrue(despuesDeCerrar.motivoFueraDeHorario().contains("mañana a las 07:00 a. m."));
        assertTrue(antesDeAbrir.motivoFueraDeHorario().contains("hoy a las 07:00 a. m."));
        assertEquals(java.time.LocalTime.of(7, 0), despuesDeCerrar.proximaApertura().orElseThrow().toLocalTime());
    }

    @Test
    void sinNingunDiaConfiguradoNoHayProximaApertura() {
        VentanaHoraria vacia = new VentanaHoraria(
                new HorarioOperacion(true, java.util.Map.of(), HorarioOperacion.ZONA_COLOMBIA),
                Clock.fixed(Instant.parse("2026-10-26T15:30:00Z"), ZoneOffset.UTC));

        assertTrue(vacia.proximaApertura().isEmpty());
        assertTrue(vacia.motivoFueraDeHorario().contains("Ningún día tiene horario"));
    }
}
