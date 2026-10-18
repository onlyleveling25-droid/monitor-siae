package co.monitorsiae.persistencia;

import co.monitorsiae.dominio.Configuracion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Guarda la configuracion en un archivo JSON legible.
 *
 * <p>Nunca escribe directamente sobre el archivo: escribe un temporal en la misma carpeta y luego
 * lo mueve encima. Asi, un corte de luz a mitad de guardar deja el archivo anterior intacto. Antes
 * de reemplazar, conserva la version previa como respaldo ({@code config.json.bak}).
 */
public final class RepositorioConfiguracionJson implements RepositorioConfiguracion {

    private final Path archivo;
    private final Path respaldo;
    private final ObjectMapper json;

    public RepositorioConfiguracionJson(Path archivo) {
        this.archivo = archivo;
        this.respaldo = archivo.resolveSibling(archivo.getFileName() + ".bak");
        this.json = crearMapeador();
    }

    private static ObjectMapper crearMapeador() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Permite abrir archivos de versiones futuras compatibles sin fallar por un campo nuevo.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Configuracion cargar() {
        if (Files.notExists(archivo)) {
            return Configuracion.porDefecto();
        }
        return leer(archivo);
    }

    @Override
    public void guardar(Configuracion configuracion) {
        Path temporal = null;
        try {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            temporal = Files.createTempFile(archivo.toAbsolutePath().getParent(), "config-", ".tmp");
            json.writeValue(temporal.toFile(), configuracion);

            if (Files.exists(archivo)) {
                Files.copy(archivo, respaldo, StandardCopyOption.REPLACE_EXISTING);
            }
            reemplazar(temporal, archivo);
            temporal = null;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la configuración en " + archivo, e);
        } finally {
            borrarSiExiste(temporal);
        }
    }

    @Override
    public boolean existeRespaldo() {
        return Files.exists(respaldo);
    }

    @Override
    public Configuracion restaurarRespaldo() {
        if (!existeRespaldo()) {
            throw new ConfiguracionIlegibleException(respaldo, "No existe una copia de respaldo.");
        }
        Configuracion configuracion = leer(respaldo);
        try {
            Files.copy(respaldo, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo restaurar el respaldo en " + archivo, e);
        }
        return configuracion;
    }

    private Configuracion leer(Path origen) {
        Configuracion configuracion;
        try {
            configuracion = json.readValue(origen.toFile(), Configuracion.class);
        } catch (JsonProcessingException e) {
            throw new ConfiguracionIlegibleException(origen,
                    "El archivo de configuración está dañado: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new ConfiguracionIlegibleException(origen, "No se pudo leer el archivo de configuración.", e);
        }

        if (configuracion == null) {
            throw new ConfiguracionIlegibleException(origen, "El archivo de configuración está vacío.");
        }
        if (configuracion.version() > Configuracion.VERSION_ACTUAL) {
            throw new ConfiguracionIlegibleException(origen,
                    "La configuración fue creada por una versión más nueva de la aplicación (formato "
                            + configuracion.version() + "). Actualice la aplicación.");
        }
        return configuracion.version() < Configuracion.VERSION_ACTUAL
                ? configuracion.conVersionActual()
                : configuracion;
    }

    private static void reemplazar(Path origen, Path destino) throws IOException {
        try {
            Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void borrarSiExiste(Path ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException ignorada) {
            // Un temporal huerfano no afecta la configuracion; no vale la pena interrumpir por eso.
        }
    }
}
