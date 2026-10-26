package co.monitorsiae.dominio;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Rango de horas de un día en el que se consulta la plataforma.
 *
 * @param desde hora inicial, inclusive
 * @param hasta hora final, exclusiva
 */
public record FranjaHoraria(LocalTime desde, LocalTime hasta) {

    public FranjaHoraria {
        desde = Objects.requireNonNullElse(desde, LocalTime.of(8, 0));
        hasta = Objects.requireNonNullElse(hasta, LocalTime.of(16, 0));
    }

    /** Una franja es válida si empieza antes de terminar; no cruza la medianoche. */
    public boolean esValida() {
        return desde.isBefore(hasta);
    }

    public boolean contiene(LocalTime hora) {
        return !hora.isBefore(desde) && hora.isBefore(hasta);
    }
}
