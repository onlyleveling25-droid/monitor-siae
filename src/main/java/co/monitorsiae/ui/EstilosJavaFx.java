package co.monitorsiae.ui;

import javafx.scene.Parent;

final class EstilosJavaFx {

    private EstilosJavaFx() {
    }

    static void aplicar(Parent raiz) {
        var hoja = EstilosJavaFx.class.getResource("monitor-siae.css");
        if (hoja == null) {
            throw new IllegalStateException("No se encontró monitor-siae.css.");
        }
        raiz.getStylesheets().add(hoja.toExternalForm());
    }
}
