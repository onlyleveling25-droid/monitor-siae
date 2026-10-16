package co.monitorsiae.dominio;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Ventana de tiempo en la que el monitor consulta la plataforma.
 *
 * @param restringir si es falso, el monitor consulta a cualquier hora
 * @param dias       dias permitidos
 * @param desde      hora inicial, inclusive
 * @param hasta      hora final, exclusiva
 * @param zona       zona horaria en la que se interpretan las horas
 */
public record HorarioOperacion(
        boolean restringir,
        List<DayOfWeek> dias,
        LocalTime desde,
        LocalTime hasta,
        ZoneId zona) {

    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    public HorarioOperacion {
        dias = dias == null ? List.of() : dias.stream().filter(Objects::nonNull).distinct().sorted().toList();
        desde = Objects.requireNonNullElse(desde, LocalTime.of(8, 0));
        hasta = Objects.requireNonNullElse(hasta, LocalTime.of(16, 0));
        zona = Objects.requireNonNullElse(zona, ZONA_COLOMBIA);
    }

    /**
     * Desactivado por defecto: hay evidencia de inventario fuera del horario que anuncia la
     * plataforma (ver docs/DECISIONES.md, D11).
     */
    public static HorarioOperacion porDefecto() {
        return new HorarioOperacion(false,
                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                LocalTime.of(8, 0), LocalTime.of(16, 0), ZONA_COLOMBIA);
    }
}
