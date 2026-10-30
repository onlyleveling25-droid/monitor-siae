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
    /** La solicitud la acaba de crear el paso 1 y todavia no se ha visto en "Estado de tramite". */
    private volatile boolean recienCreada;
    /** Seccional del paso 1 que creo la solicitud; vacia si la persona pidio vigilar una existente. */
    private volatile String seccionalOrigen = "";
    /** Armas de inventarios sin interés que ya se mostraron a la persona en esta solicitud. */
    private final java.util.Set<String> armasVistas = java.util.concurrent.ConcurrentHashMap.newKeySet();

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

    /** Verdadero si la solicitud la creo el paso 1 en este monitoreo y aun no aparecio en el portal. */
    public boolean recienCreada() {
        return recienCreada;
    }

    public String seccionalOrigen() {
        return seccionalOrigen;
    }

    /** Como {@link #seguirSolicitud(String)} sin saber en que seccional se creo. */
    public void seguirSolicitud() {
        seguirSolicitud("");
    }

    /**
     * La solicitud quedo abierta en el portal al guardar el paso 1: de aqui en adelante se busca desde
     * "Estado de tramite". Si ya se estaba siguiendo una, no cambia nada: asi no se pierde su numero.
     */
    public void seguirSolicitud(String seccional) {
        if (enSolicitud) {
            return;
        }
        enSolicitud = true;
        numeroSolicitud = "";
        recienCreada = true;
        seccionalOrigen = seccional == null ? "" : seccional;
    }

    /** La solicitud aparecio en "Estado de tramite": ya no es una recien creada pendiente de confirmar. */
    public void solicitudEncontrada() {
        recienCreada = false;
    }

    /**
     * Fija la solicitud que se encontró sin número (la primera que coincidía), para que los ciclos
     * siguientes vigilen siempre esa y no otra del mismo tipo que aparezca después.
     *
     * @return verdadero si el número quedó fijado ahora
     */
    public boolean fijarNumeroSiFalta(String numero) {
        if (!enSolicitud || !numeroSolicitud.isBlank() || numero == null || numero.isBlank()) {
            return false;
        }
        numeroSolicitud = numero.strip();
        return true;
    }

    /**
     * Anota las armas de un inventario sin interés que ya se le mostraron a la persona.
     *
     * @return verdadero si hay alguna que no se había visto: solo entonces vale la pena detenerse y avisar
     */
    public boolean anotarInventarioSinInteres(java.util.List<co.monitorsiae.dominio.ArmaInventario> inventario) {
        boolean hayNuevas = false;
        for (co.monitorsiae.dominio.ArmaInventario arma : inventario) {
            hayNuevas |= armasVistas.add(arma.descripcion());
        }
        return hayNuevas;
    }

    /** El inventario se vació: lo que llegue después es un inventario nuevo, aunque repita modelos. */
    public void olvidarInventarioVisto() {
        armasVistas.clear();
    }

    /** No hay solicitud abierta (o ya se uso): se vuelve a hacer desde el paso 1. */
    public void volverAlPaso1() {
        enSolicitud = false;
        numeroSolicitud = "";
        recienCreada = false;
        seccionalOrigen = "";
        armasVistas.clear();
    }

    public void establecer(ModoInicio modo, String numero) {
        String anterior = numeroSolicitud;
        enSolicitud = modo == ModoInicio.SOLICITUD_EXISTENTE;
        numeroSolicitud = enSolicitud && numero != null ? numero.strip() : "";
        recienCreada = false;
        seccionalOrigen = "";
        // Reanudar la misma solicitud conserva lo ya visto, para no detenerse otra vez por el mismo inventario.
        if (!enSolicitud || (!numeroSolicitud.isEmpty() && !numeroSolicitud.equals(anterior))) {
            armasVistas.clear();
        }
    }
}
