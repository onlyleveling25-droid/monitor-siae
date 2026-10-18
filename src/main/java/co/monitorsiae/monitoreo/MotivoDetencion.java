package co.monitorsiae.monitoreo;

/**
 * Por que se detuvo el monitor de un cliente.
 */
public enum MotivoDetencion {
    SOLICITADO_POR_USUARIO,
    ARMA_APARTADA,
    ARMA_ENCONTRADA,
    SESION_VENCIDA,
    FALLOS_REPETIDOS,
    NAVEGADOR_CERRADO,
    ERROR_INESPERADO
}
