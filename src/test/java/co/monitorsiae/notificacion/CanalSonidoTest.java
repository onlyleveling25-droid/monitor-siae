package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.NivelEscalamiento;
import co.monitorsiae.dominio.PerfilEscalamiento;
import co.monitorsiae.dominio.PreferenciaSonidoCliente;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void siElPerfilEstaDeshabilitadoUnaAlertaSilenciadaNoSuenaIgualQueAntes() throws InterruptedException {
        Configuracion configuracion = Configuracion.porDefecto()
                .conPreferenciasAlertas(new PreferenciasAlertas(false, true, TonoAlarma.SIRENA));
        CanalSonido canal = new CanalSonido(() -> configuracion, id -> Duration.ofHours(1));

        canal.mostrar(List.of(alerta("ana")));

        assertTrue(canal.alertasAudiblesActualmente().isEmpty());
    }

    @Test
    void unNivelDeEscalamientoForzarSonidoHaceSonarUnaAlertaSilenciada() throws InterruptedException {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(new NivelEscalamiento(Duration.ofMinutes(5), true, false, TonoAlarma.DOBLE)));
        Configuracion configuracion = Configuracion.porDefecto()
                .conPreferenciasAlertas(new PreferenciasAlertas(false, true, TonoAlarma.SIRENA, perfil));
        CanalSonido canal = new CanalSonido(() -> configuracion, id -> Duration.ofMinutes(10));

        canal.mostrar(List.of(alerta("ana")));

        assertEquals(List.of("ana"), canal.alertasAudiblesActualmente().stream().map(Alerta::clienteId).toList());
    }

    @Test
    void unNivelDeEscalamientoQueAunNoCumpleSuEsperaNoFuerzaElSonido() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(new NivelEscalamiento(Duration.ofMinutes(5), true, false, TonoAlarma.DOBLE)));
        Configuracion configuracion = Configuracion.porDefecto()
                .conPreferenciasAlertas(new PreferenciasAlertas(false, true, TonoAlarma.SIRENA, perfil));
        CanalSonido canal = new CanalSonido(() -> configuracion, id -> Duration.ofMinutes(2));

        canal.mostrar(List.of(alerta("ana")));

        assertTrue(canal.alertasAudiblesActualmente().isEmpty());
    }

    private static Cliente conPreferencia(Cliente cliente, PreferenciaSonidoCliente preferencia) {
        return new Cliente(cliente.id(), cliente.nombre(), cliente.activo(), cliente.tramite(), cliente.modo(),
                cliente.seleccion(), preferencia);
    }

    private static Alerta alerta(String id) {
        return new Alerta(id, Gravedad.OPORTUNIDAD, "Alerta", "", "Alerta de prueba");
    }
}
