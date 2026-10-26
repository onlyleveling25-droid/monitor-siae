package co.monitorsiae.app;

/**
 * Resultado de una comprobación del diagnóstico.
 *
 * @param nombre  qué se revisó
 * @param nivel   gravedad de lo encontrado
 * @param detalle qué se encontró
 * @param remedio qué hacer; vacío si no hay nada que hacer
 */
public record Comprobacion(String nombre, Nivel nivel, String detalle, String remedio) {

    public enum Nivel {
        CORRECTO, AVISO, PROBLEMA
    }

    static Comprobacion correcta(String nombre, String detalle) {
        return new Comprobacion(nombre, Nivel.CORRECTO, detalle, "");
    }

    static Comprobacion aviso(String nombre, String detalle, String remedio) {
        return new Comprobacion(nombre, Nivel.AVISO, detalle, remedio);
    }

    static Comprobacion problema(String nombre, String detalle, String remedio) {
        return new Comprobacion(nombre, Nivel.PROBLEMA, detalle, remedio);
    }
}
