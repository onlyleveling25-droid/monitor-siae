package co.monitorsiae.portal;

/**
 * Una opcion configurada del paso 1 no aparece en la lista. Puede ser temporal (la plataforma aun
 * no la habilita) o porque la plataforma la renombro.
 */
public class OpcionNoDisponibleException extends PortalException {

    private static final long serialVersionUID = 1L;

    public OpcionNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
