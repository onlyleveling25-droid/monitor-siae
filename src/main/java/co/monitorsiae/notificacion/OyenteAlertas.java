package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.monitoreo.MotivoDetencion;
import co.monitorsiae.monitoreo.OyenteMonitoreo;
import co.monitorsiae.monitoreo.ResultadoCiclo;

/**
 * Conecta el monitor con las alarmas.
 */
public final class OyenteAlertas implements OyenteMonitoreo {

    private final TraductorAlertas traductor;
    private final CentralAlertas central;

    public OyenteAlertas(TraductorAlertas traductor, CentralAlertas central) {
        this.traductor = traductor;
        this.central = central;
    }

    @Override
    public void alTerminarCiclo(Cliente cliente, ResultadoCiclo resultado) {
        traductor.deResultado(cliente, resultado).ifPresent(central::emitir);
    }

    @Override
    public void alDetenerse(Cliente cliente, MotivoDetencion motivo) {
        traductor.deDetencion(cliente, motivo).ifPresent(central::emitir);
    }
}
