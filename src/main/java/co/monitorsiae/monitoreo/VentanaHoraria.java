package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.HorarioOperacion;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * Responde si en este momento se permite consultar la plataforma.
 */
public final class VentanaHoraria {

    private final HorarioOperacion horario;
    private final Clock reloj;

    public VentanaHoraria(HorarioOperacion horario, Clock reloj) {
        this.horario = horario;
        this.reloj = reloj;
    }

    public boolean permiteAhora() {
        if (!horario.restringir()) {
            return true;
        }
        ZonedDateTime ahora = ZonedDateTime.now(reloj).withZoneSameInstant(horario.zona());
        LocalTime hora = ahora.toLocalTime();
        return horario.dias().contains(ahora.getDayOfWeek())
                && !hora.isBefore(horario.desde())
                && hora.isBefore(horario.hasta());
    }

    public String motivoFueraDeHorario() {
        ZonedDateTime ahora = ZonedDateTime.now(reloj).withZoneSameInstant(horario.zona());
        String hora = ahora.format(DateTimeFormatter.ofPattern("EEEE HH:mm",
                Locale.forLanguageTag("es")));
        String dias = horario.dias().stream()
                .map(dia -> dia.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es")))
                .collect(Collectors.joining(", "));
        return "Fuera del horario configurado (" + horario.desde() + " a " + horario.hasta()
                + ", " + horario.zona().getId() + "). Ahora son las " + hora
                + "; días permitidos: " + dias + ". Reintento en 30 segundos.";
    }
}
