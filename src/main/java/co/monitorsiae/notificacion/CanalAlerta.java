package co.monitorsiae.notificacion;

import java.util.List;

/**
 * Una forma de avisar: voz, sonido, ventana. Recibe siempre la lista completa de alertas activas,
 * no eventos sueltos, para que no pueda quedar desincronizado. Una lista vacia significa callar.
 *
 * <p>Las implementaciones no deben bloquear a quien las llama.
 */
public interface CanalAlerta {

    void mostrar(List<Alerta> activas);
}
