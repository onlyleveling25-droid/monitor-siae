package co.monitorsiae.monitoreo;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumenYTiempoBitacoraTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void resumenCuentaSoloElPeriodoYAgrupaLosProblemas() {
        List<EventoOperativo> eventos = List.of(
                evento(TipoEvento.OPORTUNIDAD, Duration.ofMinutes(5)),
                evento(TipoEvento.OPORTUNIDAD, Duration.ofHours(30)),
                evento(TipoEvento.ARMA_APARTADA, Duration.ofHours(1)),
                evento(TipoEvento.SESION_VENCIDA, Duration.ofHours(2)),
                evento(TipoEvento.APARTADO_FALLIDO, Duration.ofHours(3)),
                evento(TipoEvento.RESPUESTA_INESPERADA, Duration.ofHours(4)),
                evento(TipoEvento.MONITOREO_INICIADO, Duration.ofHours(4)));

        ResumenBitacora resumen = ResumenBitacora.de(eventos, reloj, Duration.ofHours(24));

        assertEquals(new ResumenBitacora(1, 1, 3), resumen);
    }

    @Test
    void tiempoRelativoUsaUnidadesYLuegoLaFecha() {
        assertEquals("hace unos segundos", TiempoRelativo.texto(AHORA.minusSeconds(30), reloj, BOGOTA));
        assertEquals("hace unos segundos", TiempoRelativo.texto(AHORA.plusSeconds(30), reloj, BOGOTA));
        assertEquals("hace 5 min", TiempoRelativo.texto(AHORA.minus(Duration.ofMinutes(5)), reloj, BOGOTA));
        assertEquals("hace 3 h", TiempoRelativo.texto(AHORA.minus(Duration.ofHours(3)), reloj, BOGOTA));
        // 2026-10-24 15:00 UTC = 10:00 en Colombia.
        assertEquals("24/10 10:00", TiempoRelativo.texto(AHORA.minus(Duration.ofDays(2)), reloj, BOGOTA));
        assertTrue(TiempoRelativo.completo(AHORA, BOGOTA).contains("26/10/2026 10:00:00"));
    }

    @Test
    void duracionLegibleSegunLaMagnitud() {
        assertEquals("", evento(TipoEvento.MONITOREO_DETENIDO, Duration.ZERO).duracionTexto());
        assertEquals("45 s", conDuracion(Duration.ofSeconds(45)).duracionTexto());
        assertEquals("5 min 12 s", conDuracion(Duration.ofSeconds(312)).duracionTexto());
        assertEquals("2 h 3 min", conDuracion(Duration.ofMinutes(123)).duracionTexto());
    }

    @Test
    void lasCategoriasDeTipoSonCoherentes() {
        assertTrue(TipoEvento.OPORTUNIDAD.importante() && TipoEvento.ARMA_APARTADA.importante());
        assertTrue(TipoEvento.SESION_VENCIDA.problema() && !TipoEvento.OPORTUNIDAD.problema());
        assertTrue(TipoEvento.APARTADO_FALLIDO.conInventario() && !TipoEvento.MONITOREO_INICIADO.conInventario());
    }

    private EventoOperativo evento(TipoEvento tipo, Duration antiguedad) {
        return new EventoOperativo(AHORA.minus(antiguedad), "c1", "Ana", tipo, "d");
    }

    private EventoOperativo conDuracion(Duration duracion) {
        return new EventoOperativo(AHORA, "c1", "Ana", TipoEvento.MONITOREO_DETENIDO, "d", "", "", duracion);
    }
}
