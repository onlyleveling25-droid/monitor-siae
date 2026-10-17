package co.monitorsiae.app;

import co.monitorsiae.ui.AplicacionJavaFx;
import javafx.application.Application;

/**
 * Punto de entrada separado de Application para mantener el arranque JavaFX compatible con Maven.
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] argumentos) {
        Application.launch(AplicacionJavaFx.class, argumentos);
    }
}
