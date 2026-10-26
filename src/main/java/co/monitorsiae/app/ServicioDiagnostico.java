package co.monitorsiae.app;

import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.ProblemaConfiguracion;
import co.monitorsiae.dominio.ValidadorConfiguracion;
import co.monitorsiae.monitoreo.VentanaHoraria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;

import javax.sound.sampled.AudioSystem;

/**
 * Revisa de forma independiente configuración, navegador, almacenamiento, audio/voz y horario.
 *
 * <p>Solo lee: no abre navegadores ni sesiones del portal, no escribe archivos ni cambia la
 * configuración. Cada comprobación se aísla para que la falla de una no oculte las demás.
 */
public final class ServicioDiagnostico {

    static final long ESPACIO_MINIMO_BYTES = 100L * 1024 * 1024;

    private static final Logger REGISTRO = LoggerFactory.getLogger(ServicioDiagnostico.class);

    private final Supplier<Configuracion> configuracion;
    private final ValidadorConfiguracion validador;
    private final Path carpetaDatos;
    private final Clock reloj;
    private final Predicate<NavegadorPreferido> navegadorInstalado;
    private final BooleanSupplier audioDisponible;
    private final Supplier<Optional<String>> vozEnEspanol;

    public ServicioDiagnostico(Supplier<Configuracion> configuracion, ValidadorConfiguracion validador,
                               Path carpetaDatos, Clock reloj, Predicate<NavegadorPreferido> navegadorInstalado,
                               BooleanSupplier audioDisponible, Supplier<Optional<String>> vozEnEspanol) {
        this.configuracion = configuracion;
        this.validador = validador;
        this.carpetaDatos = carpetaDatos;
        this.reloj = reloj;
        this.navegadorInstalado = navegadorInstalado;
        this.audioDisponible = audioDisponible;
        this.vozEnEspanol = vozEnEspanol;
    }

    /** Detección real en este equipo de navegador instalado y salida de audio. */
    public static Predicate<NavegadorPreferido> navegadorEnEquipo() {
        return navegador -> rutasDe(navegador).stream().anyMatch(Files::isRegularFile);
    }

    public static boolean audioEnEquipo() {
        return AudioSystem.getMixerInfo().length > 0;
    }

    /** Puede tardar unos segundos (consulta las voces del sistema): llamarlo fuera del hilo de la interfaz. */
    public Diagnostico ejecutar() {
        List<Comprobacion> resultado = new ArrayList<>();
        resultado.add(aislar("Configuración", this::comprobarConfiguracion));
        resultado.add(aislar("Navegador", this::comprobarNavegador));
        resultado.add(aislar("Almacenamiento", this::comprobarAlmacenamiento));
        resultado.add(aislar("Audio y voz", this::comprobarAudioYVoz));
        resultado.add(aislar("Horario", this::comprobarHorario));
        return new Diagnostico(resultado);
    }

    private Comprobacion aislar(String nombre, Function<Configuracion, Comprobacion> comprobacion) {
        try {
            return comprobacion.apply(configuracion.get());
        } catch (RuntimeException e) {
            REGISTRO.warn("Falló la comprobación '{}'", nombre, e);
            return Comprobacion.problema(nombre, "No se pudo completar la comprobación.",
                    "Vuelve a ejecutar el diagnóstico; si persiste, revisa el registro de la aplicación.");
        }
    }

    private Comprobacion comprobarConfiguracion(Configuracion actual) {
        List<ProblemaConfiguracion> problemas = validador.validar(actual);
        if (!problemas.isEmpty()) {
            String detalle = problemas.stream().limit(3).map(Object::toString)
                    .reduce((a, b) -> a + " · " + b).orElse("");
            String extra = problemas.size() > 3 ? " (y " + (problemas.size() - 3) + " más)" : "";
            return Comprobacion.problema("Configuración", detalle + extra,
                    "Abre Ajustes y corrige los campos señalados.");
        }
        if (actual.clientes().isEmpty()) {
            return Comprobacion.aviso("Configuración", "No hay clientes configurados.",
                    "Agrega el primer cliente en Ajustes para poder monitorear.");
        }
        return Comprobacion.correcta("Configuración",
                actual.clientes().size() + " cliente(s) con datos válidos.");
    }

    private Comprobacion comprobarNavegador(Configuracion actual) {
        NavegadorPreferido navegador = actual.navegador();
        if (navegadorInstalado.test(navegador)) {
            return Comprobacion.correcta("Navegador", navegador + " está instalado.");
        }
        String remedio = "Instala " + navegador + " o elige el otro navegador en Ajustes.";
        return actual.entornoPortal() == EntornoPortal.PORTAL_REAL
                ? Comprobacion.problema("Navegador", navegador + " no se encontró en este equipo.", remedio)
                : Comprobacion.aviso("Navegador", navegador + " no se encontró; el simulador no lo necesita.",
                        remedio + " Hará falta para usar el portal real.");
    }

    private Comprobacion comprobarAlmacenamiento(Configuracion actual) {
        Path existente = carpetaDatos;
        while (existente != null && Files.notExists(existente)) {
            existente = existente.getParent();
        }
        if (existente == null || !Files.isWritable(existente)) {
            return Comprobacion.problema("Almacenamiento", "No se puede escribir en " + carpetaDatos + ".",
                    "Revisa los permisos de la carpeta o libera el acceso desde el antivirus.");
        }
        try {
            long libre = Files.getFileStore(existente).getUsableSpace();
            if (libre < ESPACIO_MINIMO_BYTES) {
                return Comprobacion.aviso("Almacenamiento",
                        "Quedan " + libre / (1024 * 1024) + " MB libres en el disco de datos.",
                        "Libera espacio para que la configuración y la bitácora se puedan guardar.");
            }
        } catch (IOException e) {
            return Comprobacion.aviso("Almacenamiento", "No se pudo medir el espacio libre.",
                    "Comprueba manualmente que el disco tenga espacio.");
        }
        return Comprobacion.correcta("Almacenamiento", "Carpeta de datos escribible: " + carpetaDatos);
    }

    private Comprobacion comprobarAudioYVoz(Configuracion actual) {
        boolean sonido = actual.preferenciasAlertas().sonidoHabilitado();
        boolean voz = actual.preferenciasAlertas().vozHabilitada();
        if (!sonido && !voz) {
            return Comprobacion.aviso("Audio y voz", "El sonido y la voz están desactivados.",
                    "Solo verás las alertas en pantalla. Actívalos en Ajustes > Alarmas si los necesitas.");
        }
        if (sonido && !audioDisponible.getAsBoolean()) {
            return Comprobacion.problema("Audio y voz", "No se detecta ninguna salida de audio.",
                    "Conecta o activa unos altavoces y revisa el volumen del sistema.");
        }
        if (voz && vozEnEspanol.get().isEmpty()) {
            return Comprobacion.aviso("Audio y voz", "No hay una voz en español instalada.",
                    "Instala una voz en español en la configuración de voz de Windows, o desactiva la voz.");
        }
        return Comprobacion.correcta("Audio y voz", "Salida de audio disponible y canales configurados.");
    }

    private Comprobacion comprobarHorario(Configuracion actual) {
        HorarioOperacion horario = actual.horario();
        if (!horario.restringir()) {
            return Comprobacion.correcta("Horario", "Sin restricción: el monitor consulta a cualquier hora.");
        }
        if (horario.dias().isEmpty() || !horario.desde().isBefore(horario.hasta())) {
            return Comprobacion.problema("Horario", "El horario restringido no deja ninguna franja válida.",
                    "Elige al menos un día y una hora de inicio anterior a la de fin en Ajustes > Horario.");
        }
        if (new VentanaHoraria(horario, reloj).permiteAhora()) {
            return Comprobacion.correcta("Horario", "Dentro del horario configurado.");
        }
        return Comprobacion.aviso("Horario", "Ahora estás fuera del horario configurado.",
                "El monitor esperará a la próxima franja; amplía el horario si necesitas consultar ya.");
    }

    private static List<Path> rutasDe(NavegadorPreferido navegador) {
        String relativa = switch (navegador) {
            case CHROME -> "Google\\Chrome\\Application\\chrome.exe";
            case EDGE -> "Microsoft\\Edge\\Application\\msedge.exe";
        };
        List<Path> rutas = new ArrayList<>();
        for (String variable : List.of("PROGRAMFILES", "PROGRAMFILES(X86)", "LOCALAPPDATA")) {
            String base = System.getenv(variable);
            if (base != null && !base.isBlank()) {
                rutas.add(Path.of(base, relativa.split("\\\\")));
            }
        }
        return rutas;
    }
}
