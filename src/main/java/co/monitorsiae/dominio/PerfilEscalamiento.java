package co.monitorsiae.dominio;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Perfil opcional de escalamiento de alarmas: mientras una alerta siga activa sin reconocerse, va
 * subiendo de insistencia según los niveles configurados, en el orden de su espera.
 *
 * <p>Es explícito y controlable: por defecto está deshabilitado y el comportamiento de alarmas no
 * cambia respecto al existente (ver {@link PreferenciasAlertas}).
 *
 * @param habilitado si es falso, los niveles no se aplican y las preferencias generales mandan
 * @param niveles    escalones en el orden en que deben evaluarse (se ordenan por espera al construir)
 */
public record PerfilEscalamiento(boolean habilitado, List<NivelEscalamiento> niveles) {

    public PerfilEscalamiento {
        niveles = niveles == null ? List.of()
                : niveles.stream().filter(Objects::nonNull)
                        .sorted((a, b) -> a.espera().compareTo(b.espera())).toList();
    }

    public static PerfilEscalamiento deshabilitado() {
        return new PerfilEscalamiento(false, List.of());
    }

    /**
     * Nivel vigente para una alerta activa desde hace {@code transcurrido}, o vacío si el perfil
     * está deshabilitado o ningún nivel aplica todavía.
     */
    public java.util.Optional<NivelEscalamiento> nivelVigente(Duration transcurrido) {
        if (!habilitado || transcurrido == null) {
            return java.util.Optional.empty();
        }
        NivelEscalamiento vigente = null;
        for (NivelEscalamiento nivel : niveles) {
            if (!transcurrido.minus(nivel.espera()).isNegative()) {
                vigente = nivel;
            } else {
                break;
            }
        }
        return java.util.Optional.ofNullable(vigente);
    }
}
