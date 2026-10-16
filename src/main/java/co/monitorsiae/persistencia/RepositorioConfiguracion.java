package co.monitorsiae.persistencia;

import co.monitorsiae.dominio.Configuracion;

/**
 * Lugar donde se guarda la configuracion. El resto de la aplicacion no sabe si es un archivo,
 * una base de datos o memoria.
 */
public interface RepositorioConfiguracion {

    /**
     * Devuelve la configuracion guardada, o la configuracion por defecto si aun no existe.
     *
     * @throws ConfiguracionIlegibleException si existe pero no se puede leer
     */
    Configuracion cargar();

    void guardar(Configuracion configuracion);

    /** Indica si hay una copia de respaldo de la que se pueda restaurar. */
    boolean existeRespaldo();

    /**
     * Reemplaza la configuracion actual por la copia de respaldo y la devuelve.
     *
     * @throws ConfiguracionIlegibleException si el respaldo tampoco se puede leer
     */
    Configuracion restaurarRespaldo();
}
