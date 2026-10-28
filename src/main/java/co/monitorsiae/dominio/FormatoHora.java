package co.monitorsiae.dominio;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Horas para mostrar en pantalla en formato de 12 horas ("04:30 p. m.") y lectura de lo que escribe
 * el usuario en 12 o 24 horas.
 *
 * <p>El sufijo se arma a mano en vez de usar el de Java: según la versión, Java pone espacios
 * especiales (no separables) dentro de "p. m." y el texto no se vería ni se compararía igual.
 * Lo que se guarda o exporta (configuración, CSV, nombres de archivo) sigue en 24 horas.
 */
public final class FormatoHora {

    // Acepta "16:00", "4:00 pm", "04:00 p. m.", "4 pm", "4:00p.m.". Los espacios y puntos se quitan antes.
    private static final Pattern ENTRADA = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(am|pm)?");

    private FormatoHora() {
    }

    /** "04:30 p. m." */
    public static String hora(TemporalAccessor momento) {
        return formatear(momento, false);
    }

    /** "04:30:15 p. m." */
    public static String horaConSegundos(TemporalAccessor momento) {
        return formatear(momento, true);
    }

    private static String formatear(TemporalAccessor momento, boolean segundos) {
        int hora24 = momento.get(ChronoField.HOUR_OF_DAY);
        int hora12 = hora24 % 12 == 0 ? 12 : hora24 % 12;
        StringBuilder texto = new StringBuilder()
                .append(String.format(Locale.ROOT, "%02d:%02d", hora12, momento.get(ChronoField.MINUTE_OF_HOUR)));
        if (segundos) {
            texto.append(String.format(Locale.ROOT, ":%02d", momento.get(ChronoField.SECOND_OF_MINUTE)));
        }
        return texto.append(hora24 < 12 ? " a. m." : " p. m.").toString();
    }

    /**
     * Lee una hora escrita por el usuario en 12 horas ("4:00 pm", "04:00 p. m.") o 24 horas ("16:00").
     *
     * @throws IllegalArgumentException si el texto no es una hora válida.
     */
    public static LocalTime leer(String texto) {
        String limpio = texto == null ? "" : texto.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u00A0\\u202F.]", "");
        Matcher partes = ENTRADA.matcher(limpio);
        if (!partes.matches()) {
            throw new IllegalArgumentException("Hora no válida: " + texto);
        }
        int hora = Integer.parseInt(partes.group(1));
        int minutos = partes.group(2) == null ? -1 : Integer.parseInt(partes.group(2));
        String sufijo = partes.group(3);
        if (sufijo == null) {
            // Sin a. m./p. m. se exigen los minutos: "4" a secas es ambiguo.
            if (minutos < 0 || hora > 23) {
                throw new IllegalArgumentException("Hora no válida: " + texto);
            }
        } else {
            if (hora < 1 || hora > 12) {
                throw new IllegalArgumentException("Hora no válida: " + texto);
            }
            hora = hora % 12 + (sufijo.equals("pm") ? 12 : 0);
        }
        if (minutos > 59) {
            throw new IllegalArgumentException("Hora no válida: " + texto);
        }
        return LocalTime.of(hora, Math.max(minutos, 0));
    }
}
