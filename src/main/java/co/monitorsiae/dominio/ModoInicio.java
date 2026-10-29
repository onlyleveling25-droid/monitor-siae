package co.monitorsiae.dominio;

/**
 * Desde donde empieza a trabajar un monitoreo.
 *
 * <p>Cuando el paso 1 ya se guardo y el portal tenia inventario pero ninguna arma de interes, la
 * solicitud queda abierta en el modulo "Estado de tramite": repetir el paso 1 no tiene sentido, hay
 * que buscar inventario desde esa solicitud.
 */
public enum ModoInicio {

    /** Hace la solicitud en el paso 1 (si hay una abierta, el monitor la sigue por su cuenta). */
    NUEVA_SOLICITUD,

    /** Va a "Estado de tramite" y vigila la solicitud ya abierta; si no hay, cae al paso 1. */
    SOLICITUD_EXISTENTE
}
