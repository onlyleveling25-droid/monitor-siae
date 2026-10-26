package co.monitorsiae.monitoreo;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FiltroBitacoraTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T12:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final EventoOperativo viejo = evento("a", TipoEvento.OPORTUNIDAD, Duration.ofDays(3));
    private final EventoOperativo reciente = evento("b", TipoEvento.MONITOREO_INICIADO, Duration.ofMinutes(5));
    private final EventoOperativo medio = evento("a", TipoEvento.SESION_VENCIDA, Duration.ofHours(5));

    @Test
    void sinCriteriosDevuelveTodoDelMasRecienteAlMasAntiguo() {
        assertEquals(List.of(reciente, medio, viejo),
                FiltroBitacora.TODO.aplicar(List.of(viejo, reciente, medio), reloj));
    }

    @Test
    void filtraPorClienteTipoYPeriodo() {
        List<EventoOperativo> todos = List.of(viejo, reciente, medio);

        assertEquals(List.of(medio, viejo), new FiltroBitacora("a", null, null).aplicar(todos, reloj));
        assertEquals(List.of(medio), new FiltroBitacora(null, TipoEvento.SESION_VENCIDA, null).aplicar(todos, reloj));
        assertEquals(List.of(reciente), new FiltroBitacora(null, null, Duration.ofHours(1)).aplicar(todos, reloj));
        assertEquals(List.of(medio), new FiltroBitacora("a", null, Duration.ofDays(1)).aplicar(todos, reloj));
    }

    private static EventoOperativo evento(String cliente, TipoEvento tipo, Duration antiguedad) {
        return new EventoOperativo(AHORA.minus(antiguedad), cliente, cliente, tipo, "detalle");
    }
}
