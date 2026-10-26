package co.monitorsiae.monitoreo;

/** Duraciones legibles: "45 s", "3 min", "5 min 12 s", "2 h 3 min". */
public final class TextoDuracion {

    private TextoDuracion() {
    }

    public static String texto(long segundosTotales) {
        long total = Math.max(0, segundosTotales);
        long horas = total / 3600;
        long minutos = (total % 3600) / 60;
        long segundos = total % 60;
        if (horas > 0) {
            return horas + " h " + minutos + " min";
        }
        if (minutos > 0) {
            return segundos == 0 ? minutos + " min" : minutos + " min " + segundos + " s";
        }
        return segundos + " s";
    }

    /** "180 s (3 min)": los segundos y su equivalencia cuando pasa de un minuto. */
    public static String conEquivalencia(long segundosTotales) {
        long total = Math.max(0, segundosTotales);
        return total < 60 ? total + " s" : total + " s (" + texto(total) + ")";
    }
}
