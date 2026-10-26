package co.monitorsiae.app;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.NavegadorPreferido;

import java.util.List;
import java.util.Objects;

/**
 * Reglas del asistente que se muestra la primera vez que se abre la aplicación (sin clientes
 * configurados todavía): elegir navegador, crear el primer cliente y elegir el entorno del portal.
 *
 * <p>Es una clase de dominio puro, sin JavaFX, para poder probar sus reglas sin arrancar una
 * interfaz gráfica; la pantalla ({@code AsistentePrimeraEjecucionDialogo}) solo la recorre y
 * presenta sus pasos.
 */
public final class AsistentePrimeraEjecucion {

    /** Pasos del asistente, en el orden en que deben mostrarse. */
    public enum Paso {
        BIENVENIDA,
        NAVEGADOR,
        PRIMER_CLIENTE,
        ENTORNO_PORTAL,
        RESUMEN
    }

    public static final List<Paso> PASOS = List.of(Paso.values());

    private AsistentePrimeraEjecucion() {
    }

    /**
     * El asistente solo tiene sentido cuando todavía no hay ningún cliente configurado: es la
     * señal de que la aplicación se está usando por primera vez (ver {@link Configuracion#porDefecto()}).
     */
    public static boolean esNecesario(Configuracion configuracion) {
        return configuracion == null || configuracion.clientes().isEmpty();
    }

    /** Mensaje de error si el nombre no sirve para crear el primer cliente, o {@code null} si es válido. */
    public static String validarNombreCliente(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Escribe el nombre del primer cliente para continuar.";
        }
        return null;
    }

    /**
     * Aplica las respuestas del asistente sobre la configuración base: fija el navegador, el
     * entorno del portal y agrega el primer cliente. No toca nada más (horario, alarmas, etc.).
     */
    public static Configuracion aplicar(Configuracion base, NavegadorPreferido navegador,
                                         String nombreCliente, EntornoPortal entorno) {
        Objects.requireNonNull(base, "base");
        if (validarNombreCliente(nombreCliente) != null) {
            throw new IllegalArgumentException(validarNombreCliente(nombreCliente));
        }
        Configuracion conNavegador = navegador == null ? base : conNavegador(base, navegador);
        Configuracion conEntorno = entorno == null ? conNavegador : conNavegador.conEntornoPortal(entorno);
        return conEntorno.conClientes(List.of(Cliente.nuevo(nombreCliente.strip())));
    }

    private static Configuracion conNavegador(Configuracion base, NavegadorPreferido navegador) {
        return new Configuracion(base.version(), base.acceso(), navegador, base.entornoPortal(),
                base.intervaloConsultaSegundos(), base.horario(), base.clientes(), base.preferenciasAlertas());
    }
}
