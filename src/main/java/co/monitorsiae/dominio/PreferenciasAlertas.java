package co.monitorsiae.dominio;

import java.util.Objects;

public record PreferenciasAlertas(boolean sonidoHabilitado, boolean vozHabilitada, TonoAlarma tono) {

    public PreferenciasAlertas {
        tono = Objects.requireNonNullElse(tono, TonoAlarma.SIRENA);
    }

    public static PreferenciasAlertas porDefecto() {
        return new PreferenciasAlertas(true, true, TonoAlarma.SIRENA);
    }
}
