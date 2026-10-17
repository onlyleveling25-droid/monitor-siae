package co.monitorsiae.notificacion;

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
        double agudo = gravedad == Gravedad.OPORTUNIDAD ? 880 : 520;
        double grave = gravedad == Gravedad.OPORTUNIDAD ? 660 : 390;
        int muestrasPorTono = (int) (FRECUENCIA_MUESTREO / 2);

        byte[] audio = new byte[muestrasPorTono * 2 * 2];
        escribirTono(audio, 0, muestrasPorTono, agudo);
        escribirTono(audio, muestrasPorTono, muestrasPorTono, grave);
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
