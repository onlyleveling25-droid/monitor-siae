package co.monitorsiae.ui;

import co.monitorsiae.acceso.ContrasenaRechazadaException;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ModoReaccion;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Editor de configuracion general y de los perfiles de clientes.
 */
final class EditorConfiguracion {

    private static final Map<DayOfWeek, String> DIAS = Map.of(
            DayOfWeek.MONDAY, "Lun", DayOfWeek.TUESDAY, "Mar", DayOfWeek.WEDNESDAY, "Mié",
            DayOfWeek.THURSDAY, "Jue", DayOfWeek.FRIDAY, "Vie", DayOfWeek.SATURDAY, "Sáb",
            DayOfWeek.SUNDAY, "Dom");

    private final Window propietario;
    private final ServicioAplicacion servicio;
    private final Consumer<Configuracion> alGuardar;
    private final Dialog<Configuracion> dialogo = new Dialog<>();
    private final ComboBox<NavegadorPreferido> navegador = new ComboBox<>();
    private final Spinner<Integer> intervalo = new Spinner<>(2, 3600, 5);
    private final CheckBox restringirHorario = new CheckBox("Limitar el monitoreo a un horario");
    private final Map<DayOfWeek, CheckBox> dias = new EnumMap<>(DayOfWeek.class);
    private final TextField desde = new TextField();
    private final TextField hasta = new TextField();
    private final CheckBox proteger = new CheckBox("Solicitar contraseña para editar la configuración");
    private final ListView<Cliente> listaClientes = new ListView<>();
    private final Map<String, Cliente> borradores = new LinkedHashMap<>();
    private final TextField nombre = new TextField();
    private final CheckBox activo = new CheckBox("Cliente incluido en el monitoreo");
    private final TextField permiso = new TextField();
    private final TextField tipoTramite = new TextField();
    private final TextArea seccionales = area("Una seccional por línea, en orden de preferencia");
    private final TextArea descripcion = area("Descripción del trámite (150–500 caracteres)");
    private final Label contadorDescripcion = new Label();
    private final ComboBox<ModoReaccion> modo = new ComboBox<>();
    private final ComboBox<Modalidad> modalidad = new ComboBox<>();
    private final TextArea objetivos = area("Un criterio por línea; separa palabras clave con coma. Ej.: glock, g-43");
    private final ComboBox<PoliticaSinObjetivo> politica = new ComboBox<>();
    private final TextArea alternativas = area("Un criterio alternativo por línea; palabras clave separadas por coma");
    private final TextField tipoComodin = new TextField("Pistola");
    private final TextArea excluir = area("Una palabra de exclusión por línea");
    private final Spinner<Integer> reintentos = new Spinner<>(1, 5, 2);
    private String clienteActualId;

    EditorConfiguracion(Window propietario, ServicioAplicacion servicio, Consumer<Configuracion> alGuardar) {
        this.propietario = propietario;
        this.servicio = servicio;
        this.alGuardar = alGuardar;
        crearDialogo();
        cargarConfiguracion(servicio.configuracion());
    }

    void showAndWait() {
        dialogo.showAndWait();
    }

    private void crearDialogo() {
        dialogo.initOwner(propietario);
        dialogo.setTitle("Configuración");
        dialogo.getDialogPane().getStyleClass().add("config-dialog");
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        dialogo.getDialogPane().setPrefSize(960, 700);
        ButtonType guardar = new ButtonType("Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        TabPane tabs = new TabPane();
        Tab general = new Tab("General", crearGeneral());
        general.setClosable(false);
        Tab clientes = new Tab("Clientes", crearClientes());
        clientes.setClosable(false);
        tabs.getTabs().addAll(general, clientes);
        dialogo.getDialogPane().setContent(tabs);
        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.getStyleClass().add("primary-button");
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                Configuracion propuesta = construirConfiguracion();
                servicio.validar(propuesta);
                manejarCambioProteccion();
                Configuracion configuracionFinal = propuesta.conAcceso(servicio.configuracion().acceso());
                servicio.guardar(configuracionFinal);
                alGuardar.accept(servicio.configuracion());
            } catch (RuntimeException e) {
                mostrarError("No se pudo guardar la configuración", e.getMessage());
                evento.consume();
            }
        });
        dialogo.setResultConverter(boton -> boton == guardar ? servicio.configuracion() : null);
        descripcion.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= ParametrosTramite.DESCRIPCION_MAXIMA ? cambio : null));
        descripcion.textProperty().addListener((observable, antes, ahora) ->
                contadorDescripcion.setText(ahora.length() + " / 500 caracteres"));
        restringirHorario.selectedProperty().addListener((observable, antes, ahora) -> {
            boolean habilitado = ahora;
            dias.values().forEach(check -> check.setDisable(!habilitado));
            desde.setDisable(!habilitado);
            hasta.setDisable(!habilitado);
        });
    }

    private Node crearGeneral() {
        GridPane formulario = new GridPane();
        formulario.setHgap(14);
        formulario.setVgap(16);
        formulario.setPadding(new Insets(24));
        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(200);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setHgrow(Priority.ALWAYS);
        formulario.getColumnConstraints().addAll(etiquetas, valores);

        navegador.setItems(FXCollections.observableArrayList(NavegadorPreferido.values()));
        addRow(formulario, 0, "Navegador", navegador);
        intervalo.setEditable(true);
        addRow(formulario, 1, "Intervalo entre consultas (segundos)", intervalo);
        formulario.add(restringirHorario, 0, 2, 2, 1);
        HBox diasFila = new HBox(8);
        for (DayOfWeek dia : DayOfWeek.values()) {
            CheckBox check = new CheckBox(DIAS.get(dia));
            dias.put(dia, check);
            diasFila.getChildren().add(check);
        }
        addRow(formulario, 3, "Días permitidos", diasFila);
        HBox horas = new HBox(10, desde, new Label("a"), hasta,
                new Label("Hora local de Colombia (America/Bogota)"));
        desde.setPromptText("08:00");
        hasta.setPromptText("16:00");
        addRow(formulario, 4, "Horario (24 horas)", horas);
        formulario.add(new Label("El horario viene desactivado por defecto; actívalo solo si deseas limitar las consultas."),
                0, 5, 2, 1);
        formulario.add(proteger, 0, 6, 2, 1);
        Button cambiar = new Button("Cambiar contraseña");
        cambiar.getStyleClass().add("secondary-button");
        cambiar.setOnAction(event -> cambiarProteccion());
        formulario.add(cambiar, 0, 7);
        VBox contenedor = new VBox(formulario);
        contenedor.getStyleClass().add("settings-page");
        return contenedor;
    }

    private Node crearClientes() {
        listaClientes.setPrefWidth(225);
        listaClientes.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(Cliente cliente, boolean vacio) {
                super.updateItem(cliente, vacio);
                setText(vacio || cliente == null ? null
                        : (cliente.nombre().isBlank() ? "Nuevo cliente" : cliente.nombre()));
            }
        });
        listaClientes.getSelectionModel().selectedItemProperty().addListener((observable, anterior, nuevo) -> {
            if (anterior != null && clienteActualId != null) {
                guardarBorradorActual();
            }
            if (nuevo != null) {
                clienteActualId = nuevo.id();
                cargarCliente(borradores.getOrDefault(nuevo.id(), nuevo));
            }
        });
        Button agregar = new Button("+ Agregar cliente");
        agregar.getStyleClass().add("secondary-button");
        agregar.setMaxWidth(Double.MAX_VALUE);
        agregar.setOnAction(event -> agregarCliente());
        Button eliminar = new Button("Eliminar seleccionado");
        eliminar.getStyleClass().add("text-button");
        eliminar.setMaxWidth(Double.MAX_VALUE);
        eliminar.setOnAction(event -> eliminarCliente());
        VBox columnaLista = new VBox(10, agregar, listaClientes, eliminar);
        VBox.setVgrow(listaClientes, Priority.ALWAYS);
        columnaLista.setPadding(new Insets(18));
        columnaLista.setPrefWidth(250);
        columnaLista.getStyleClass().add("client-list");

        VBox editor = new VBox(13);
        editor.setPadding(new Insets(22));
        Label encabezado = new Label("Perfil del cliente");
        encabezado.getStyleClass().add("section-title");
        editor.getChildren().addAll(encabezado, campo("Nombre para mostrar", nombre), activo,
                seccion("Paso 1 · Datos del trámite"),
                campo("Cómo obtener el permiso (texto del portal)", permiso),
                campo("Tipo de trámite (texto del portal)", tipoTramite),
                campo("Seccionales preferidas", seccionales),
                descripcionConContador(),
                seccion("Selección del arma"),
                campo("Reacción", modo), campo("Modalidad", modalidad),
                campo("Objetivos prioritarios", objetivos), campo("Si no hay objetivo", politica),
                campo("Alternativas", alternativas), campo("Tipo comodín", tipoComodin),
                campo("Excluir", excluir), campo("Reintentos al apartar", reintentos));
        ScrollPaneFormulario scroll = new ScrollPaneFormulario(editor);
        BorderPane cuerpo = new BorderPane(scroll);
        cuerpo.setLeft(columnaLista);
        cuerpo.getStyleClass().add("client-editor");
        HBox.setHgrow(scroll, Priority.ALWAYS);
        HBox.setHgrow(editor, Priority.ALWAYS);
        return cuerpo;
    }

    private void cargarConfiguracion(Configuracion configuracion) {
        navegador.setValue(configuracion.navegador());
        intervalo.getValueFactory().setValue(configuracion.intervaloConsultaSegundos());
        HorarioOperacion horario = configuracion.horario();
        restringirHorario.setSelected(horario.restringir());
        horario.dias().forEach(dia -> dias.get(dia).setSelected(true));
        desde.setText(horario.desde().toString());
        hasta.setText(horario.hasta().toString());
        proteger.setSelected(configuracion.acceso().requerirContrasena());
        borradores.clear();
        configuracion.clientes().forEach(cliente -> borradores.put(cliente.id(), cliente));
        listaClientes.setItems(FXCollections.observableArrayList(configuracion.clientes()));
        if (!configuracion.clientes().isEmpty()) {
            listaClientes.getSelectionModel().selectFirst();
        } else {
            clienteActualId = null;
            cargarCliente(Cliente.nuevo(""));
        }
    }

    private Configuracion construirConfiguracion() {
        guardarBorradorActual();
        Configuracion base = servicio.configuracion();
        int segundos;
        try {
            segundos = Integer.parseInt(intervalo.getEditor().getText().strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El intervalo debe ser un número entero de segundos.", e);
        }
        if (segundos < Configuracion.INTERVALO_MINIMO_SEGUNDOS || segundos > 3600) {
            throw new IllegalArgumentException("El intervalo debe estar entre 2 y 3600 segundos.");
        }
        HorarioOperacion horario = new HorarioOperacion(restringirHorario.isSelected(),
                dias.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList(),
                LocalTime.parse(desde.getText().strip()), LocalTime.parse(hasta.getText().strip()),
                HorarioOperacion.ZONA_COLOMBIA);
        return new Configuracion(base.version(), base.acceso(), navegador.getValue(),
                segundos, horario, new ArrayList<>(borradores.values()));
    }

    private void manejarCambioProteccion() {
        boolean estabaActiva = servicio.configuracion().acceso().requerirContrasena();
        if (proteger.isSelected() == estabaActiva) {
            return;
        }
        if (proteger.isSelected()) {
            var nuevas = DialogosContrasena.pedirNueva(propietario, "Activar contraseña");
            if (nuevas.isEmpty()) {
                throw new IllegalArgumentException("Se canceló la activación de la contraseña.");
            }
            char[][] valores = nuevas.get();
            try {
                servicio.crearContrasena(valores[0], valores[1]);
            } finally {
                DialogosContrasena.borrar(valores[0]);
                DialogosContrasena.borrar(valores[1]);
            }
        } else {
            var actual = DialogosContrasena.pedirActual(propietario, "Desactivar protección");
            if (actual.isEmpty()) {
                throw new IllegalArgumentException("Se canceló la desactivación de la protección.");
            }
            try {
                servicio.desactivarProteccion(actual.get());
            } catch (ContrasenaRechazadaException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    private void cambiarProteccion() {
        if (servicio.pideContrasena()) {
            var actual = DialogosContrasena.pedirActual(propietario, "Cambiar contraseña");
            if (actual.isEmpty()) {
                return;
            }
            var nuevas = DialogosContrasena.pedirNueva(propietario, "Cambiar contraseña");
            if (nuevas.isEmpty()) {
                DialogosContrasena.borrar(actual.get());
                return;
            }
            char[][] valores = nuevas.get();
            try {
                servicio.cambiarContrasena(actual.get(), valores[0], valores[1]);
                proteger.setSelected(true);
                mostrarInfo("Contraseña actualizada", "La contraseña de la aplicación se cambió.");
            } catch (RuntimeException e) {
                mostrarError("No se pudo cambiar la contraseña", e.getMessage());
            } finally {
                DialogosContrasena.borrar(actual.get());
                DialogosContrasena.borrar(valores[0]);
                DialogosContrasena.borrar(valores[1]);
            }
        } else {
            var nuevas = DialogosContrasena.pedirNueva(propietario, "Crear contraseña");
            if (nuevas.isEmpty()) {
                return;
            }
            char[][] valores = nuevas.get();
            try {
                servicio.crearContrasena(valores[0], valores[1]);
                proteger.setSelected(true);
                mostrarInfo("Protección activada", "La configuración ahora requiere contraseña.");
            } catch (RuntimeException e) {
                mostrarError("No se pudo crear la contraseña", e.getMessage());
            } finally {
                DialogosContrasena.borrar(valores[0]);
                DialogosContrasena.borrar(valores[1]);
            }
        }
    }

    private void agregarCliente() {
        guardarBorradorActual();
        Cliente nuevo = Cliente.nuevo("");
        borradores.put(nuevo.id(), nuevo);
        listaClientes.getItems().add(nuevo);
        listaClientes.getSelectionModel().select(nuevo);
        nombre.requestFocus();
    }

    private void eliminarCliente() {
        Cliente seleccionado = listaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el perfil de " + seleccionado.nombre() + "?");
        confirmacion.initOwner(propietario);
        confirmacion.setHeaderText("Eliminar cliente");
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        borradores.remove(seleccionado.id());
        listaClientes.getItems().remove(seleccionado);
        if (listaClientes.getItems().isEmpty()) {
            clienteActualId = null;
            cargarCliente(Cliente.nuevo(""));
        }
    }

    private void guardarBorradorActual() {
        if (clienteActualId == null) {
            return;
        }
        try {
            Cliente cliente = capturarCliente(borradores.get(clienteActualId));
            borradores.put(cliente.id(), cliente);
            listaClientes.refresh();
        } catch (RuntimeException e) {
            mostrarError("Revisa los datos del cliente", e.getMessage());
        }
    }

    private void cargarCliente(Cliente cliente) {
        nombre.setText(cliente.nombre());
        activo.setSelected(cliente.activo());
        permiso.setText(cliente.tramite().obtenerPermiso());
        tipoTramite.setText(cliente.tramite().tipoTramite());
        seccionales.setText(String.join("\n", cliente.tramite().seccionales()));
        descripcion.setText(cliente.tramite().descripcion());
        modo.setItems(FXCollections.observableArrayList(ModoReaccion.values()));
        modo.setValue(cliente.modo());
        modalidad.setItems(FXCollections.observableArrayList(Modalidad.values()));
        modalidad.setValue(cliente.seleccion().modalidad());
        objetivos.setText(formatearCriterios(cliente.seleccion().objetivos()));
        politica.setItems(FXCollections.observableArrayList(PoliticaSinObjetivo.values()));
        politica.setValue(cliente.seleccion().siNoHayObjetivo());
        alternativas.setText(formatearCriterios(cliente.seleccion().alternativas()));
        tipoComodin.setText(cliente.seleccion().tipoComodin());
        excluir.setText(String.join("\n", cliente.seleccion().excluir()));
        reintentos.getValueFactory().setValue(Math.min(5, cliente.seleccion().reintentos()));
    }

    private Cliente capturarCliente(Cliente base) {
        Cliente anterior = base == null ? Cliente.nuevo("") : base;
        ParametrosTramite tramite = new ParametrosTramite(
                permiso.getText(), tipoTramite.getText(), lineas(seccionales.getText()), descripcion.getText());
        ReglasSeleccion reglas = new ReglasSeleccion(valor(modalidad.getValue()),
                criterios(objetivos.getText()), valor(politica.getValue()), criterios(alternativas.getText()),
                tipoComodin.getText(), lineas(excluir.getText()), reintentos.getValue());
        return new Cliente(anterior.id(), nombre.getText(), activo.isSelected(), tramite,
                valor(modo.getValue()), reglas);
    }

    private static Modalidad valor(Modalidad valor) {
        return valor == null ? Modalidad.PORTE : valor;
    }

    private static ModoReaccion valor(ModoReaccion valor) {
        return valor == null ? ModoReaccion.APARTAR : valor;
    }

    private static PoliticaSinObjetivo valor(PoliticaSinObjetivo valor) {
        return valor == null ? PoliticaSinObjetivo.SEGUIR_ESPERANDO : valor;
    }

    private static List<String> lineas(String texto) {
        return texto.lines().map(String::strip).filter(linea -> !linea.isEmpty()).toList();
    }

    private static List<CriterioArma> criterios(String texto) {
        return lineas(texto).stream().map(linea -> new CriterioArma(
                java.util.Arrays.stream(linea.split(",")).map(String::strip).toList())).toList();
    }

    private static String formatearCriterios(List<CriterioArma> criterios) {
        return criterios.stream().map(criterio -> String.join(", ", criterio.palabrasClave()))
                .collect(Collectors.joining("\n"));
    }

    private Node descripcionConContador() {
        VBox caja = new VBox(5, descripcion, contadorDescripcion);
        contadorDescripcion.setText("0 / 500 caracteres");
        return campo("Descripción", caja);
    }

    private static VBox campo(String etiqueta, Node control) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("field-label");
        VBox grupo = new VBox(5, titulo, control);
        if (control instanceof TextArea texto) {
            texto.setPrefRowCount(3);
            VBox.setVgrow(texto, Priority.NEVER);
        }
        return grupo;
    }

    private static Label seccion(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("section-title");
        return etiqueta;
    }

    private static TextArea area(String ayuda) {
        TextArea area = new TextArea();
        area.setPromptText(ayuda);
        area.setWrapText(true);
        area.setPrefRowCount(3);
        return area;
    }

    private static void addRow(GridPane grid, int row, String etiqueta, Node control) {
        grid.add(new Label(etiqueta), 0, row);
        grid.add(control, 1, row);
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert error = new Alert(Alert.AlertType.ERROR, mensaje == null ? "Error inesperado." : mensaje);
        error.initOwner(propietario);
        EstilosJavaFx.aplicar(error.getDialogPane());
        error.setTitle(titulo);
        error.setHeaderText(titulo);
        error.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert info = new Alert(Alert.AlertType.INFORMATION, mensaje);
        info.initOwner(propietario);
        EstilosJavaFx.aplicar(info.getDialogPane());
        info.setTitle(titulo);
        info.setHeaderText(titulo);
        info.showAndWait();
    }

    private static final class ScrollPaneFormulario extends javafx.scene.control.ScrollPane {
        private ScrollPaneFormulario(Node contenido) {
            setContent(contenido);
            setFitToWidth(true);
            getStyleClass().add("transparent-scroll");
        }
    }
}
