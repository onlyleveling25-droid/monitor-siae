package co.monitorsiae.dominio;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Ventana de tiempo en la que el monitor consulta la plataforma.
 *
 * @param restringir si es falso, el monitor consulta a cualquier hora
 * @param franjas    rango de horas de cada día permitido; un día sin franja no se consulta
 * @param zona       zona horaria en la que se interpretan las horas
 */
public record HorarioOperacion(
        boolean restringir,
        Map<DayOfWeek, FranjaHoraria> franjas,
        ZoneId zona) {

    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    public HorarioOperacion {
        EnumMap<DayOfWeek, FranjaHoraria> ordenadas = new EnumMap<>(DayOfWeek.class);
        if (franjas != null) {
            franjas.forEach((dia, franja) -> {
                if (dia != null && franja != null) {
                    ordenadas.put(dia, franja);
                }
            });
        }
        franjas = java.util.Collections.unmodifiableMap(ordenadas);
        zona = Objects.requireNonNullElse(zona, ZONA_COLOMBIA);
    }

    /** Mismo rango de horas para varios días: el formato anterior a los horarios por día. */
    public HorarioOperacion(boolean restringir, List<DayOfWeek> dias, LocalTime desde, LocalTime hasta,
                            ZoneId zona) {
        this(restringir, mismaFranja(dias, new FranjaHoraria(desde, hasta)), zona);
    }

    private static Map<DayOfWeek, FranjaHoraria> mismaFranja(List<DayOfWeek> dias, FranjaHoraria franja) {
        Map<DayOfWeek, FranjaHoraria> resultado = new EnumMap<>(DayOfWeek.class);
        if (dias != null) {
            dias.stream().filter(Objects::nonNull).forEach(dia -> resultado.put(dia, franja));
        }
        return resultado;
    }

    /** Días con consulta permitida, de lunes a domingo. */
    public List<DayOfWeek> dias() {
        return List.copyOf(franjas.keySet());
    }

    public Optional<FranjaHoraria> franja(DayOfWeek dia) {
        return Optional.ofNullable(franjas.get(dia));
    }

    /**
     * Desactivado por defecto: hay evidencia de inventario fuera del horario que anuncia la
     * plataforma (ver docs/DECISIONES.md, D11). Si se activa, propone lunes a viernes de 8 a 16.
     */
    public static HorarioOperacion porDefecto() {
        return new HorarioOperacion(false,
                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                LocalTime.of(8, 0), LocalTime.of(16, 0), ZONA_COLOMBIA);
    }
}
