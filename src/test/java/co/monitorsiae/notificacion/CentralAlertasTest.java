package co.monitorsiae.notificacion;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    void reportaElTiempoTranscurridoDesdeQueLaAlertaQuedoActiva() {
        AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2024-01-01T00:00:00Z"));
        Clock reloj = Clock.fixed(ahora.get(), ZoneOffset.UTC);
        CentralAlertas central = new CentralAlertas(List.of(), new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return reloj.getZone();
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return ahora.get();
            }
        });

        central.emitir(alerta("ana", "uno"));
        ahora.set(ahora.get().plus(Duration.ofMinutes(5)));

        assertEquals(Duration.ofMinutes(5), central.tiempoActivo("ana"));
    }

    @Test
    void unClienteSinAlertaActivaReportaTiempoCero() {
        assertEquals(Duration.ZERO, central.tiempoActivo("nadie"));
    }

    @Test
    void reconocerReiniciaElTiempoActivo() {
        central.emitir(alerta("ana", "uno"));
        central.reconocer("ana");

        assertEquals(Duration.ZERO, central.tiempoActivo("ana"));
    }

    private static Alerta alerta(String clienteId, String titulo) {
        return new Alerta(clienteId, Gravedad.OPORTUNIDAD, titulo, "", titulo);
    }
}
