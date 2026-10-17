package co.monitorsiae.notificacion;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentralAlertasTest {

    private final List<List<Alerta>> recibidas = new ArrayList<>();
    private final CentralAlertas central = new CentralAlertas(List.of(recibidas::add));

    @Test
    void reparteLaListaCompletaACadaCanal() {
        central.emitir(alerta("ana", "uno"));
        central.emitir(alerta("luis", "dos"));

        assertEquals(2, recibidas.get(1).size());
    }

    @Test
    void unaAlertaNuevaDelMismoClienteReemplazaLaAnterior() {
        central.emitir(alerta("ana", "sesion vencida"));
        central.emitir(alerta("ana", "arma apartada"));

        assertEquals(1, central.activas().size());
        assertEquals("arma apartada", central.activas().get(0).titulo());
    }

    @Test
    void reconocerCallaSoloEseCliente() {
        central.emitir(alerta("ana", "uno"));
        central.emitir(alerta("luis", "dos"));

        central.reconocer("ana");

        assertEquals(List.of("luis"), central.activas().stream().map(Alerta::clienteId).toList());
        assertEquals(1, recibidas.get(recibidas.size() - 1).size());
    }

    @Test
    void reconocerTodasEnviaListaVacia() {
        central.emitir(alerta("ana", "uno"));

        central.reconocerTodas();

        assertTrue(recibidas.get(recibidas.size() - 1).isEmpty());
    }

    @Test
    void reconocerAlgoInexistenteNoMolestaALosCanales() {
        central.reconocer("nadie");
        central.reconocerTodas();

        assertTrue(recibidas.isEmpty());
    }

    private static Alerta alerta(String clienteId, String titulo) {
        return new Alerta(clienteId, Gravedad.OPORTUNIDAD, titulo, "", titulo);
    }
}
