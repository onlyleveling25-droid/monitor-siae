package co.monitorsiae.notificacion;

/**
 * Tipo de alerta. Cambia el color de la ventana y el sonido, no la insistencia: ambas suenan hasta
 * que la persona las reconozca.
 */
public enum Gravedad {

    /** Hay un arma apartada o disponible: actuar ya. */
    OPORTUNIDAD,

    /** El monitor se detuvo y necesita a la persona (sesion vencida, errores repetidos). */
    PROBLEMA
}
