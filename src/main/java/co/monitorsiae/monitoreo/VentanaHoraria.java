package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.HorarioOperacion;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;


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
        String hoy = ahora.getDayOfWeek().getDisplayName(TextStyle.FULL, es);
        String rangoHoy = horario.franja(ahora.getDayOfWeek())
                .map(f -> f.desde() + " a " + f.hasta())
                .orElse("sin consulta");
        return "Fuera del horario configurado (hoy " + hoy + ": " + rangoHoy + "). "
                + proximaApertura(ahora).map(p -> "Retomará la consulta " + describir(p, ahora, es) + ".")
                        .orElse("Ningún día tiene horario de consulta.");
    }

    /** Próximo momento en que se vuelve a consultar, buscando hasta una semana adelante. */
    public Optional<ZonedDateTime> proximaApertura() {
        return proximaApertura(ZonedDateTime.now(reloj).withZoneSameInstant(horario.zona()));
    }

    private Optional<ZonedDateTime> proximaApertura(ZonedDateTime ahora) {
        for (int dias = 0; dias <= 7; dias++) {
            ZonedDateTime dia = ahora.plusDays(dias);
            Optional<ZonedDateTime> inicio = horario.franja(dia.getDayOfWeek())
                    .map(f -> dia.with(f.desde()))
                    .filter(candidato -> candidato.isAfter(ahora));
            if (inicio.isPresent()) {
                return inicio;
            }
        }
        return Optional.empty();
    }

    private static String describir(ZonedDateTime apertura, ZonedDateTime ahora, Locale es) {
        String hora = apertura.toLocalTime().toString();
        if (apertura.toLocalDate().equals(ahora.toLocalDate())) {
            return "hoy a las " + hora;
        }
        if (apertura.toLocalDate().equals(ahora.toLocalDate().plusDays(1))) {
            return "mañana a las " + hora;
        }
        return "el " + apertura.getDayOfWeek().getDisplayName(TextStyle.FULL, es) + " a las " + hora;
    }
}
