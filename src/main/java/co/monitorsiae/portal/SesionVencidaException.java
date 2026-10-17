package co.monitorsiae.portal;

/**
 * La plataforma pidio iniciar sesion de nuevo. Solo una persona puede resolverlo (captcha).
 */
public class SesionVencidaException extends PortalException {

    private static final long serialVersionUID = 1L;

    public SesionVencidaException(String mensaje) {
        super(mensaje);
    }
}
