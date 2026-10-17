package co.monitorsiae.acceso;

import co.monitorsiae.dominio.AccesoAplicacion;

import java.util.Arrays;

/**
 * Reglas de la contrasena de la aplicacion. No guarda nada: recibe el estado actual y devuelve el
 * nuevo, para que quien lo llame decida cuando persistirlo.
 *
 * <p>Los metodos que reciben contrasenas las borran de memoria al terminar.
 */
public final class ServicioAcceso {

    public static final int LONGITUD_MINIMA = 6;

    private final CifradorContrasenas cifrador;

    public ServicioAcceso(CifradorContrasenas cifrador) {
        this.cifrador = cifrador;
    }

    /** Primer uso con proteccion activa: hay que crear la contrasena antes de seguir. */
    public boolean debeCrearContrasena(AccesoAplicacion acceso) {
        return acceso.requerirContrasena() && !acceso.tieneContrasena();
    }

    /** Hay que pedir la contrasena antes de editar la configuracion. */
    public boolean pideContrasena(AccesoAplicacion acceso) {
        return acceso.requerirContrasena() && acceso.tieneContrasena();
    }

    public boolean verificar(AccesoAplicacion acceso, char[] intento) {
        try {
            return acceso.tieneContrasena() && cifrador.coincide(intento, acceso.hash());
        } finally {
            borrar(intento);
        }
    }

    /** Crea la contrasena y activa la proteccion. Sirve para el primer uso y para reactivarla. */
    public AccesoAplicacion crear(char[] nueva, char[] confirmacion) {
        try {
            validarNueva(nueva, confirmacion);
            return new AccesoAplicacion(true, cifrador.derivar(nueva));
        } finally {
            borrar(nueva);
            borrar(confirmacion);
        }
    }

    public AccesoAplicacion cambiar(AccesoAplicacion acceso, char[] actual, char[] nueva, char[] confirmacion) {
        try {
            exigirActual(acceso, actual);
            validarNueva(nueva, confirmacion);
            return new AccesoAplicacion(true, cifrador.derivar(nueva));
        } finally {
            borrar(actual);
            borrar(nueva);
            borrar(confirmacion);
        }
    }

    /** Quita la proteccion. Exige la contrasena actual para que nadie la desactive sin conocerla. */
    public AccesoAplicacion desactivar(AccesoAplicacion acceso, char[] actual) {
        try {
            if (acceso.tieneContrasena()) {
                exigirActual(acceso, actual);
            }
            return new AccesoAplicacion(false, "");
        } finally {
            borrar(actual);
        }
    }

    private void exigirActual(AccesoAplicacion acceso, char[] actual) {
        if (!acceso.tieneContrasena() || !cifrador.coincide(actual, acceso.hash())) {
            throw new ContrasenaRechazadaException("La contraseña actual no es correcta.");
        }
    }

    private static void validarNueva(char[] nueva, char[] confirmacion) {
        if (nueva == null || nueva.length < LONGITUD_MINIMA) {
            throw new ContrasenaRechazadaException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        if (!Arrays.equals(nueva, confirmacion)) {
            throw new ContrasenaRechazadaException("Las contraseñas no coinciden.");
        }
    }

    private static void borrar(char[] contrasena) {
        if (contrasena != null) {
            Arrays.fill(contrasena, '\0');
        }
    }
}
