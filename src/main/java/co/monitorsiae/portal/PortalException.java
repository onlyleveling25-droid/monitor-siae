package co.monitorsiae.portal;

/**
 * La plataforma no respondio como se esperaba.
 */
public class PortalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PortalException(String mensaje) {
        super(mensaje);
    }

    public PortalException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
