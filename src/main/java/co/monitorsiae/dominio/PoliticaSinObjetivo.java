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
    CUALQUIERA_DEL_TIPO,

    /** Prueba las alternativas y, si ninguna aparece, toma la primera arma del tipo comodin. */
    USAR_ALTERNATIVAS_LUEGO_TIPO
}
