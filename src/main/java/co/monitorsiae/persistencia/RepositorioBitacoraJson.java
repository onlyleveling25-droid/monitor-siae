package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.EventoOperativo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bitácora en un archivo JSON local. La retención es doble para que el archivo no crezca sin
 * límite: se descartan los eventos de más de {@link #RETENCION} y se conservan como máximo
 * {@link #MAXIMO_EVENTOS}.
 */
public final class RepositorioBitacoraJson implements RepositorioBitacora {

    public static final int MAXIMO_EVENTOS = 1000;
    public static final Duration RETENCION = Duration.ofDays(30);

    private static final TypeReference<List<EventoOperativo>> TIPO_EVENTOS = new TypeReference<>() {
    };

    private final Path archivo;
    private final Clock reloj;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public RepositorioBitacoraJson(Path archivo) {
        this(archivo, Clock.systemUTC());
    }

    public RepositorioBitacoraJson(Path archivo, Clock reloj) {
        this.archivo = archivo;
        this.reloj = reloj;
    }

    @Override
    public synchronized void registrar(EventoOperativo evento) {
        List<EventoOperativo> conservados = new ArrayList<>(vigentes(cargar()));
        conservados.addFirst(evento);
        escribir(conservados.stream().limit(MAXIMO_EVENTOS).toList());
    }

    @Override
    public synchronized List<EventoOperativo> eventos() {
        return vigentes(cargar());
    }

    private List<EventoOperativo> vigentes(List<EventoOperativo> eventos) {
        Instant limite = reloj.instant().minus(RETENCION);
        return eventos.stream().filter(e -> !e.momento().isBefore(limite)).toList();
    }

    private List<EventoOperativo> cargar() {
        if (Files.notExists(archivo)) {
            return List.of();
        }
        try {
            List<EventoOperativo> eventos = json.readValue(archivo.toFile(), TIPO_EVENTOS);
            if (eventos == null) {
                throw new IllegalStateException("La bitácora está vacía o dañada: " + archivo);
            }
            return eventos;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la bitácora en " + archivo, e);
        }
    }

    private void escribir(List<EventoOperativo> eventos) {
        Path temporal = null;
        try {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            temporal = Files.createTempFile(archivo.toAbsolutePath().getParent(), "bitacora-", ".tmp");
            json.writeValue(temporal.toFile(), eventos);
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
            temporal = null;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la bitácora en " + archivo, e);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException e) {
                    throw new UncheckedIOException("No se pudo eliminar el archivo temporal " + temporal, e);
                }
            }
        }
    }
}
