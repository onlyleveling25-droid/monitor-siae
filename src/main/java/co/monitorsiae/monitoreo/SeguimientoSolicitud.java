package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ModoInicio;

/**
 * Recuerda si el cliente ya tiene una solicitud abierta en el portal y por tanto el monitor debe
 * buscar inventario desde "Estado de tramite" en lugar de repetir el paso 1.
 *
 * <p>Se usa desde el hilo del monitor del cliente y se conserva entre detenciones y reinicios dentro
 * de la misma sesion de la aplicacion.
 */
public final class SeguimientoSolicitud {

    private volatile boolean enSolicitud;

    public SeguimientoSolicitud() {
        this(ModoInicio.NUEVA_SOLICITUD);
    }

    public SeguimientoSolicitud(ModoInicio inicio) {
        establecer(inicio);
    }

    public boolean siguiendoSolicitud() {
        return enSolicitud;
    }

    /** La solicitud quedo abierta en el portal: de aqui en adelante se busca desde "Estado de tramite". */
    public void seguirSolicitud() {
        enSolicitud = true;
    }

    /** No hay solicitud abierta (o ya se uso): se vuelve a hacer desde el paso 1. */
    public void volverAlPaso1() {
        enSolicitud = false;
    }

    public void establecer(ModoInicio modo) {
        enSolicitud = modo == ModoInicio.SOLICITUD_EXISTENTE;
    }
}
