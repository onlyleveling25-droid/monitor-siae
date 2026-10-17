package co.monitorsiae.portal.simulado;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.portal.PortalDriver;

import java.util.List;
import java.util.function.Function;

/**
 * Proporciona a cada cliente una escena de demostracion con inventario de pistolas.
 */
public final class FabricaPortalSimulada implements Function<Cliente, PortalDriver> {

    @Override
    public PortalDriver apply(Cliente cliente) {
        List<String> seccionales = cliente.tramite().seccionales();
        String seccional = seccionales.isEmpty()
                ? "Sin seccional configurada"
                : seccionales.getFirst();
        return PortalSimulado.con(Escena.conInventario(seccional,
                "Pistola Cordova Compacta Cal 9MM 9T 3P CORDOVA 9MM",
                "Pistola CZ P10 CAL 9 mm 2P 9T CZ 9MM"));
    }
}
