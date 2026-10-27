package co.monitorsiae.ui;

import co.monitorsiae.app.AsistentePrimeraEjecucion;
import co.monitorsiae.app.AsistentePrimeraEjecucion.Paso;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.NavegadorPreferido;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Asistente de primera ejecución: elegir navegador, crear el primer cliente y elegir el entorno
 * del portal. Las reglas (qué es válido, qué se aplica) viven en {@link AsistentePrimeraEjecucion};
 * esta clase solo presenta los pasos y recoge las respuestas.
 */
final class AsistentePrimeraEjecucionDialogo {

    private final ComboBox<NavegadorPreferido> navegador = new ComboBox<>();
    private final TextField nombreCliente = new TextField();
    private final ComboBox<EntornoPortal> entorno = new ComboBox<>();
    private final Label mensajeError = new Label();
    private final Dialog<Configuracion> dialogo = new Dialog<>();
    private final ButtonType botonSiguiente = new ButtonType("Siguiente", ButtonBar.ButtonData.OK_DONE);
    private final ButtonType botonAtras = new ButtonType("Atrás", ButtonBar.ButtonData.BACK_PREVIOUS);
    private final ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
    private int indicePaso;

    private AsistentePrimeraEjecucionDialogo() {
    }

    /** Muestra el asistente; vacío si la persona canceló en cualquier paso. */
    static Optional<Configuracion> mostrar(Window propietario, Configuracion base) {
        return new AsistentePrimeraEjecucionDialogo().ejecutar(propietario, base);
    }

    private Optional<Configuracion> ejecutar(Window propietario, Configuracion base) {
        navegador.setItems(FXCollections.observableArrayList(NavegadorPreferido.values()));
        navegador.setValue(base.navegador());
        entorno.setItems(FXCollections.observableArrayList(EntornoPortal.values()));
        entorno.setCellFactory(ignorado -> celdaEntorno());
        entorno.setButtonCell(celdaEntorno());
        entorno.setValue(base.entornoPortal());
        mensajeError.getStyleClass().add("field-error");
        mensajeError.setVisible(false);
        mensajeError.setManaged(false);

        // En la primera ejecucion la ventana principal aun no tiene escena y JavaFX no admite
        // un propietario asi (NullPointerException); el asistente se abre entonces sin propietario.
        if (propietario != null && propietario.getScene() != null) {
            dialogo.initOwner(propietario);
        }
        dialogo.setTitle("Bienvenido a Monitor SIAE");
        dialogo.setResizable(false);
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        indicePaso = 0;
        actualizarPaso();

        dialogo.setResultConverter(boton -> boton != null && boton.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? AsistentePrimeraEjecucion.aplicar(base, navegador.getValue(), nombreCliente.getText(),
                        entorno.getValue())
                : null);

        return dialogo.showAndWait();
    }

    /**
     * El botón "Atrás" y el "Siguiente" de un paso intermedio no deben cerrar el diálogo: se
     * consume el evento y solo se cambia el contenido al paso correspondiente. Como el botón del
     * último paso sí debe cerrar (dispara el resultConverter), su filtro solo valida.
     */
    private void engancharNavegacion() {
        Button atras = (Button) dialogo.getDialogPane().lookupButton(botonAtras);
        if (atras != null) {
            atras.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
                indicePaso--;
                actualizarPaso();
                evento.consume();
            });
        }
        Button siguiente = (Button) dialogo.getDialogPane().lookupButton(botonSiguiente);
        if (siguiente != null) {
            boolean esUltimoPaso = indicePaso == AsistentePrimeraEjecucion.PASOS.size() - 1;
            siguiente.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
                if (!avanzar()) {
                    evento.consume();
                } else if (!esUltimoPaso) {
                    indicePaso++;
                    actualizarPaso();
                    evento.consume();
                }
            });
        }
    }

    /** Valida el paso actual; si falla, muestra el error y devuelve falso sin avanzar. */
    private boolean avanzar() {
        Paso paso = AsistentePrimeraEjecucion.PASOS.get(indicePaso);
        if (paso == Paso.PRIMER_CLIENTE) {
            String error = AsistentePrimeraEjecucion.validarNombreCliente(nombreCliente.getText());
            if (error != null) {
                mensajeError.setText(error);
                mensajeError.setVisible(true);
                mensajeError.setManaged(true);
                return false;
            }
        }
        return true;
    }

    private void actualizarPaso() {
        Paso paso = AsistentePrimeraEjecucion.PASOS.get(indicePaso);
        mensajeError.setVisible(false);
        mensajeError.setManaged(false);
        DialogPane panel = dialogo.getDialogPane();
        panel.setHeaderText(tituloDePaso(paso));
        panel.setContent(contenidoDePaso(paso));
        panel.getButtonTypes().setAll(botonCancelar);
        if (indicePaso > 0) {
            panel.getButtonTypes().add(botonAtras);
        }
        panel.getButtonTypes().add(botonSiguiente);
        Button siguiente = (Button) panel.lookupButton(botonSiguiente);
        if (siguiente != null) {
            siguiente.setText(paso == Paso.RESUMEN ? "Empezar a usar Monitor SIAE" : "Siguiente");
        }
        engancharNavegacion();
    }

    private static String tituloDePaso(Paso paso) {
        return switch (paso) {
            case BIENVENIDA -> "Bienvenido";
            case NAVEGADOR -> "Elige el navegador";
            case PRIMER_CLIENTE -> "Crea el primer cliente";
            case ENTORNO_PORTAL -> "Elige el entorno del portal";
            case RESUMEN -> "Todo listo";
        };
    }

    private VBox contenidoDePaso(Paso paso) {
        VBox caja = new VBox(12);
        caja.setPadding(new Insets(18));
        caja.setPrefWidth(420);
        switch (paso) {
            case BIENVENIDA -> {
                Label texto = new Label(
                        "Este asistente te ayuda a dejar Monitor SIAE listo para vigilar tu primer cliente: "
                                + "el navegador que vas a usar, el nombre del cliente y si vas a trabajar con el "
                                + "portal real o con el simulador de pruebas.");
                texto.setWrapText(true);
                caja.getChildren().add(texto);
            }
            case NAVEGADOR -> {
                Label texto = new Label("¿Qué navegador instalado vas a usar para controlar el portal?");
                texto.setWrapText(true);
                caja.getChildren().addAll(texto, navegador);
            }
            case PRIMER_CLIENTE -> {
                Label texto = new Label("Escribe el nombre de la persona para la que vas a tramitar el permiso. "
                        + "Podrás agregar más clientes y ajustar sus datos después, en Configuración.");
                texto.setWrapText(true);
                nombreCliente.setPromptText("Ej. Ana López");
                caja.getChildren().addAll(texto, nombreCliente, mensajeError);
            }
            case ENTORNO_PORTAL -> {
                Label texto = new Label(
                        "Usa el simulador para practicar sin afectar el portal real. Cuando estés listo para "
                                + "monitorear de verdad, elige el portal real y abre sesión manualmente en el "
                                + "navegador de cada cliente antes de iniciar.");
                texto.setWrapText(true);
                caja.getChildren().addAll(texto, entorno);
            }
            case RESUMEN -> {
                Label texto = new Label("Guardaremos esta configuración inicial. Puedes cambiar cualquier ajuste "
                        + "más adelante desde Configuración.");
                texto.setWrapText(true);
                caja.getChildren().add(texto);
            }
        }
        return caja;
    }

    private static ListCell<EntornoPortal> celdaEntorno() {
        return new ListCell<>() {
            @Override
            protected void updateItem(EntornoPortal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : switch (valor) {
                    case SIMULADOR -> "Simulador (pruebas)";
                    case PORTAL_REAL -> "Portal real (sesión manual)";
                });
            }
        };
    }
}
