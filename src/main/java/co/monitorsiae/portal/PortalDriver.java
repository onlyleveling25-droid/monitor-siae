package co.monitorsiae.portal;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.ReglasSeleccion;

import java.util.List;

/**
 * Las manos del monitor sobre la plataforma: ejecuta acciones en la pagina y reporta lo que ve,
 * sin tomar decisiones. Las decisiones (que seccional, que arma) las toma el monitor.
 *
 * <p>Una instancia corresponde a la sesion de un cliente. Las implementaciones con navegador no
 * son seguras entre hilos: todos los metodos deben llamarse desde el hilo que creo la instancia.
 *
 * <p>Cualquier metodo puede lanzar {@link SesionVencidaException} o
 * {@link PortalInesperadoException}.
 */
public interface PortalDriver extends AutoCloseable {

    /**
     * Recarga el paso 1, elige como obtener el permiso y el tipo de tramite, y devuelve las
     * seccionales que ofrece la lista en ese momento (solo aparecen las abiertas).
     *
     * @return seccionales tal como las muestra la plataforma; vacia si todas estan cerradas
     * @throws OpcionNoDisponibleException si alguna de las opciones configuradas no aparece
     */
    List<String> abrirFormulario(ParametrosTramite tramite);

    /**
     * Elige la seccional, escribe la descripcion y pulsa GUARDAR.
     *
     * @param seccional texto exacto de una de las opciones devueltas por {@link #abrirFormulario}
     */
    ResultadoGuardar enviarPaso1(String seccional, String descripcion);

    /** Lee el listado de armas del paso 2. */
    List<ArmaInventario> leerInventario();

    /**
     * Selecciona el arma, pulsa la modalidad y Confirmar.
     *
     * @return verdadero si la plataforma confirmo la asignacion
     */
    boolean apartar(ArmaInventario arma, Modalidad modalidad);

    /** Indica si la sesión del portal y su página siguen disponibles. */
    default boolean estaAbierto() {
        return true;
    }

    /**
     * Registra una acción que se ejecuta cuando la sesión del portal se cierra. La acción puede
     * ejecutarse desde un hilo distinto al propietario del driver y no debe invocar métodos del
     * driver.
     */
    default void alCerrar(Runnable accion) {
    }

    /** Lleva al frente la ventana asociada a esta sesión; debe invocarse desde su hilo propietario. */
    default void enfocarVentana() {
        throw new UnsupportedOperationException("Este navegador no permite enfocar la ventana desde la aplicación.");
    }

    @Override
    default void close() {
    }

    /**
     * Aparta el arma: selecciona la fila, abre el modal de la modalidad y confirma.
     * Deja el arma pre-asignada, pero sin sellar la reserva.
     *
     * @return verdadero si la plataforma acepto la pre-asignacion
     */
    default boolean apartar(ArmaInventario arma, ReglasSeleccion reglas) {
        return apartar(arma, reglas.modalidad());
    }

    /**
     * Pulsa FINALIZAR y confirma el cierre general, lo que sella la reserva y avanza al paso 3.
     *
     * @return verdadero si la plataforma confirmo el cierre
     */
    default boolean confirmarCierre() {
        return true;
    }
}
