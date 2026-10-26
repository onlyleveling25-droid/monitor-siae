package co.monitorsiae.ui;

import co.monitorsiae.app.Comprobacion;
import co.monitorsiae.app.Diagnostico;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

/**
 * Veredicto general y comprobaciones independientes. Se ejecuta fuera del hilo de la interfaz y no
 * toca el portal ni los navegadores.
 */
final class PaginaDiagnostico extends VBox {

    private final Supplier<Diagnostico> diagnostico;
    private final Button ejecutar = new Button("Ejecutar diagnóstico");
    private final Label veredicto = new Label("Aún no se ha ejecutado.");
    private final VBox resultados = new VBox(10);
    private boolean enCurso;

    PaginaDiagnostico(Supplier<Diagnostico> diagnostico) {
        super(18);
        this.diagnostico = diagnostico;
        setPadding(new Insets(28, 32, 28, 32));
        Label titulo = new Label("Diagnóstico");
        titulo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Revisa configuración, navegador, almacenamiento, audio y horario. "
                + "No abre el portal ni cambia nada.");
        subtitulo.getStyleClass().add("muted");
        subtitulo.setWrapText(true);
        VBox titulos = new VBox(4, titulo, subtitulo);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        ejecutar.getStyleClass().add("primary-button");
        ejecutar.setOnAction(e -> ejecutar());
        HBox encabezado = new HBox(14, titulos, ejecutar);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.getStyleClass().add("page-header");

        veredicto.getStyleClass().add("section-title");
        ScrollPane desplazamiento = new ScrollPane(resultados);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        getChildren().addAll(encabezado, veredicto, desplazamiento);
    }

    void ejecutar() {
        if (enCurso) {
            return;
        }
        enCurso = true;
        ejecutar.setDisable(true);
        veredicto.setText("Revisando…");
        Thread hilo = new Thread(() -> {
            try {
                Diagnostico resultado = diagnostico.get();
                Platform.runLater(() -> mostrar(resultado));
            } catch (RuntimeException e) {
                Platform.runLater(() -> fallo(e));
            }
        }, "diagnostico");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrar(Diagnostico resultado) {
        resultados.getChildren().setAll(resultado.comprobaciones().stream().map(PaginaDiagnostico::tarjeta).toList());
        veredicto.setText(resultado.resumen());
        terminar();
    }

    private void fallo(RuntimeException error) {
        resultados.getChildren().clear();
        veredicto.setText("No se pudo ejecutar el diagnóstico: " + error.getMessage());
        terminar();
    }

    private void terminar() {
        enCurso = false;
        ejecutar.setDisable(false);
    }

    private static VBox tarjeta(Comprobacion c) {
        Label estado = new Label(switch (c.nivel()) {
            case CORRECTO -> "Correcto";
            case AVISO -> "Revisar";
            case PROBLEMA -> "Problema";
        });
        estado.getStyleClass().addAll("status-pill", switch (c.nivel()) {
            case CORRECTO -> "status-success";
            case AVISO -> "status-waiting";
            case PROBLEMA -> "status-error";
        });
        Label nombre = new Label(c.nombre());
        nombre.getStyleClass().add("client-name");
        HBox.setHgrow(nombre, Priority.ALWAYS);
        nombre.setMaxWidth(Double.MAX_VALUE);
        Label detalle = new Label(c.detalle());
        detalle.setWrapText(true);
        VBox tarjeta = new VBox(6, new HBox(10, nombre, estado), detalle);
        if (!c.remedio().isBlank()) {
            Label remedio = new Label("Qué hacer: " + c.remedio());
            remedio.getStyleClass().add("muted");
            remedio.setWrapText(true);
            tarjeta.getChildren().add(remedio);
        }
        tarjeta.getStyleClass().add("client-row");
        tarjeta.setPadding(new Insets(14, 16, 14, 16));
        tarjeta.setAccessibleText(c.nombre() + ": " + c.detalle());
        return tarjeta;
    }
}
