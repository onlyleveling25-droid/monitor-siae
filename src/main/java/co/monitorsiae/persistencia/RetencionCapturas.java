package co.monitorsiae.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Borra las capturas de pantalla de fallos que superan el periodo de retención, para que la carpeta no
 * crezca sin límite.
 *
 * <p>Solo toca archivos con el nombre exacto que genera el portal ({@code fallo-AAAAMMDD-HHMMSS.png}) dentro
 * de la carpeta indicada: no entra en subcarpetas, no sigue enlaces y no borra nada más. La antigüedad es la
 * fecha de modificación del archivo.
 */
public final class RetencionCapturas {

    private static final Logger REGISTRO = LoggerFactory.getLogger(RetencionCapturas.class);
    private static final Pattern NOMBRE = Pattern.compile("fallo-\\d{8}-\\d{6}\\.png");

    private final Path carpeta;
    private final Duration retencion;
    private final Clock reloj;

    public RetencionCapturas(Path carpeta, Duration retencion, Clock reloj) {
        this.carpeta = carpeta;
        this.retencion = retencion;
        this.reloj = reloj;
    }

    /** Borra lo vencido y devuelve cuántas capturas eliminó; un fallo con un archivo no detiene a los demás. */
    public int limpiar() {
        if (Files.notExists(carpeta, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }
        Instant limite = reloj.instant().minus(retencion);
        int borradas = 0;
        try (Stream<Path> archivos = Files.list(carpeta)) {
            for (Path archivo : (Iterable<Path>) archivos::iterator) {
                if (esVencida(archivo, limite) && borrar(archivo)) {
                    borradas++;
                }
            }
        } catch (IOException | RuntimeException e) {
            REGISTRO.warn("No se pudo revisar la carpeta de capturas {}", carpeta, e);
        }
        if (borradas > 0) {
            REGISTRO.info("Se borraron {} captura(s) con más de {} días", borradas, retencion.toDays());
        }
        return borradas;
    }

    private static boolean esVencida(Path archivo, Instant limite) {
        try {
            return NOMBRE.matcher(archivo.getFileName().toString()).matches()
                    && Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)
                    && Files.getLastModifiedTime(archivo, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(limite);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean borrar(Path archivo) {
        try {
            Files.delete(archivo);
            return true;
        } catch (IOException e) {
            REGISTRO.warn("No se pudo borrar la captura {}", archivo, e);
            return false;
        }
    }
}
