package co.monitorsiae.portal;

/**
 * La ventana del navegador asociada a la sesión del portal se cerró.
 */
public final class SesionPortalCerradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SesionPortalCerradaException(String mensaje) {
        super(mensaje);
    }
}
