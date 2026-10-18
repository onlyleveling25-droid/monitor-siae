package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.TonoAlarma;

import java.util.Arrays;

/**
 * Genera el sonido de la alarma en memoria, sin archivos de audio: dos tonos que se alternan.
 * Formato: PCM de 16 bits con signo, mono, little-endian.
 */
public final class GeneradorSirena {

    public static final float FRECUENCIA_MUESTREO = 44_100f;

    private static final double AMPLITUD = 0.8;
    private static final double SEGUNDOS_TRANSICION = 0.01;

    private GeneradorSirena() {
    }

    /** Un ciclo de un segundo: tono agudo y tono grave, medio segundo cada uno. */
    public static byte[] ciclo(Gravedad gravedad) {
        return ciclo(gravedad, TonoAlarma.SIRENA);
    }

    public static byte[] ciclo(Gravedad gravedad, TonoAlarma tono) {
        boolean oportunidad = gravedad == Gravedad.OPORTUNIDAD;
        double agudo = tono.frecuenciaAguda(oportunidad);
        double grave = tono.frecuenciaGrave(oportunidad);
        int muestrasPorTono = (int) (FRECUENCIA_MUESTREO / 2);

        byte[] audio = new byte[muestrasPorTono * 2 * 2];
        escribirTono(audio, 0, muestrasPorTono, agudo);
        escribirTono(audio, muestrasPorTono, muestrasPorTono, grave);
        return audio;
    }

    static byte[] previsualizacion(TonoAlarma tono, int ciclos) {
        if (ciclos < 1) {
            throw new IllegalArgumentException("La previsualización debe incluir al menos un ciclo.");
        }
        byte[] ciclo = ciclo(Gravedad.OPORTUNIDAD, tono);
        byte[] audio = Arrays.copyOf(ciclo, ciclo.length * ciclos);
        for (int i = 1; i < ciclos; i++) {
            System.arraycopy(ciclo, 0, audio, i * ciclo.length, ciclo.length);
        }
        return audio;
    }

    private static void escribirTono(byte[] audio, int desde, int muestras, double frecuencia) {
        int transicion = (int) (FRECUENCIA_MUESTREO * SEGUNDOS_TRANSICION);
        for (int i = 0; i < muestras; i++) {
            // Entrada y salida suaves para evitar chasquidos al cambiar de tono.
            double envolvente = Math.min(1.0, Math.min(i, muestras - 1 - i) / (double) transicion);
            double valor = Math.sin(2 * Math.PI * frecuencia * i / FRECUENCIA_MUESTREO) * AMPLITUD * envolvente;
            short muestra = (short) Math.round(valor * Short.MAX_VALUE);
            int posicion = (desde + i) * 2;
            audio[posicion] = (byte) muestra;
            audio[posicion + 1] = (byte) (muestra >> 8);
        }
    }
}
