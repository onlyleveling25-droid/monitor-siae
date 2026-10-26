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
    RESPUESTA_INESPERADA("Respuesta inesperada");

    private final String etiqueta;

    TipoEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
