package co.monitorsiae.dominio;

/**
 * Que hace el monitor cuando encuentra un arma que cumple las reglas del cliente.
 */
public enum ModoReaccion {

    /** Selecciona el arma, pulsa la modalidad y Confirmar, y luego alerta. */
    APARTAR,

    /** Solo alerta; la persona selecciona el arma manualmente. */
    ALERTA
}
