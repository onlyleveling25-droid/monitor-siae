package co.monitorsiae.monitoreo;

/**
 * Clase de evento operativo que se guarda en la bitácora. Los ciclos rutinarios sin novedad no
 * tienen tipo: no se registran.
 */
public enum TipoEvento {
    MONITOREO_INICIADO("Monitoreo iniciado"),
    MONITOREO_DETENIDO("Monitoreo detenido"),
    OPORTUNIDAD("Oportunidad"),
    ARMA_APARTADA("Arma apartada"),
    APARTADO_FALLIDO("Apartado fallido"),
    SESION_VENCIDA("Sesión vencida"),
    RESPUESTA_INESPERADA("Respuesta inesperada"),
    PORTAL_SIN_RESPUESTA("Portal sin respuesta");

    private final String etiqueta;

    TipoEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Hallazgos que la persona no debería perderse entre los demás eventos. */
    public boolean importante() {
        return this == OPORTUNIDAD || this == ARMA_APARTADA;
    }

    /** Eventos que indican que algo falló o necesita atención. */
    public boolean problema() {
        return this == APARTADO_FALLIDO || this == SESION_VENCIDA || this == RESPUESTA_INESPERADA
                || this == PORTAL_SIN_RESPUESTA;
    }

    /** Eventos cuyo inventario puede haberse guardado en el historial del cliente. */
    public boolean conInventario() {
        return this == OPORTUNIDAD || this == ARMA_APARTADA || this == APARTADO_FALLIDO;
    }
}
