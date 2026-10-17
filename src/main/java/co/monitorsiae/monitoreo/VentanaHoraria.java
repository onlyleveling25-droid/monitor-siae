package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.HorarioOperacion;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;

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
}
