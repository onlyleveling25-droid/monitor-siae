package co.monitorsiae.ui;

import co.monitorsiae.acceso.ContrasenaRechazadaException;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.EntornoPortal;
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
    private final ComboBox<EntornoPortal> entornoPortal = new ComboBox<>();
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
    private final ListaEditable seccionales = new ListaEditable(
            "Escribe una seccional y agrégala. Puedes cambiar el orden con las flechas.", "Ej. Medellín");
    private final TextArea descripcion = area("Descripción del trámite (150–500 caracteres)");
    private final Label contadorDescripcion = new Label();
    private final ComboBox<ModoReaccion> modo = new ComboBox<>();
    private final ComboBox<Modalidad> modalidad = new ComboBox<>();
    private final ListaEditable objetivos = new ListaEditable(
            "Cada fila es un objetivo prioritario. Separa con coma las palabras que deben coincidir juntas.",
            "Ej. glock, g-43");
    private final ComboBox<PoliticaSinObjetivo> politica = new ComboBox<>();
    private final ListaEditable alternativas = new ListaEditable(
            "Opcional. Cada fila es una alternativa; se revisan en el orden de la lista.",
            "Ej. cz, p10");
    private final TextField tipoComodin = new TextField("Pistola");
    private final ListaEditable excluir = new ListaEditable(
            "Si una palabra aparece en el arma, esta se descarta aunque coincida con un objetivo.",
            "Ej. 22 lr");
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
        entornoPortal.setItems(FXCollections.observableArrayList(EntornoPortal.values()));
        entornoPortal.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(EntornoPortal entorno, boolean vacio) {
                super.updateItem(entorno, vacio);
                setText(vacio || entorno == null ? null : textoEntorno(entorno));
            }
        });
        entornoPortal.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EntornoPortal entorno, boolean vacio) {
                super.updateItem(entorno, vacio);
                setText(vacio || entorno == null ? null : textoEntorno(entorno));
            }
        });
        addRow(formulario, 1, "Entorno del portal", entornoPortal);
        Label notaEntorno = new Label("El portal real solo abre la sesión del navegador por ahora; "
                + "la automatización se habilitará tras validar sus selectores.");
        notaEntorno.setWrapText(true);
        notaEntorno.getStyleClass().add("muted");
        formulario.add(notaEntorno, 1, 2);
        intervalo.setEditable(true);
        addRow(formulario, 3, "Intervalo entre consultas (segundos)", intervalo);
        formulario.add(restringirHorario, 0, 4, 2, 1);
        HBox diasFila = new HBox(8);
        for (DayOfWeek dia : DayOfWeek.values()) {
            CheckBox check = new CheckBox(DIAS.get(dia));
            dias.put(dia, check);
            diasFila.getChildren().add(check);
        }
        addRow(formulario, 5, "Días permitidos", diasFila);
        HBox horas = new HBox(10, desde, new Label("a"), hasta,
                new Label("Hora local de Colombia (America/Bogota)"));
        desde.setPromptText("08:00");
        hasta.setPromptText("16:00");
        addRow(formulario, 6, "Horario (24 horas)", horas);
        formulario.add(new Label("El horario viene desactivado por defecto; actívalo solo si deseas limitar las consultas."),
                0, 7, 2, 1);
        formulario.add(proteger, 0, 8, 2, 1);
        Button cambiar = new Button("Cambiar contraseña");
        cambiar.getStyleClass().add("secondary-button");
        cambiar.setOnAction(event -> cambiarProteccion());
        formulario.add(cambiar, 0, 9);
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
                campo("Seccionales preferidas (de mayor a menor prioridad)", seccionales),
                descripcionConContador(),
                seccion("Selección del arma"),
                campo("Reacción", modo), campo("Modalidad", modalidad),
                campo("Objetivos prioritarios", objetivos), campo("Si no aparece ningún objetivo", politica),
                campo("Alternativas", alternativas), campo("Tipo comodín (último recurso)", tipoComodin),
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
        entornoPortal.setValue(configuracion.entornoPortal());
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
        return new Configuracion(base.version(), base.acceso(), navegador.getValue(), entornoPortal.getValue(),
                segundos, horario, new ArrayList<>(borradores.values()));
    }

    private static String textoEntorno(EntornoPortal entorno) {
        return switch (entorno) {
            case SIMULADOR -> "Simulador (pruebas)";
            case PORTAL_REAL -> "Portal real (sesión manual)";
        };
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
        guardarBorradorActual();
        clienteActualId = null;
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
        Cliente cliente = capturarCliente(borradores.get(clienteActualId));
        borradores.put(cliente.id(), cliente);
        listaClientes.refresh();
    }

    private void cargarCliente(Cliente cliente) {
        nombre.setText(cliente.nombre());
        activo.setSelected(cliente.activo());
        permiso.setText(cliente.tramite().obtenerPermiso());
        tipoTramite.setText(cliente.tramite().tipoTramite());
        seccionales.setValues(cliente.tramite().seccionales());
        descripcion.setText(cliente.tramite().descripcion());
        modo.setItems(FXCollections.observableArrayList(ModoReaccion.values()));
        modo.setValue(cliente.modo());
        modalidad.setItems(FXCollections.observableArrayList(Modalidad.values()));
        modalidad.setValue(cliente.seleccion().modalidad());
        objetivos.setValues(formatearCriterios(cliente.seleccion().objetivos()));
        politica.setItems(FXCollections.observableArrayList(PoliticaSinObjetivo.values()));
        politica.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(PoliticaSinObjetivo item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : textoPolitica(item));
            }
        });
        politica.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PoliticaSinObjetivo item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : textoPolitica(item));
            }
        });
        politica.setValue(cliente.seleccion().siNoHayObjetivo());
        alternativas.setValues(formatearCriterios(cliente.seleccion().alternativas()));
        tipoComodin.setText(cliente.seleccion().tipoComodin());
        excluir.setValues(cliente.seleccion().excluir());
        reintentos.getValueFactory().setValue(Math.min(5, cliente.seleccion().reintentos()));
    }

    private Cliente capturarCliente(Cliente base) {
        Cliente anterior = base == null ? Cliente.nuevo("") : base;
        ParametrosTramite tramite = new ParametrosTramite(
                permiso.getText(), tipoTramite.getText(), seccionales.values(), descripcion.getText());
        ReglasSeleccion reglas = new ReglasSeleccion(valor(modalidad.getValue()),
                criterios(objetivos.values()), valor(politica.getValue()), criterios(alternativas.values()),
                tipoComodin.getText(), excluir.values(), reintentos.getValue());
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

    private static List<CriterioArma> criterios(List<String> valores) {
        return valores.stream().map(linea -> new CriterioArma(
                java.util.Arrays.stream(linea.split(",")).map(String::strip).toList())).toList();
    }

    private static List<String> formatearCriterios(List<CriterioArma> criterios) {
        return criterios.stream().map(criterio -> String.join(", ", criterio.palabrasClave()))
                .toList();
    }

    private static String textoPolitica(PoliticaSinObjetivo politica) {
        return switch (politica) {
            case SEGUIR_ESPERANDO -> "Seguir esperando solo los objetivos";
            case USAR_ALTERNATIVAS -> "Probar alternativas; si ninguna coincide, seguir esperando";
            case CUALQUIERA_DEL_TIPO -> "Tomar la primera arma del tipo indicado";
            case USAR_ALTERNATIVAS_LUEGO_TIPO ->
                    "Probar alternativas y, si ninguna coincide, tomar la primera del tipo";
        };
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

    private static final class ListaEditable extends VBox {
        private final javafx.collections.ObservableList<String> valores =
                FXCollections.observableArrayList();
        private final ListView<String> lista = new ListView<>(valores);
        private final TextField entrada = new TextField();

        private ListaEditable(String ayuda, String ejemplo) {
            super(7);
            Label nota = new Label(ayuda);
            nota.getStyleClass().add("muted");
            nota.setWrapText(true);
            entrada.setPromptText(ejemplo);
            Button agregar = new Button("Agregar");
            agregar.getStyleClass().add("secondary-button");
            agregar.setOnAction(event -> agregar());
            entrada.setOnAction(event -> agregar());
            HBox filaAgregar = new HBox(8, entrada, agregar);
            HBox.setHgrow(entrada, Priority.ALWAYS);
            lista.setPrefHeight(90);
            lista.setPlaceholder(new Label("Todavía no hay elementos."));
            Button quitar = new Button("Quitar");
            quitar.getStyleClass().add("text-button");
            quitar.setOnAction(event -> {
                int indice = lista.getSelectionModel().getSelectedIndex();
                if (indice >= 0) {
                    valores.remove(indice);
                }
            });
            Button subir = new Button("↑");
            subir.setTooltip(new javafx.scene.control.Tooltip("Subir prioridad"));
            subir.getStyleClass().add("secondary-button");
            subir.setOnAction(event -> mover(-1));
            Button bajar = new Button("↓");
            bajar.setTooltip(new javafx.scene.control.Tooltip("Bajar prioridad"));
            bajar.getStyleClass().add("secondary-button");
            bajar.setOnAction(event -> mover(1));
            HBox acciones = new HBox(7, subir, bajar, quitar);
            getChildren().addAll(nota, filaAgregar, lista, acciones);
        }

        private void agregar() {
            String valor = entrada.getText().strip();
            if (valor.isEmpty()) {
                return;
            }
            if (valores.stream().anyMatch(actual -> actual.equalsIgnoreCase(valor))) {
                return;
            }
            valores.add(valor);
            lista.getSelectionModel().select(valores.size() - 1);
            entrada.clear();
            entrada.requestFocus();
        }

        private void mover(int desplazamiento) {
            int indice = lista.getSelectionModel().getSelectedIndex();
            int destino = indice + desplazamiento;
            if (indice < 0 || destino < 0 || destino >= valores.size()) {
                return;
            }
            String valor = valores.remove(indice);
            valores.add(destino, valor);
            lista.getSelectionModel().select(destino);
        }

        private List<String> values() {
            return List.copyOf(valores);
        }

        private void setValues(List<String> nuevos) {
            valores.setAll(nuevos);
        }
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
