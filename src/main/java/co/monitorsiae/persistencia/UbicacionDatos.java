package co.monitorsiae.persistencia;

import java.nio.file.Path;

/**
 * Carpeta donde la aplicacion guarda sus datos locales: configuracion, perfiles de navegador,
 * capturas y registros.
 */
public final class UbicacionDatos {

    private static final String NOMBRE_CARPETA = "MonitorSIAE";

    private final Path raiz;

    public UbicacionDatos(Path raiz) {
        this.raiz = raiz;
    }

    /** %APPDATA%\MonitorSIAE en Windows; carpeta oculta en el directorio personal en otros sistemas. */
    public static UbicacionDatos delUsuario() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return new UbicacionDatos(Path.of(appData, NOMBRE_CARPETA));
        }
        return new UbicacionDatos(Path.of(System.getProperty("user.home"), "." + NOMBRE_CARPETA.toLowerCase()));
    }

    public Path raiz() {
        return raiz;
    }

    public Path archivoConfiguracion() {
        return raiz.resolve("config.json");
    }

    public Path perfilesNavegador() {
        return raiz.resolve("perfiles");
    }

    public Path capturas() {
        return raiz.resolve("capturas");
    }

    public Path registros() {
        return raiz.resolve("logs");
    }
}
