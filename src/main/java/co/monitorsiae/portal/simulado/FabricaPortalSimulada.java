package co.monitorsiae.portal.simulado;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.EscenarioSimulador;
import co.monitorsiae.portal.PortalDriver;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Proporciona a cada cliente un portal simulado que sigue el escenario elegido en la configuración. El
 * escenario se consulta cada vez que se crea el portal, así que un cambio se aplica al siguiente inicio.
 */
public final class FabricaPortalSimulada implements Function<Cliente, PortalDriver> {

    private final Supplier<EscenarioSimulador> escenario;

    public FabricaPortalSimulada() {
        this(EscenarioSimulador::porDefecto);
    }

    public FabricaPortalSimulada(Supplier<EscenarioSimulador> escenario) {
        this.escenario = escenario;
    }

    @Override
    public PortalDriver apply(Cliente cliente) {
        return new PortalSimulado(GeneradorEscenas.para(escenario.get(), cliente));
    }
}
