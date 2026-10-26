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

    @Test
    void buscaPorTextoSinImportarTildesMayusculasNiGuiones() {
        EventoOperativo glock = new EventoOperativo(AHORA.minusSeconds(10), "a", "Ana", TipoEvento.OPORTUNIDAD,
                "Arma disponible", "05 - Medellín", "Pistola Glock G-43", null);
        EventoOperativo cz = new EventoOperativo(AHORA.minusSeconds(20), "b", "Luis", TipoEvento.ARMA_APARTADA,
                "Arma apartada", "16 - Bogotá", "Pistola CZ P10", null);
        List<EventoOperativo> todos = List.of(glock, cz, viejo);

        assertEquals(List.of(glock), new FiltroBitacora(null, null, null, "GLOCK g43").aplicar(todos, reloj));
        assertEquals(List.of(cz), new FiltroBitacora(null, null, null, "bogota").aplicar(todos, reloj));
        assertEquals(List.of(cz), new FiltroBitacora(null, null, null, "luis apartada").aplicar(todos, reloj));
        assertEquals(List.of(), new FiltroBitacora(null, null, null, "glock cz").aplicar(todos, reloj));
        assertEquals(3, new FiltroBitacora(null, null, null, "   ").aplicar(todos, reloj).size());
    }

    @Test
    void indicaSiHayAlgunCriterioActivo() {
        assertEquals(false, FiltroBitacora.TODO.activo());
        assertEquals(true, new FiltroBitacora("a", null, null).activo());
        assertEquals(true, new FiltroBitacora(null, null, null, "glock").activo());
        assertEquals(false, new FiltroBitacora(null, null, null, " - ").activo());
    }

    @Test
    void variasAlternativasSeSeparanConOComaOBarra() {
        EventoOperativo medellin = new EventoOperativo(AHORA.minusSeconds(10), "a", "Ana", TipoEvento.OPORTUNIDAD,
                "Arma disponible", "05 - Medellín", "Pistola Glock G-43", null);
        EventoOperativo bogota = new EventoOperativo(AHORA.minusSeconds(20), "b", "Luis", TipoEvento.ARMA_APARTADA,
                "Arma apartada", "16 - Bogotá", "Pistola CZ P10", null);
        EventoOperativo cali = new EventoOperativo(AHORA.minusSeconds(30), "b", "Luis", TipoEvento.OPORTUNIDAD,
                "Arma disponible", "76 - Cali", "Revólver", null);
        List<EventoOperativo> todos = List.of(medellin, bogota, cali);

        assertEquals(List.of(medellin, bogota), new FiltroBitacora(null, null, null, "medellin o bogota").aplicar(todos, reloj));
        assertEquals(List.of(medellin, bogota), new FiltroBitacora(null, null, null, "Medellín, Bogotá").aplicar(todos, reloj));
        assertEquals(List.of(medellin, bogota), new FiltroBitacora(null, null, null, "medellin | bogota").aplicar(todos, reloj));
        assertEquals(List.of(bogota), new FiltroBitacora(null, null, null, "cz o glock luis").aplicar(todos, reloj));
        assertEquals(List.of(), new FiltroBitacora(null, null, null, "pereira o").aplicar(todos, reloj));
    }
}
