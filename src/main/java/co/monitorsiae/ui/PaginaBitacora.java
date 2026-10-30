package co.monitorsiae.ui;

import co.monitorsiae.app.ExportadorBitacoraCsv;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.monitoreo.EventoOperativo;
import co.monitorsiae.monitoreo.FiltroBitacora;
import co.monitorsiae.monitoreo.ResumenBitacora;
import co.monitorsiae.monitoreo.TiempoRelativo;
import co.monitorsiae.monitoreo.TipoEvento;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Eventos operativos por cliente, tipo, periodo y texto. No cambia el monitoreo; solo puede vaciar la bitácora. */
final class PaginaBitacora extends VBox {

    private static final java.time.Duration PERIODO_RESUMEN = java.time.Duration.ofHours(24);

    private final Supplier<List<EventoOperativo>> eventos;
    private final Supplier<List<Cliente>> clientes;
    private final Consumer<EventoOperativo> verInventario;
    private final Path carpetaCapturas;
    private final Consumer<Path> abrirArchivo;
    private final java.util.function.IntSupplier vaciar;
    private final Clock reloj = Clock.systemUTC();
    private final ZoneId zona = ZoneId.systemDefault();
    private final ComboBox<Cliente> filtroCliente = new ComboBox<>();
    private final ComboBox<TipoEvento> filtroTipo = new ComboBox<>();
    private final ComboBox<java.time.Duration> filtroPeriodo = new ComboBox<>();
    private final TextField busqueda = new TextField();
    private final Button quitarFiltros = new Button("Quitar filtros");
    private final TableView<EventoOperativo> tabla = new TableView<>();
    private final Label cuenta = new Label();
    private final Label aviso = new Label();
    private final Label oportunidades = new Label("0");
    private final Label apartadas = new Label("0");
    private final Label problemas = new Label("0");
    private List<EventoOperativo> visibles = List.of();
    /** Evita que cambiar la lista del selector (que borra su valor) vuelva a disparar la actualización. */
    private boolean actualizando;

    PaginaBitacora(Supplier<List<EventoOperativo>> eventos, Supplier<List<Cliente>> clientes,
                   Consumer<EventoOperativo> verInventario, Path carpetaCapturas, Consumer<Path> abrirArchivo,
                   java.util.function.IntSupplier vaciar) {
        super(16);
        this.vaciar = vaciar;
        this.carpetaCapturas = carpetaCapturas;
        this.abrirArchivo = abrirArchivo;
        this.eventos = eventos;
        this.clientes = clientes;
        this.verInventario = verInventario;
        setPadding(new Insets(28, 32, 28, 32));
        Label titulo = new Label("Bitácora");
        titulo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Hechos relevantes de cada cliente. Los ciclos sin novedad no se registran; "
                + "se conservan los últimos 30 días. Haz doble clic en un evento para ver su detalle.");
        subtitulo.getStyleClass().add("muted");
        subtitulo.setWrapText(true);
        VBox titulos = new VBox(4, titulo, subtitulo);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        Button exportar = new Button("Exportar CSV");
        exportar.getStyleClass().add("secondary-button");
        exportar.setTooltip(new Tooltip("Guarda los eventos que ves ahora (con los filtros) en un archivo para Excel"));
        exportar.setOnAction(e -> exportar());
        Button vaciarBitacora = new Button("Vaciar bitácora");
        vaciarBitacora.getStyleClass().add("danger-button");
        vaciarBitacora.setGraphic(IconoSvg.crear("close", 16, "#B91C1C"));
        vaciarBitacora.setTooltip(new Tooltip("Borra todos los eventos para liberar espacio; pide confirmación"));
        vaciarBitacora.setOnAction(e -> vaciarConConfirmacion());
        HBox encabezado = new HBox(14, titulos, exportar, vaciarBitacora);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.getStyleClass().add("page-header");

        HBox resumen = new HBox(12,
                tarjeta("Oportunidades · 24 h", oportunidades, "bell", "#EA580C"),
                tarjeta("Armas apartadas · 24 h", apartadas, "check", "#15803D"),
                tarjeta("Problemas · 24 h", problemas, "alert", "#B91C1C"));

        configurarFiltros();
        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("secondary-button");
        actualizar.setOnAction(e -> actualizar());
        quitarFiltros.getStyleClass().add("text-button");
        quitarFiltros.setTooltip(new Tooltip("Vuelve a mostrar todos los eventos"));
        quitarFiltros.setOnAction(e -> {
            filtroCliente.setValue(null);
            filtroTipo.setValue(null);
            filtroPeriodo.setValue(null);
            busqueda.clear();
        });
        HBox cajaBusqueda = CajaBusqueda.envolver(busqueda);
        HBox filtros = new HBox(10, cajaBusqueda, filtroCliente, filtroTipo, filtroPeriodo, quitarFiltros, actualizar);
        filtros.setAlignment(Pos.CENTER_LEFT);

        aviso.getStyleClass().add("validation-banner");
        aviso.setWrapText(true);
        ocultarAviso();
        cuenta.getStyleClass().add("muted");
        configurarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(encabezado, resumen, filtros, aviso, tabla, cuenta);
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            if (getScene() != null) {
                tabla.refresh();
            }
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        actualizar();
    }

    /** Vuelve a leer los clientes y los eventos; lo llama la navegación al mostrar la página. */
    void actualizar() {
        if (actualizando) {
            return;
        }
        actualizando = true;
        try {
            refrescar();
        } finally {
            actualizando = false;
        }
    }

    private void refrescar() {
        Cliente elegido = filtroCliente.getValue();
        List<Cliente> lista = clientes.get();
        if (!lista.equals(filtroCliente.getItems())) {
            filtroCliente.getItems().setAll(lista);
        }
        filtroCliente.setValue(elegido == null ? null
                : lista.stream().filter(c -> c.id().equals(elegido.id())).findFirst().orElse(null));
        try {
            List<EventoOperativo> todos = eventos.get();
            FiltroBitacora filtro = new FiltroBitacora(
                    filtroCliente.getValue() == null ? null : filtroCliente.getValue().id(),
                    filtroTipo.getValue(), filtroPeriodo.getValue(), busqueda.getText());
            visibles = filtro.aplicar(todos, reloj);
            tabla.getItems().setAll(visibles);
            quitarFiltros.setDisable(!filtro.activo());
            cuenta.setText(filtro.activo()
                    ? visibles.size() + " de " + todos.size() + " evento(s) con los filtros actuales"
                    : todos.size() + " evento(s) en total");
            ResumenBitacora conteo = ResumenBitacora.de(todos, reloj, PERIODO_RESUMEN);
            oportunidades.setText(String.valueOf(conteo.oportunidades()));
            apartadas.setText(String.valueOf(conteo.apartadas()));
            problemas.setText(String.valueOf(conteo.problemas()));
            ocultarAviso();
        } catch (RuntimeException e) {
            visibles = List.of();
            tabla.getItems().clear();
            cuenta.setText("");
            aviso.setText("No se pudo leer la bitácora: " + e.getMessage()
                    + " El monitoreo no se ve afectado.");
            aviso.setVisible(true);
            aviso.setManaged(true);
        }
    }

    private static VBox tarjeta(String titulo, Label valor, String icono, String color) {
        HBox encabezado = new HBox(8, IconoSvg.crear(icono, 16, color), new Label(titulo));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.getStyleClass().add("metric-heading");
        valor.getStyleClass().add("metric-value");
        VBox tarjeta = new VBox(8, encabezado, valor);
        tarjeta.getStyleClass().add("metric-card");
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    private void configurarFiltros() {
        busqueda.setPromptText("Buscar: glock, bogotá o medellín…");
        busqueda.setTooltip(new Tooltip("Busca en cliente, tipo, detalle, seccional y arma. Usa \"o\" o coma para ver varias opciones."));
        busqueda.setPrefColumnCount(18);
        busqueda.textProperty().addListener((obs, antes, ahora) -> actualizar());
        filtroCliente.setPromptText("Todos los clientes");
        filtroCliente.setConverter(convertidor(Cliente::nombre));
        filtroTipo.setPromptText("Todos los tipos");
        filtroTipo.getItems().setAll(TipoEvento.values());
        filtroTipo.setConverter(convertidor(TipoEvento::etiqueta));
        filtroPeriodo.setPromptText("Todo el periodo");
        filtroPeriodo.getItems().setAll(java.time.Duration.ofHours(1), java.time.Duration.ofDays(1),
                java.time.Duration.ofDays(7));
        filtroPeriodo.setConverter(convertidor(PaginaBitacora::etiquetaPeriodo));
        for (ComboBox<?> combo : List.of(filtroCliente, filtroTipo, filtroPeriodo)) {
            combo.valueProperty().addListener((obs, antes, ahora) -> actualizar());
            combo.setMinWidth(170);
        }
    }

    private void configurarTabla() {
        tabla.getStyleClass().add("client-list");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("No hay eventos con estos filtros."));
        TableColumn<EventoOperativo, EventoOperativo> hora = new TableColumn<>("Hora");
        hora.setPrefWidth(130);
        hora.setSortable(false);
        hora.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue()));
        hora.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(EventoOperativo evento, boolean vacio) {
                super.updateItem(evento, vacio);
                if (vacio || evento == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(TiempoRelativo.texto(evento.momento(), reloj, zona));
                setTooltip(new Tooltip(TiempoRelativo.completo(evento.momento(), zona)));
            }
        });
        TableColumn<EventoOperativo, EventoOperativo> tipo = new TableColumn<>("Tipo");
        tipo.setPrefWidth(190);
        tipo.setSortable(false);
        tipo.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue()));
        tipo.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(EventoOperativo evento, boolean vacio) {
                super.updateItem(evento, vacio);
                setGraphic(vacio || evento == null ? null : etiquetaTipo(evento.tipo()));
                setText(null);
            }
        });
        tabla.getColumns().add(hora);
        tabla.getColumns().add(texto("Cliente", 150, EventoOperativo::clienteNombre));
        tabla.getColumns().add(tipo);
        tabla.getColumns().add(texto("Seccional", 140, EventoOperativo::seccional));
        TableColumn<EventoOperativo, EventoOperativo> detalle = new TableColumn<>("Detalle");
        detalle.setPrefWidth(420);
        detalle.setSortable(false);
        detalle.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue()));
        detalle.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(EventoOperativo evento, boolean vacio) {
                super.updateItem(evento, vacio);
                boolean hayEvento = !vacio && evento != null;
                setText(hayEvento ? evento.detalle() : null);
                setGraphic(hayEvento && !evento.captura().isEmpty()
                        ? IconoSvg.crear("image", 14, "#64748B") : null);
                setTooltip(hayEvento && !evento.captura().isEmpty()
                        ? new Tooltip("Tiene captura de pantalla: haz doble clic para verla") : null);
                setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                setGraphicTextGap(8);
            }
        });
        tabla.getColumns().add(detalle);
        tabla.setRowFactory(t -> {
            TableRow<EventoOperativo> fila = new TableRow<>() {
                @Override
                protected void updateItem(EventoOperativo evento, boolean vacio) {
                    super.updateItem(evento, vacio);
                    getStyleClass().remove("bitacora-importante");
                    if (!vacio && evento != null && evento.tipo().importante()) {
                        getStyleClass().add("bitacora-importante");
                    }
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    mostrarDetalle(fila.getItem());
                }
            });
            return fila;
        });
        tabla.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && tabla.getSelectionModel().getSelectedItem() != null) {
                mostrarDetalle(tabla.getSelectionModel().getSelectedItem());
            }
        });
    }

    private static TableColumn<EventoOperativo, String> texto(String titulo, double ancho,
            Function<EventoOperativo, String> valor) {
        TableColumn<EventoOperativo, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setCellValueFactory(celda -> new SimpleStringProperty(valor.apply(celda.getValue())));
        return columna;
    }

    /** Etiqueta de color e icono por tipo de evento. */
    private static Label etiquetaTipo(TipoEvento tipo) {
        String clase;
        String icono;
        String color;
        switch (tipo) {
            case OPORTUNIDAD -> { clase = "status-alert"; icono = "bell"; color = "#C2410C"; }
            case ARMA_APARTADA -> { clase = "status-success"; icono = "check"; color = "#15803D"; }
            case INVENTARIO_SIN_INTERES -> { clase = "status-waiting"; icono = "info"; color = "#B45309"; }
            case APARTADO_FALLIDO, SESION_VENCIDA -> { clase = "status-error"; icono = "alert"; color = "#B91C1C"; }
            case RESPUESTA_INESPERADA, PORTAL_SIN_RESPUESTA -> { clase = "status-waiting"; icono = "diagnostics"; color = "#B45309"; }
            case MONITOREO_INICIADO -> { clase = "status-running"; icono = "activity"; color = "#1D4ED8"; }
            default -> { clase = "status-ready"; icono = "info"; color = "#64748B"; }
        }
        Label etiqueta = new Label(tipo.etiqueta(), IconoSvg.crear(icono, 13, color));
        etiqueta.setGraphicTextGap(6);
        etiqueta.getStyleClass().addAll("status-pill", clase);
        return etiqueta;
    }

    private void mostrarDetalle(EventoOperativo evento) {
        Alert dialogo = new Alert(Alert.AlertType.NONE);
        if (getScene() != null) {
            dialogo.initOwner(getScene().getWindow());
        }
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        dialogo.setTitle("Detalle del evento");
        GridPane datos = new GridPane();
        datos.setHgap(14);
        datos.setVgap(8);
        int fila = 0;
        fila = dato(datos, fila, "Tipo", null, etiquetaTipo(evento.tipo()));
        fila = dato(datos, fila, "Cuándo", TiempoRelativo.completo(evento.momento(), zona), null);
        fila = dato(datos, fila, "Cliente", evento.clienteNombre(), null);
        if (!evento.seccional().isEmpty()) {
            fila = dato(datos, fila, "Seccional", evento.seccional(), null);
        }
        if (!evento.arma().isEmpty()) {
            fila = dato(datos, fila, "Arma", evento.arma(), null);
        }
        if (evento.duracion() != null) {
            fila = dato(datos, fila, "Duración", evento.duracionTexto(), null);
        }
        TextArea texto = new TextArea(evento.detalle());
        texto.setEditable(false);
        texto.setWrapText(true);
        texto.setPrefRowCount(4);
        texto.setPrefWidth(460);
        VBox contenido = new VBox(12, datos, new Label("Detalle completo"), texto);
        if (!evento.captura().isEmpty()) {
            contenido.getChildren().add(seccionCaptura(evento.captura()));
        }
        contenido.setPadding(new Insets(18, 20, 6, 20));
        dialogo.getDialogPane().setContent(contenido);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType inventario = new ButtonType("Ver inventario guardado", ButtonBar.ButtonData.OK_DONE);
        if (evento.tipo().conInventario()) {
            dialogo.getButtonTypes().setAll(inventario, cerrar);
        } else {
            dialogo.getButtonTypes().setAll(cerrar);
        }
        dialogo.showAndWait().filter(boton -> boton == inventario).ifPresent(boton -> verInventario.accept(evento));
    }

    /** La captura se resuelve siempre dentro de la carpeta de capturas: el evento solo guarda el nombre. */
    private javafx.scene.Node seccionCaptura(String nombre) {
        Label titulo = new Label("Captura de pantalla del fallo");
        Path archivo = carpetaCapturas.resolve(nombre).normalize();
        if (!archivo.startsWith(carpetaCapturas.normalize()) || !java.nio.file.Files.isRegularFile(archivo)) {
            Label ausente = new Label("La captura " + nombre + " ya no está en la carpeta de capturas.");
            ausente.getStyleClass().add("muted");
            ausente.setWrapText(true);
            return new VBox(6, titulo, ausente);
        }
        Image imagen = new Image(archivo.toUri().toString(), 560, 0, true, true);
        if (imagen.isError()) {
            Label ilegible = new Label("No se pudo abrir la captura " + nombre + ".");
            ilegible.getStyleClass().add("muted");
            return new VBox(6, titulo, ilegible);
        }
        ImageView vista = new ImageView(imagen);
        vista.setFitWidth(560);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        javafx.scene.control.ScrollPane desplazamiento = new javafx.scene.control.ScrollPane(vista);
        desplazamiento.setPrefViewportHeight(300);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("transparent-scroll");
        Button abrir = new Button("Abrir imagen completa");
        abrir.getStyleClass().add("secondary-button");
        abrir.setOnAction(e -> abrirArchivo.accept(archivo));
        Label archivoNombre = new Label(nombre);
        archivoNombre.getStyleClass().add("muted");
        return new VBox(8, titulo, desplazamiento, new HBox(12, abrir, archivoNombre));
    }

    private static int dato(GridPane rejilla, int fila, String nombre, String valor, javafx.scene.Node nodo) {
        Label etiqueta = new Label(nombre);
        etiqueta.getStyleClass().add("muted");
        rejilla.add(etiqueta, 0, fila);
        if (nodo != null) {
            rejilla.add(nodo, 1, fila);
        } else {
            Label contenido = new Label(valor);
            contenido.setWrapText(true);
            rejilla.add(contenido, 1, fila);
        }
        return fila + 1;
    }

    private void exportar() {
        if (visibles.isEmpty()) {
            mostrarAviso(Alert.AlertType.INFORMATION, "No hay eventos para exportar con los filtros actuales.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar bitácora");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV (Excel)", "*.csv"));
        selector.setInitialFileName("bitacora-monitor-siae-"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now()) + ".csv");
        java.io.File archivo = selector.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            ExportadorBitacoraCsv.escribir(Path.of(archivo.getPath()), visibles, zona);
            mostrarAviso(Alert.AlertType.INFORMATION, visibles.size() + " evento(s) guardados en " + archivo.getName());
        } catch (java.io.IOException | RuntimeException e) {
            mostrarAviso(Alert.AlertType.ERROR, "No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    /** Pide confirmación (Cancelar por defecto) y borra todos los eventos de la bitácora. */
    private void vaciarConConfirmacion() {
        int total;
        try {
            total = eventos.get().size();
        } catch (RuntimeException e) {
            total = -1;   // la bitácora no se pudo leer: igual se permite vaciarla
        }
        if (total == 0) {
            mostrarAviso(Alert.AlertType.INFORMATION, "La bitácora ya está vacía.");
            return;
        }
        ButtonType aceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                (total > 0 ? "Se borrarán los " + total + " eventos de todos los clientes. " : "Se borrarán todos los eventos. ")
                        + "Esta acción no se puede deshacer; si quieres conservarlos, usa antes «Exportar CSV».\n\n"
                        + "No se detiene ningún monitoreo, y el historial de inventarios y las capturas no se borran.",
                aceptar, cancelar);
        if (getScene() != null) {
            confirmacion.initOwner(getScene().getWindow());
        }
        EstilosJavaFx.aplicar(confirmacion.getDialogPane());
        confirmacion.setTitle("Vaciar bitácora");
        confirmacion.setHeaderText("¿Vaciar la bitácora?");
        // Cancelar es la opción por defecto: pulsar Enter sin pensar no debe borrar nada.
        ((Button) confirmacion.getDialogPane().lookupButton(aceptar)).setDefaultButton(false);
        ((Button) confirmacion.getDialogPane().lookupButton(cancelar)).setDefaultButton(true);
        if (confirmacion.showAndWait().orElse(cancelar) != aceptar) {
            return;
        }
        try {
            int borrados = vaciar.getAsInt();
            actualizar();
            mostrarAviso(Alert.AlertType.INFORMATION, "Bitácora vaciada: se borraron " + borrados + " evento(s).");
        } catch (RuntimeException e) {
            mostrarAviso(Alert.AlertType.ERROR, "No se pudo vaciar la bitácora: " + e.getMessage());
        }
    }

    private void mostrarAviso(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        if (getScene() != null) {
            alerta.initOwner(getScene().getWindow());
        }
        EstilosJavaFx.aplicar(alerta.getDialogPane());
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private void ocultarAviso() {
        aviso.setVisible(false);
        aviso.setManaged(false);
    }

    private static String etiquetaPeriodo(java.time.Duration periodo) {
        if (periodo.toHours() < 24) {
            return "Última hora";
        }
        return periodo.toDays() == 1 ? "Últimas 24 horas" : "Últimos " + periodo.toDays() + " días";
    }

    private static <T> StringConverter<T> convertidor(Function<T, String> texto) {
        return new StringConverter<>() {
            @Override
            public String toString(T valor) {
                return valor == null ? "" : texto.apply(valor);
            }

            @Override
            public T fromString(String cadena) {
                return null;
            }
        };
    }
}
