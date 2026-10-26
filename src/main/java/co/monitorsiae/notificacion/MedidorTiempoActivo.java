package co.monitorsiae.notificacion;

import java.time.Duration;

/**
 * Cuanto tiempo lleva activa, sin reconocerse, la alerta de un cliente. Es el dato que necesitan
 * los canales para decidir si un nivel de {@link co.monitorsiae.dominio.PerfilEscalamiento} entra
 * en vigor.
 */
@FunctionalInterface
public interface MedidorTiempoActivo {

    Duration tiempoActivo(String clienteId);

    /** Siempre reporta cero: equivale a no tener escalamiento, para no romper el comportamiento previo. */
    MedidorTiempoActivo SIN_TIEMPO = clienteId -> Duration.ZERO;
}
