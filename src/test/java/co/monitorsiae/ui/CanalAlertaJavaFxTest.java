package co.monitorsiae.ui;

import co.monitorsiae.notificacion.Alerta;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.Gravedad;
import co.monitorsiae.portal.SesionPortalCerradaException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanalAlertaJavaFxTest {
    @Test
    void sesionCerradaReconoceSoloLaAlertaDelClienteSolicitado() {
        CentralAlertas central = centralConDosClientes();
        Throwable error = new CompletionException(new ExecutionException(
                new SesionPortalCerradaException("La sesión se cerró")));

        CanalAlertaJavaFx.reconocerSiSesionCerrada("ana", error, central::reconocer);

        assertEquals(List.of("luis"), central.activas().stream().map(Alerta::clienteId).toList());
    }

    @Test
    void errorDeEnfoqueOSolicitudExitosaNoSilenciaAlertas() {
        CentralAlertas central = centralConDosClientes();

        CanalAlertaJavaFx.reconocerSiSesionCerrada("ana",
                new CompletionException(new IllegalStateException("No se pudo enfocar")), central::reconocer);
        CanalAlertaJavaFx.reconocerSiSesionCerrada("ana", null, central::reconocer);

        assertEquals(List.of("ana", "luis"), central.activas().stream().map(Alerta::clienteId).toList());
    }

    private static CentralAlertas centralConDosClientes() {
        var central = new CentralAlertas(List.of());
        central.emitir(new Alerta("ana", Gravedad.PROBLEMA, "Aviso", "", ""));
        central.emitir(new Alerta("luis", Gravedad.PROBLEMA, "Aviso", "", ""));
        return central;
    }
}
