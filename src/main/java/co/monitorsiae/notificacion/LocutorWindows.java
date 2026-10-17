package co.monitorsiae.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Usa el sintetizador de voz de Windows (System.Speech) mediante PowerShell, sin librerias extra.
 * Prefiere una voz en espanol si esta instalada.
 *
 * <p>El texto viaja en una variable de entorno y no dentro del comando: incluye nombres de clientes
 * escritos por el usuario, y asi ninguna comilla o simbolo puede alterar el comando.
 */
public final class LocutorWindows implements Locutor {

    static final String VARIABLE_TEXTO = "MONITOR_SIAE_VOZ";

    private static final Logger REGISTRO = LoggerFactory.getLogger(LocutorWindows.class);
    private static final long SEGUNDOS_MAXIMOS = 30;

    private static final String GUION = String.join("; ",
            "Add-Type -AssemblyName System.Speech",
            "$sintetizador = New-Object System.Speech.Synthesis.SpeechSynthesizer",
            "$voz = $sintetizador.GetInstalledVoices() | Where-Object { $_.Enabled -and "
                    + "$_.VoiceInfo.Culture.Name -like 'es*' } | Select-Object -First 1",
            "if ($voz) { $sintetizador.SelectVoice($voz.VoiceInfo.Name) }",
            "$sintetizador.Volume = 100",
            "$sintetizador.Speak($env:" + VARIABLE_TEXTO + ")");

    private boolean fallaRegistrada;

    @Override
    public void decir(String texto) throws InterruptedException {
        Process proceso;
        try {
            proceso = construir(texto).start();
        } catch (IOException e) {
            registrarFallaUnaVez(e);
            return;
        }
        if (!proceso.waitFor(SEGUNDOS_MAXIMOS, TimeUnit.SECONDS)) {
            proceso.destroyForcibly();
        }
    }

    ProcessBuilder construir(String texto) {
        ProcessBuilder constructor = new ProcessBuilder(List.of(
                "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", GUION));
        constructor.environment().put(VARIABLE_TEXTO, texto);
        constructor.redirectErrorStream(true);
        constructor.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return constructor;
    }

    private void registrarFallaUnaVez(IOException e) {
        // Se repite cada pocos segundos mientras haya alerta; basta con un aviso en el registro.
        if (!fallaRegistrada) {
            fallaRegistrada = true;
            REGISTRO.warn("No se pudo usar la voz de Windows: {}", e.getMessage());
        }
    }
}
