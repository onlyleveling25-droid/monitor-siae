package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.PreferenciaSonidoCliente;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanalSonidoTest {

    @Test
    void aplicaLaPreferenciaGeneralYLaSobrescrituraPorCliente() {
        Cliente ana = conPreferencia(Cliente.nuevo("Ana"), PreferenciaSonidoCliente.ACTIVADO);
        Cliente luis = conPreferencia(Cliente.nuevo("Luis"), PreferenciaSonidoCliente.HEREDAR);
        Configuracion configuracion = Configuracion.porDefecto()
                .conClientes(List.of(ana, luis))
                .conPreferenciasAlertas(new PreferenciasAlertas(false, true, TonoAlarma.SIRENA));
        List<Alerta> activas = List.of(alerta(ana.id()), alerta(luis.id()), alerta("desconocido"));

        List<Alerta> audibles = CanalSonido.filtrarPorPreferencia(configuracion, activas);

        assertEquals(List.of(ana.id()), audibles.stream().map(Alerta::clienteId).toList());
    }

    private static Cliente conPreferencia(Cliente cliente, PreferenciaSonidoCliente preferencia) {
        return new Cliente(cliente.id(), cliente.nombre(), cliente.activo(), cliente.tramite(), cliente.modo(),
                cliente.seleccion(), preferencia);
    }

    private static Alerta alerta(String id) {
        return new Alerta(id, Gravedad.OPORTUNIDAD, "Alerta", "", "Alerta de prueba");
    }
}
