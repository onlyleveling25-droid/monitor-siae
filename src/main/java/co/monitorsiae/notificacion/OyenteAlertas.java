package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.monitoreo.InventarioNuevo;
import co.monitorsiae.monitoreo.MotivoDetencion;
import co.monitorsiae.monitoreo.OyenteMonitoreo;
import co.monitorsiae.monitoreo.ResultadoCiclo;

/**
 * Conecta el monitor con las alarmas.
 */
public final class OyenteAlertas implements OyenteMonitoreo {

    private final TraductorAlertas traductor;
    private final CentralAlertas central;
    private final InventarioNuevo inventarioNuevo = new InventarioNuevo();

    public OyenteAlertas(TraductorAlertas traductor, CentralAlertas central) {
        this.traductor = traductor;
        this.central = central;
    }

    @Override
    public void alTerminarCiclo(Cliente cliente, ResultadoCiclo resultado) {
        boolean nuevo = inventarioNuevo.registrar(cliente.id(), resultado);
        if (resultado instanceof ResultadoCiclo.SinArmaDeInteres && !nuevo) {
            return;   // el mismo inventario sin interés ya sonó una vez
        }
        traductor.deResultado(cliente, resultado).ifPresent(central::emitir);
    }

    @Override
    public void alIniciar(Cliente cliente) {
        inventarioNuevo.olvidar(cliente.id());
    }

    @Override
    public void alDetenerse(Cliente cliente, MotivoDetencion motivo) {
        traductor.deDetencion(cliente, motivo).ifPresent(central::emitir);
    }
}
