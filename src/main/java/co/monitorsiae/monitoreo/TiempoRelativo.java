package co.monitorsiae.monitoreo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** "hace 5 min" para lo reciente y la fecha para lo antiguo. */
public final class TiempoRelativo {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM HH:mm", ES);
    private static final DateTimeFormatter FECHA_COMPLETA =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy HH:mm:ss", ES);

    private TiempoRelativo() {
    }

    public static String texto(Instant momento, Clock reloj, ZoneId zona) {
        Duration pasado = Duration.between(momento, reloj.instant());
        if (pasado.isNegative() || pasado.toSeconds() < 60) {
            return "hace unos segundos";
        }
        if (pasado.toMinutes() < 60) {
            return "hace " + pasado.toMinutes() + " min";
        }
        if (pasado.toHours() < 24) {
            return "hace " + pasado.toHours() + " h";
        }
        return FECHA_CORTA.format(momento.atZone(zona));
    }

    public static String completo(Instant momento, ZoneId zona) {
        return FECHA_COMPLETA.format(momento.atZone(zona));
    }
}
