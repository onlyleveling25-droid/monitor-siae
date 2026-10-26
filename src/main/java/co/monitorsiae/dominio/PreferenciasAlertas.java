package co.monitorsiae.dominio;

import java.util.Objects;

/**
 * @param sonidoHabilitado    valor general de sonido; cada cliente puede heredarlo o sobrescribirlo
 * @param vozHabilitada       valor general de voz, aplicado a todos los clientes
 * @param tono                patrón de sonido usado mientras no entre en vigor un nivel de escalamiento
 * @param perfilEscalamiento  escalamiento opcional de insistencia mientras una alerta siga sin
 *                            reconocerse; deshabilitado por defecto, sin cambiar el comportamiento previo
 */
public record PreferenciasAlertas(boolean sonidoHabilitado, boolean vozHabilitada, TonoAlarma tono,
                                  PerfilEscalamiento perfilEscalamiento) {

    public PreferenciasAlertas {
        tono = Objects.requireNonNullElse(tono, TonoAlarma.SIRENA);
        perfilEscalamiento = Objects.requireNonNullElseGet(perfilEscalamiento, PerfilEscalamiento::deshabilitado);
    }

    /** Compatibilidad con código y configuraciones previas al escalamiento de alarmas. */
    public PreferenciasAlertas(boolean sonidoHabilitado, boolean vozHabilitada, TonoAlarma tono) {
        this(sonidoHabilitado, vozHabilitada, tono, PerfilEscalamiento.deshabilitado());
    }

    public static PreferenciasAlertas porDefecto() {
        return new PreferenciasAlertas(true, true, TonoAlarma.SIRENA, PerfilEscalamiento.deshabilitado());
    }
}

