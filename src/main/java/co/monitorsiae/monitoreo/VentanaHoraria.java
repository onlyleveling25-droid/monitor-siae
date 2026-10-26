package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.HorarioOperacion;

import java.time.Clock;
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
        return horario.franja(ahora.getDayOfWeek())
                .map(franja -> franja.contiene(ahora.toLocalTime()))
                .orElse(false);
    }

    public String motivoFueraDeHorario() {
        ZonedDateTime ahora = ZonedDateTime.now(reloj).withZoneSameInstant(horario.zona());
        Locale es = Locale.forLanguageTag("es");
        String hora = ahora.format(DateTimeFormatter.ofPattern("EEEE HH:mm", es));
        String franjas = horario.franjas().entrySet().stream()
                .map(e -> e.getKey().getDisplayName(TextStyle.SHORT, es)
                        + " " + e.getValue().desde() + " a " + e.getValue().hasta())
                .collect(Collectors.joining(", "));
        return "Fuera del horario configurado (" + horario.zona().getId() + "). Ahora son las " + hora
                + "; horario por día: " + franjas + ". Reintento en 30 segundos.";
    }
}
