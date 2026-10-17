package co.monitorsiae.acceso;

/**
 * Convierte una contrasena en un valor que se puede guardar sin revelarla, y comprueba intentos
 * contra ese valor.
 */
public interface CifradorContrasenas {

    String derivar(char[] contrasena);

    boolean coincide(char[] intento, String derivada);
}
