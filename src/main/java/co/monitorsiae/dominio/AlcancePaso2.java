package co.monitorsiae.dominio;

/**
 * Hasta donde debe llegar la automatizacion del paso 2 para un cliente.
 *
 * <p>El paso 2 tiene dos hitos separados en el portal: la pre-asignacion del arma (Modal 1,
 * boton CONFIRMAR tras elegir PORTE o TENENCIA) y el cierre definitivo del tramite (Modal 2,
 * boton FINALIZAR seguido de su propio CONFIRMAR). Algunos clientes prefieren revisar la
 * pre-asignacion manualmente antes de sellarla.
 */
public enum AlcancePaso2 {

    /** Deja el arma pre-asignada (Modal 1) pero no pulsa FINALIZAR ni confirma el cierre. */
    PRE_ASIGNAR_ARMA,

    /** Pre-asigna el arma y ademas sella el tramite completo (Modal 1 y Modal 2). */
    SELLAR_TRAMITE_COMPLETO
}
