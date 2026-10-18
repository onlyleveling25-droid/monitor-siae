package co.monitorsiae.ui;

import co.monitorsiae.notificacion.Alerta;
import co.monitorsiae.notificacion.CanalAlerta;
import co.monitorsiae.portal.SesionPortalCerradaException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Taskbar;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Presenta las alertas activas en una ventana visible y permite reconocerlas.
 */
public final class CanalAlertaJavaFx implements CanalAlerta {

    private final Window propietario;
    private final Consumer<String> alReconocer;
    private final Function<String, CompletableFuture<Void>> alEnfocar;
    private final Stage ventana = new Stage();
    private final VBox lista = new VBox(12);

    public CanalAlertaJavaFx(Window propietario, Consumer<String> alReconocer,
                             Function<String, CompletableFuture<Void>> alEnfocar) {
        this.propietario = propietario;
        this.alReconocer = alReconocer;
        this.alEnfocar = alEnfocar;
        ventana.initOwner(propietario);
        ventana.initModality(Modality.NONE);
        ventana.setTitle("Monitor SIAE · Alertas");
        lista.setPadding(new Insets(22));
        ventana.setScene(new Scene(lista, 440, 280));
        EstilosJavaFx.aplicar(lista);
    }

    @Override
    public void mostrar(List<Alerta> activas) {
        List<Alerta> copia = List.copyOf(activas);
        Platform.runLater(() -> actualizar(copia));
    }

    private void actualizar(List<Alerta> activas) {
        lista.getChildren().clear();
        if (activas.isEmpty()) {
            ventana.setAlwaysOnTop(false);
            ventana.hide();
            solicitarAtencion(false);
            return;
        }
        solicitarAtencion(true);
        Label encabezado = new Label("Requiere tu atención");
        encabezado.getStyleClass().add("alert-heading");
        lista.getChildren().add(encabezado);
        for (Alerta alerta : activas) {
            VBox tarjeta = new VBox(7);
            tarjeta.getStyleClass().add(alerta.gravedad().name().equals("OPORTUNIDAD")
                    ? "alert-opportunity" : "alert-problem");
            Label titulo = new Label(alerta.titulo());
            titulo.getStyleClass().add("alert-title");
            Label detalle = new Label(alerta.detalle());
            detalle.setWrapText(true);
            Label resultadoEnfoque = new Label();
            resultadoEnfoque.getStyleClass().add("field-help");
            resultadoEnfoque.setWrapText(true);
            Button irAlNavegador = new Button("Ir al navegador");
            irAlNavegador.getStyleClass().add("secondary-button");
            irAlNavegador.setOnAction(event -> {
                irAlNavegador.setDisable(true);
                resultadoEnfoque.setText("Conectando con la sesión del cliente…");
                ventana.setAlwaysOnTop(false);
                ventana.toBack();
                try {
                    alEnfocar.apply(alerta.clienteId()).whenComplete((ignorado, error) ->
                            Platform.runLater(() -> {
                                irAlNavegador.setDisable(false);
                                if (error == null) {
                                    resultadoEnfoque.setText(mensajeEnfoqueSolicitado());
                                } else {
                                    resultadoEnfoque.setText(mensajeErrorEnfoque(error));
                                    if (esSesionCerrada(error)) {
                                        alReconocer.accept(alerta.clienteId());
                                    }
                                }
                            }));
                } catch (RuntimeException error) {
                    irAlNavegador.setDisable(false);
                    resultadoEnfoque.setText(mensajeErrorEnfoque(error));
                    if (esSesionCerrada(error)) {
                        alReconocer.accept(alerta.clienteId());
                    }
                }
            });
            Button reconocer = new Button("Entendido");
            reconocer.setOnAction(event -> alReconocer.accept(alerta.clienteId()));
            tarjeta.getChildren().addAll(titulo, detalle, irAlNavegador, resultadoEnfoque, reconocer);
            lista.getChildren().add(tarjeta);
        }
        ventana.sizeToScene();
        if (!ventana.isShowing()) {
            ventana.show();
        }
        ventana.setAlwaysOnTop(true);
        ventana.toFront();
        ventana.requestFocus();
    }

    private static String mensajeErrorEnfoque(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null
                && (causa instanceof java.util.concurrent.CompletionException
                || causa instanceof java.util.concurrent.ExecutionException)) {
            causa = causa.getCause();
        }
        return causa.getMessage() == null || causa.getMessage().isBlank()
                ? "No se pudo enfocar la ventana del navegador."
                : causa.getMessage();
    }

    private static boolean esSesionCerrada(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SesionPortalCerradaException) {
                return true;
            }
        }
        return false;
    }

    private static String mensajeEnfoqueSolicitado() {
        return "Solicitud enviada. Si Windows no la trae al frente, selecciónala en la barra de tareas.";
    }

    private static void solicitarAtencion(boolean activa) {
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.USER_ATTENTION)) {
                taskbar.requestUserAttention(activa, true);
            }
        }
    }
}
