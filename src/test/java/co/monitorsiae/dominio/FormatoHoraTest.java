package co.monitorsiae.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatoHoraTest {

    @Test
    void muestraEnDoceHoras() {
        assertEquals("12:00 a. m.", FormatoHora.hora(LocalTime.MIDNIGHT));
        assertEquals("08:05 a. m.", FormatoHora.hora(LocalTime.of(8, 5)));
        assertEquals("12:00 p. m.", FormatoHora.hora(LocalTime.NOON));
        assertEquals("03:30 p. m.", FormatoHora.hora(LocalTime.of(15, 30)));
        assertEquals("11:59 p. m.", FormatoHora.hora(LocalTime.of(23, 59)));
        assertEquals("04:33:07 p. m.", FormatoHora.horaConSegundos(LocalTime.of(16, 33, 7)));
    }

    @Test
    void leeVeinticuatroHoras() {
        assertEquals(LocalTime.of(16, 0), FormatoHora.leer("16:00"));
        assertEquals(LocalTime.of(8, 0), FormatoHora.leer("8:00"));
        assertEquals(LocalTime.MIDNIGHT, FormatoHora.leer("00:00"));
    }

    @Test
    void leeDoceHorasEscritasDeVariasFormas() {
        assertEquals(LocalTime.of(16, 0), FormatoHora.leer("04:00 p. m."));
        assertEquals(LocalTime.of(16, 0), FormatoHora.leer("4:00 pm"));
        assertEquals(LocalTime.of(16, 0), FormatoHora.leer("4:00PM"));
        assertEquals(LocalTime.of(16, 0), FormatoHora.leer("4 p.m."));
        assertEquals(LocalTime.of(8, 30), FormatoHora.leer(" 8:30 a. m. "));
        assertEquals(LocalTime.MIDNIGHT, FormatoHora.leer("12:00 a. m."));
        assertEquals(LocalTime.NOON, FormatoHora.leer("12:00 p. m."));
    }

    @Test
    void loQueMuestraSeVuelveALeerIgual() {
        for (int hora = 0; hora < 24; hora++) {
            LocalTime momento = LocalTime.of(hora, 15);
            assertEquals(momento, FormatoHora.leer(FormatoHora.hora(momento)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "4", "24:00", "13:00 pm", "0:30 am", "10:75", "16:00:00"})
    void rechazaLoQueNoEsUnaHora(String texto) {
        assertThrows(IllegalArgumentException.class, () -> FormatoHora.leer(texto));
    }

    @Test
    void rechazaNulo() {
        assertThrows(IllegalArgumentException.class, () -> FormatoHora.leer(null));
    }
}
