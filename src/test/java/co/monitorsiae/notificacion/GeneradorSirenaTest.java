package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.TonoAlarma;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorSirenaTest {

    @Test
    void generaUnSegundoDeAudioDe16Bits() {
        byte[] audio = GeneradorSirena.ciclo(Gravedad.OPORTUNIDAD);

        assertEquals((int) GeneradorSirena.FRECUENCIA_MUESTREO * 2, audio.length);
    }

    @Test
    void noSatura() {
        byte[] audio = GeneradorSirena.ciclo(Gravedad.PROBLEMA);
        int maximo = 0;
        for (int i = 0; i < audio.length; i += 2) {
            short muestra = (short) ((audio[i] & 0xFF) | (audio[i + 1] << 8));
            maximo = Math.max(maximo, Math.abs(muestra));
        }

        assertTrue(maximo > Short.MAX_VALUE / 2, "La alarma debe sonar fuerte");
        assertTrue(maximo < Short.MAX_VALUE, "La alarma no debe saturar");
    }

    @Test
    void generaUnaFormaDeOndaDistintaParaCadaTono() {
        byte[] sirena = GeneradorSirena.ciclo(Gravedad.OPORTUNIDAD, TonoAlarma.SIRENA);
        byte[] doble = GeneradorSirena.ciclo(Gravedad.OPORTUNIDAD, TonoAlarma.DOBLE);

        assertFalse(java.util.Arrays.equals(sirena, doble));
    }

    @Test
    void previsualizacionRepiteTresCiclosCompletos() {
        byte[] audio = GeneradorSirena.previsualizacion(TonoAlarma.SIRENA, 3);

        assertEquals((int) GeneradorSirena.FRECUENCIA_MUESTREO * 2 * 3, audio.length);
    }
}
