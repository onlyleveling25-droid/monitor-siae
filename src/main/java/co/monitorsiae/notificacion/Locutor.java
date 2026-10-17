package co.monitorsiae.notificacion;

/**
 * Lee un texto en voz alta y espera a terminar.
 */
@FunctionalInterface
public interface Locutor {

    void decir(String texto) throws InterruptedException;
}
