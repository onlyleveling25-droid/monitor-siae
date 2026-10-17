package co.monitorsiae.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Habla con las voces de Windows mediante el guion {@code hablar.ps1}, sin librerias extra.
 *
 * <p>El guion vive como recurso del programa para que se pueda probar a mano exactamente igual
 * que lo ejecuta la aplicacion. Se copia a un archivo temporal porque PowerShell necesita una ruta.
 *
 * <p>El texto viaja en una variable de entorno y no en la linea de comandos: incluye nombres de
 * clientes escritos por el usuario, y asi ninguna comilla o simbolo puede alterar el comando.
 */
public final class LocutorWindows implements Locutor {

    static final String VARIABLE_TEXTO = "MONITOR_SIAE_VOZ";
    static final String RECURSO_GUION = "hablar.ps1";

    private static final Logger REGISTRO = LoggerFactory.getLogger(LocutorWindows.class);
    private static final long SEGUNDOS_MAXIMOS_HABLAR = 30;
    private static final long SEGUNDOS_MAXIMOS_VERIFICAR = 20;
    private static final String SIN_VOZ = "NINGUNA";

    private final Path guion;
    private boolean fallaRegistrada;

    public LocutorWindows() {
        this.guion = extraerGuion();
    }

    @Override
    public void decir(String texto) throws InterruptedException {
        try {
            Process proceso = construir(texto).start();
            if (!proceso.waitFor(SEGUNDOS_MAXIMOS_HABLAR, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
            }
        } catch (IOException e) {
            registrarFallaUnaVez(e);
        }
    }

    /**
     * Nombre de la voz en espanol que se usara, o vacio si el equipo no tiene ninguna. Tarda unos
     * segundos: llamarlo fuera del hilo de la interfaz.
     */
    public Optional<String> vozEnEspanol() throws InterruptedException {
        try {
            Process proceso = comando(List.of("-SoloVerificar")).redirectErrorStream(true).start();
            // La salida es una sola linea: cabe en el bufer del proceso, asi que se puede esperar primero.
            if (!proceso.waitFor(SEGUNDOS_MAXIMOS_VERIFICAR, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                return Optional.empty();
            }
            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            return salida.isEmpty() || salida.equals(SIN_VOZ) ? Optional.empty() : Optional.of(salida);
        } catch (IOException e) {
            REGISTRO.warn("No se pudo consultar las voces de Windows: {}", e.getMessage());
            return Optional.empty();
        }
    }

    ProcessBuilder construir(String texto) {
        ProcessBuilder constructor = comando(List.of());
        constructor.environment().put(VARIABLE_TEXTO, texto);
        constructor.redirectErrorStream(true);
        constructor.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return constructor;
    }

    private ProcessBuilder comando(List<String> argumentos) {
        List<String> partes = new ArrayList<>(List.of(
                "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", guion.toString()));
        partes.addAll(argumentos);
        return new ProcessBuilder(partes);
    }

    private static Path extraerGuion() {
        try (InputStream origen = LocutorWindows.class.getResourceAsStream(RECURSO_GUION)) {
            if (origen == null) {
                throw new IllegalStateException("Falta el recurso " + RECURSO_GUION);
            }
            Path destino = Files.createTempFile("monitor-siae-voz-", ".ps1");
            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            destino.toFile().deleteOnExit();
            return destino;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar el guion de voz", e);
        }
    }

    private void registrarFallaUnaVez(IOException e) {
        // Se repite cada pocos segundos mientras haya alerta; basta con un aviso en el registro.
        if (!fallaRegistrada) {
            fallaRegistrada = true;
            REGISTRO.warn("No se pudo usar la voz de Windows: {}", e.getMessage());
        }
    }
}
