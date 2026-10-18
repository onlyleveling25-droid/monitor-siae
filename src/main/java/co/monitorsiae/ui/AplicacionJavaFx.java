package co.monitorsiae.ui;

import co.monitorsiae.acceso.CifradorPbkdf2;
import co.monitorsiae.acceso.ServicioAcceso;
import co.monitorsiae.app.CoordinadorMonitores;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.ValidadorConfiguracion;
import co.monitorsiae.monitoreo.Pausa;
import co.monitorsiae.notificacion.Alerta;
import co.monitorsiae.notificacion.CanalSonido;
import co.monitorsiae.notificacion.CanalVoz;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.LocutorWindows;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.real.FabricaPortalPlaywright;
import co.monitorsiae.portal.simulado.FabricaPortalSimulada;
import co.monitorsiae.persistencia.RepositorioConfiguracionJson;
import co.monitorsiae.persistencia.RepositorioSnapshotsJson;
import co.monitorsiae.persistencia.ConfiguracionIlegibleException;
import co.monitorsiae.persistencia.UbicacionDatos;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Arranque y composicion de dependencias de la aplicacion JavaFX.
 */
public final class AplicacionJavaFx extends Application {

    private ServicioAplicacion servicio;
    private CoordinadorMonitores monitores;
    private CentralAlertas centralAlertas;
    private VistaPrincipal vista;
    private Stage ventana;
    private volatile boolean cerrando;

    @Override
    public void start(Stage stage) {
        ventana = stage;
        try {
            UbicacionDatos ubicacion = UbicacionDatos.delUsuario();
            RepositorioConfiguracionJson repositorio =
                    new RepositorioConfiguracionJson(ubicacion.archivoConfiguracion());
            servicio = crearServicio(repositorio);

            CanalAlertaJavaFx canalVentana = new CanalAlertaJavaFx(stage,
                    id -> centralAlertas.reconocer(id), this::enfocarCliente);
            centralAlertas = new CentralAlertas(List.of(
                    canalVentana,
                    new CanalSonido(servicio::configuracion),
                    new CanalVoz(new LocutorWindows(), Pausa.real(), servicio::configuracion)));
            Function<co.monitorsiae.dominio.Cliente, PortalDriver> fabricaPortales = cliente ->
                    switch (servicio.configuracion().entornoPortal()) {
                        case SIMULADOR -> new FabricaPortalSimulada().apply(cliente);
                        case PORTAL_REAL -> new FabricaPortalPlaywright(ubicacion,
                                servicio::configuracion).apply(cliente);
                    };
            monitores = new CoordinadorMonitores(servicio.configuracion(), centralAlertas,
                    fabricaPortales,
                    (id, estado) -> {
                        if (!cerrando) {
                            Platform.runLater(() -> vista.actualizarEstado(id, monitores.estado(id)));
                        }
                    }, new RepositorioSnapshotsJson(ubicacion.archivoHistorialInventarios()));

            vista = new VistaPrincipal(ventana, servicio, monitores, centralAlertas, this::actualizarConfiguracion);
            Scene escena = new Scene(vista, 1100, 720);
            EstilosJavaFx.aplicar(vista);
            stage.setTitle("Monitor SIAE | Centro de monitoreo");
            stage.setMinWidth(900);
            stage.setMinHeight(620);
            stage.setScene(escena);
            stage.setOnCloseRequest(event -> cerrar());
            stage.show();
        } catch (RuntimeException e) {
            mostrarErrorFatal(e);
        }
    }

    private ServicioAplicacion crearServicio(RepositorioConfiguracionJson repositorio) {
        try {
            return new ServicioAplicacion(repositorio,
                    new ServicioAcceso(new CifradorPbkdf2()), new ValidadorConfiguracion());
        } catch (ConfiguracionIlegibleException e) {
            if (!repositorio.existeRespaldo()) {
                throw e;
            }
            javafx.scene.control.Alert recuperar = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    e.getMessage() + "\n\n¿Restaurar la copia de respaldo más reciente?");
            recuperar.initOwner(ventana);
            recuperar.setTitle("Configuración ilegible");
            recuperar.setHeaderText("No se pudo leer la configuración");
            if (recuperar.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                    != javafx.scene.control.ButtonType.OK) {
                throw e;
            }
            repositorio.restaurarRespaldo();
            return new ServicioAplicacion(repositorio,
                    new ServicioAcceso(new CifradorPbkdf2()), new ValidadorConfiguracion());
        }
    }

    private void actualizarConfiguracion(Configuracion configuracion) {
        monitores.actualizarConfiguracion(configuracion);
        centralAlertas.actualizarPreferencias();
        vista.refrescarConfiguracion();
    }

    private CompletableFuture<Void> enfocarCliente(String clienteId) {
        return monitores.enfocarVentana(clienteId);
    }

    private void cerrar() {
        cerrando = true;
        if (monitores != null) {
            monitores.close();
        }
        if (centralAlertas != null) {
            centralAlertas.reconocerTodas();
        }
    }

    private void mostrarErrorFatal(RuntimeException error) {
        org.slf4j.LoggerFactory.getLogger(AplicacionJavaFx.class)
                .error("No se pudo iniciar Monitor SIAE", error);
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "No se pudo iniciar la aplicación:\n" + error.getMessage());
        alerta.setHeaderText("Error al iniciar Monitor SIAE");
        alerta.showAndWait();
        Platform.exit();
    }

    @Override
    public void stop() {
        cerrar();
    }
}
