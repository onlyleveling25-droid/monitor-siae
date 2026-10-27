package co.monitorsiae.portal;

/**
 * El servidor del portal no contesto a una accion del programa: la peticion fallo o devolvio un
 * error 5xx (por ejemplo un 504 del gateway). El fallo es de la plataforma, no de una pantalla
 * desconocida, asi que el monitor lo espera sin contarlo como un error inesperado.
 */
public class PortalNoRespondeException extends PortalException {

    private static final long serialVersionUID = 1L;

    public PortalNoRespondeException(String mensaje) {
        super(mensaje);
    }
}
