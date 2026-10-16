package co.monitorsiae.persistencia;

import java.nio.file.Path;

/**
 * La configuracion existe pero no se puede usar: esta danada o fue creada por una version mas
 * nueva de la aplicacion. La interfaz debe ofrecer restaurar el respaldo en lugar de cerrarse.
 */
public class ConfiguracionIlegibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Path archivo;

    public ConfiguracionIlegibleException(Path archivo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.archivo = archivo;
    }

    public ConfiguracionIlegibleException(Path archivo, String mensaje) {
        this(archivo, mensaje, null);
    }

    public Path archivo() {
        return archivo;
    }
}
