package co.monitorsiae.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.Optional;

final class DialogosContrasena {

    private DialogosContrasena() {
    }

    static Optional<char[]> pedirActual(Window propietario, String titulo) {
        Dialog<char[]> dialogo = nuevo(propietario, titulo);
        PasswordField actual = new PasswordField();
        GridPane contenido = contenido("Ingresa la contraseña de la aplicación para continuar.", actual);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.setResultConverter(boton -> {
            if (boton.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                actual.clear();
                return null;
            }
            char[] valor = actual.getText().toCharArray();
            actual.clear();
            return valor;
        });
        return dialogo.showAndWait();
    }

    static Optional<char[][]> pedirNueva(Window propietario, String titulo) {
        Dialog<char[][]> dialogo = nuevo(propietario, titulo);
        PasswordField nueva = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        GridPane contenido = new GridPane();
        contenido.setHgap(12);
        contenido.setVgap(12);
        contenido.setPadding(new Insets(18));
        contenido.add(new Label("Mínimo 6 caracteres"), 0, 0, 2, 1);
        contenido.add(new Label("Nueva contraseña"), 0, 1);
        contenido.add(nueva, 1, 1);
        contenido.add(new Label("Confirmar"), 0, 2);
        contenido.add(confirmacion, 1, 2);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.setResultConverter(boton -> {
            if (boton.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                nueva.clear();
                confirmacion.clear();
                return null;
            }
            char[][] valores = {nueva.getText().toCharArray(), confirmacion.getText().toCharArray()};
            nueva.clear();
            confirmacion.clear();
            return valores;
        });
        return dialogo.showAndWait();
    }

    static void borrar(char[] valor) {
        if (valor != null) {
            Arrays.fill(valor, '\0');
        }
    }

    private static <T> Dialog<T> nuevo(Window propietario, String titulo) {
        Dialog<T> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle(titulo);
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        dialogo.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        return dialogo;
    }

    private static GridPane contenido(String texto, PasswordField campo) {
        GridPane contenido = new GridPane();
        contenido.setHgap(12);
        contenido.setVgap(12);
        contenido.setPadding(new Insets(18));
        contenido.add(new Label(texto), 0, 0, 2, 1);
        contenido.add(new Label("Contraseña"), 0, 1);
        contenido.add(campo, 1, 1);
        return contenido;
    }
}
