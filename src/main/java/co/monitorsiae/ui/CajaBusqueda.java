package co.monitorsiae.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Casilla de búsqueda con lupa y botón para borrar, compartida por el Panel y la Bitácora para que
 * se vean y funcionen igual. La ✕ aparece solo si hay texto; Esc también borra la búsqueda.
 */
final class CajaBusqueda {

    private CajaBusqueda() {
    }

    static HBox envolver(TextField campo) {
        campo.getStyleClass().add("client-search-field");
        HBox.setHgrow(campo, Priority.ALWAYS);

        Button limpiar = new Button();
        limpiar.getStyleClass().add("client-search-clear");
        limpiar.setGraphic(IconoSvg.crear("close", 12, "#64748B"));
        limpiar.setTooltip(new Tooltip("Borrar la búsqueda (Esc)"));
        limpiar.setAccessibleText("Borrar la búsqueda");
        limpiar.setFocusTraversable(false);
        limpiar.visibleProperty().bind(campo.textProperty().isNotEmpty());
        limpiar.managedProperty().bind(limpiar.visibleProperty());
        limpiar.setOnAction(event -> {
            campo.clear();
            campo.requestFocus();
        });
        campo.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && !campo.getText().isEmpty()) {
                campo.clear();
                event.consume();
            }
        });

        HBox caja = new HBox(6, IconoSvg.crear("search", 15, "#64748B"), campo, limpiar);
        caja.getStyleClass().add("client-search");
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }
}
