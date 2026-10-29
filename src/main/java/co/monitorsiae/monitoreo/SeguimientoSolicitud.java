package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ModoInicio;

/**
 * Recuerda si el cliente ya tiene una solicitud abierta en el portal y por tanto el monitor debe
 * buscar inventario desde "Estado de tramite" en lugar de repetir el paso 1, y cual solicitud es
 * cuando la persona la indico por su numero.
 *
 * <p>Se usa desde el hilo del monitor del cliente y se conserva entre detenciones y reinicios dentro
 * de la misma sesion de la aplicacion.
 */
public final class SeguimientoSolicitud {

    private volatile boolean enSolicitud;
    private volatile String numeroSolicitud = "";

    public SeguimientoSolicitud() {
        this(ModoInicio.NUEVA_SOLICITUD);
    }

    public SeguimientoSolicitud(ModoInicio inicio) {
        establecer(inicio, "");
    }

    public boolean siguiendoSolicitud() {
        return enSolicitud;
    }

    /** Numero de la solicitud a vigilar, tal como aparece en el portal; vacio = la primera que coincida. */
    public String numeroSolicitud() {
        return numeroSolicitud;
    }

    /**
     * La solicitud quedo abierta en el portal al guardar el paso 1: de aqui en adelante se busca desde
     * "Estado de tramite". Su numero no se conoce, asi que se descarta cualquiera indicado antes.
     */
    public void seguirSolicitud() {
        enSolicitud = true;
        numeroSolicitud = "";
    }

    /** No hay solicitud abierta (o ya se uso): se vuelve a hacer desde el paso 1. */
    public void volverAlPaso1() {
        enSolicitud = false;
        numeroSolicitud = "";
    }

    public void establecer(ModoInicio modo, String numero) {
        enSolicitud = modo == ModoInicio.SOLICITUD_EXISTENTE;
        numeroSolicitud = enSolicitud && numero != null ? numero.strip() : "";
    }
}
