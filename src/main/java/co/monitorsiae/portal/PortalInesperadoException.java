package co.monitorsiae.portal;

import java.nio.file.Path;
import java.util.Optional;

/**
 * La plataforma mostro algo que el programa no reconoce: un error del servidor, un mensaje nuevo o
 * una pagina distinta. Puede llevar la ruta de una captura de pantalla para diagnosticarlo.
 */
public class PortalInesperadoException extends PortalException {

    private static final long serialVersionUID = 1L;

    private final transient Path captura;

    public PortalInesperadoException(String mensaje, Path captura, Throwable causa) {
        super(mensaje, causa);
        this.captura = captura;
    }

    public PortalInesperadoException(String mensaje) {
        this(mensaje, null, null);
    }

    public Optional<Path> captura() {
        return Optional.ofNullable(captura);
    }
}
