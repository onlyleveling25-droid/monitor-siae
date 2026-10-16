package co.monitorsiae.dominio;

/**
 * Que hacer cuando el inventario no contiene ninguno de los objetivos del cliente.
 */
public enum PoliticaSinObjetivo {

    /** Ignora el inventario y sigue buscando solo los objetivos. */
    SEGUIR_ESPERANDO,

    /** Toma la primera coincidencia de la lista de alternativas. */
    USAR_ALTERNATIVAS,

    /** Toma cualquier arma cuyo tipo sea el tipo comodin (por ejemplo, cualquier pistola). */
    CUALQUIERA_DEL_TIPO
}
