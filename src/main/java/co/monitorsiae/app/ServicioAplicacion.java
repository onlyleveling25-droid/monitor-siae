package co.monitorsiae.app;

import co.monitorsiae.acceso.ServicioAcceso;
import co.monitorsiae.dominio.AccesoAplicacion;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.ValidadorConfiguracion;
import co.monitorsiae.persistencia.RepositorioConfiguracion;

import java.util.List;
import java.util.Objects;

/**
 * Casos de uso de configuracion y acceso que la interfaz necesita.
 */
public final class ServicioAplicacion {

    private final RepositorioConfiguracion repositorio;
    private final ServicioAcceso acceso;
    private final ValidadorConfiguracion validador;
    private Configuracion configuracion;

    public ServicioAplicacion(RepositorioConfiguracion repositorio, ServicioAcceso acceso,
                              ValidadorConfiguracion validador) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.acceso = Objects.requireNonNull(acceso);
        this.validador = Objects.requireNonNull(validador);
        this.configuracion = repositorio.cargar();
    }

    public synchronized Configuracion configuracion() {
        return configuracion;
    }

    public synchronized List<String> problemasConfiguracion() {
        return validador.validar(configuracion).stream().map(Object::toString).toList();
    }

    public synchronized boolean debeCrearContrasena() {
        return acceso.debeCrearContrasena(configuracion.acceso());
    }

    public synchronized boolean pideContrasena() {
        return acceso.pideContrasena(configuracion.acceso());
    }

    public synchronized boolean verificarContrasena(char[] intento) {
        return acceso.verificar(configuracion.acceso(), intento);
    }

    public synchronized void crearContrasena(char[] nueva, char[] confirmacion) {
        AccesoAplicacion nuevoAcceso = acceso.crear(nueva, confirmacion);
        guardarConfiguracion(configuracion.conAcceso(nuevoAcceso));
    }

    public synchronized void cambiarContrasena(char[] actual, char[] nueva, char[] confirmacion) {
        AccesoAplicacion nuevoAcceso = acceso.cambiar(configuracion.acceso(), actual, nueva, confirmacion);
        guardarConfiguracion(configuracion.conAcceso(nuevoAcceso));
    }

    public synchronized void desactivarProteccion(char[] actual) {
        AccesoAplicacion nuevoAcceso = acceso.desactivar(configuracion.acceso(), actual);
        guardarConfiguracion(configuracion.conAcceso(nuevoAcceso));
    }

    public synchronized void guardar(Configuracion nueva) {
        validar(nueva);
        guardarConfiguracion(nueva);
    }

    public synchronized void validar(Configuracion nueva) {
        List<String> problemas = validador.validar(nueva).stream().map(Object::toString).toList();
        if (!problemas.isEmpty()) {
            throw new IllegalArgumentException(String.join(System.lineSeparator(), problemas));
        }
    }

    private void guardarConfiguracion(Configuracion nueva) {
        repositorio.guardar(nueva);
        configuracion = nueva;
    }
}
