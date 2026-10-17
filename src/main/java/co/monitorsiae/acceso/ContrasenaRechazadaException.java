package co.monitorsiae.acceso;

/**
 * Una operacion sobre la contrasena no se pudo hacer. El mensaje esta pensado para mostrarse al
 * usuario tal cual.
 */
public class ContrasenaRechazadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ContrasenaRechazadaException(String mensaje) {
        super(mensaje);
    }
}
