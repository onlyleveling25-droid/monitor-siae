package co.monitorsiae.dominio;

import java.time.Duration;
import java.util.Objects;

/**
 * Un escalón del perfil de escalamiento de alarmas: a partir de cuánto tiempo sin reconocer la
 * alerta, qué tan insistente se vuelve el aviso.
 *
 * @param espera       tiempo transcurrido desde que la alerta quedó activa para que este nivel
 *                     entre en vigor; nunca negativo
 * @param forzarSonido si es verdadero, el sonido suena aunque el cliente lo tenga silenciado
 * @param forzarVoz    si es verdadero, la voz habla aunque esté deshabilitada en general
 * @param tono         patrón de sonido que se usa a partir de este nivel
 */
public record NivelEscalamiento(Duration espera, boolean forzarSonido, boolean forzarVoz, TonoAlarma tono) {

    public NivelEscalamiento {
        espera = espera == null || espera.isNegative() ? Duration.ZERO : espera;
        tono = Objects.requireNonNullElse(tono, TonoAlarma.SIRENA);
    }
}
