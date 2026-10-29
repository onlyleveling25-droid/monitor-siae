package co.monitorsiae.portal;

/** Lo que encuentra el monitor al buscar inventario desde una solicitud abierta en "Estado de tramite". */
public enum ResultadoSolicitud {

    /** No hay ninguna solicitud en progreso que coincida con el tramite del cliente. */
    SIN_SOLICITUD,

    /** La solicitud existe pero el portal no tiene inventario por ahora. */
    SIN_INVENTARIO,

    /** La solicitud existe y el listado de inventario esta abierto, listo para leerse. */
    CON_INVENTARIO
}
