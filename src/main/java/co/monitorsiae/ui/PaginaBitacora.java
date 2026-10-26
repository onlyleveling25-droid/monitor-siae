package co.monitorsiae.ui;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.monitoreo.EventoOperativo;
import co.monitorsiae.monitoreo.FiltroBitacora;
import co.monitorsiae.monitoreo.TipoEvento;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/** Eventos operativos por cliente, tipo y periodo. Solo lee; no cambia el monitoreo. */
final class PaginaBitacora extends VBox {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final Supplier<List<EventoOperativo>> eventos;
    private final Supplier<List<Cliente>> clientes;
    private final Clock reloj = Clock.systemUTC();
    private final ComboBox<Cliente> filtroCliente = new ComboBox<>();
    private final ComboBox<TipoEvento> filtroTipo = new ComboBox<>();
    private final ComboBox<Duration> filtroPeriodo = new ComboBox<>();
    private final TableView<EventoOperativo> tabla = new TableView<>();
    private final Label cuenta = new Label();
    private final Label aviso = new Label();

    PaginaBitacora(Supplier<List<EventoOperativo>> eventos, Supplier<List<Cliente>> clientes) {
        super(18);
        this.eventos = eventos;
        this.clientes = clientes;
        setPadding(new Insets(28, 32, 28, 32));
        Label titulo = new Label("Bitácora");
        titulo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Hechos relevantes de cada cliente. Los ciclos sin novedad no se registran; "
                + "se conservan los últimos 30 días.");
        subtitulo.getStyleClass().add("muted");
        subtitulo.setWrapText(true);
        VBox encabezado = new VBox(4, titulo, subtitulo);
        encabezado.getStyleClass().add("page-header");

        configurarFiltros();
        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("secondary-button");
        actualizar.setOnAction(e -> actualizar());
        HBox filtros = new HBox(10, filtroCliente, filtroTipo, filtroPeriodo, actualizar);
        filtros.setAlignment(Pos.CENTER_LEFT);

        aviso.getStyleClass().add("validation-banner");
        aviso.setWrapText(true);
        ocultarAviso();
        cuenta.getStyleClass().add("muted");
        configurarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(encabezado, filtros, aviso, tabla, cuenta);
        actualizar();
    }

    /** Vuelve a leer los clientes y los eventos; lo llama la navegación al mostrar la página. */
    void actualizar() {
        Cliente elegido = filtroCliente.getValue();
        List<Cliente> lista = clientes.get();
        filtroCliente.getItems().setAll(lista);
        filtroCliente.setValue(elegido == null ? null
                : lista.stream().filter(c -> c.id().equals(elegido.id())).findFirst().orElse(null));
        try {
            FiltroBitacora filtro = new FiltroBitacora(
                    filtroCliente.getValue() == null ? null : filtroCliente.getValue().id(),
                    filtroTipo.getValue(), filtroPeriodo.getValue());
            List<EventoOperativo> visibles = filtro.aplicar(eventos.get(), reloj);
            tabla.getItems().setAll(visibles);
            cuenta.setText(visibles.size() + " evento(s)");
            ocultarAviso();
        } catch (RuntimeException e) {
            tabla.getItems().clear();
            cuenta.setText("");
            aviso.setText("No se pudo leer la bitácora: " + e.getMessage()
                    + " El monitoreo no se ve afectado.");
            aviso.setVisible(true);
            aviso.setManaged(true);
        }
    }

    private void configurarFiltros() {
        filtroCliente.setPromptText("Todos los clientes");
        filtroCliente.setConverter(convertidor(Cliente::nombre));
        filtroTipo.setPromptText("Todos los tipos");
        filtroTipo.getItems().setAll(TipoEvento.values());
        filtroTipo.setConverter(convertidor(TipoEvento::etiqueta));
        filtroPeriodo.setPromptText("Todo el periodo");
        filtroPeriodo.getItems().setAll(Duration.ofHours(1), Duration.ofDays(1), Duration.ofDays(7));
        filtroPeriodo.setConverter(convertidor(PaginaBitacora::etiquetaPeriodo));
        for (ComboBox<?> combo : List.of(filtroCliente, filtroTipo, filtroPeriodo)) {
            combo.valueProperty().addListener((obs, antes, ahora) -> actualizar());
            combo.setMinWidth(180);
        }
    }

    private void configurarTabla() {
        tabla.getStyleClass().add("client-list");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Aún no hay eventos con estos filtros."));
        tabla.getColumns().add(columna("Hora", 120,
                e -> FORMATO.format(e.momento().atZone(ZoneId.systemDefault()))));
        tabla.getColumns().add(columna("Cliente", 160, EventoOperativo::clienteNombre));
        tabla.getColumns().add(columna("Tipo", 170, e -> e.tipo().etiqueta()));
        tabla.getColumns().add(columna("Detalle", 400, EventoOperativo::detalle));
    }

    private static TableColumn<EventoOperativo, String> columna(String titulo, double ancho,
            java.util.function.Function<EventoOperativo, String> valor) {
        TableColumn<EventoOperativo, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setCellValueFactory(celda ->
                new javafx.beans.property.SimpleStringProperty(valor.apply(celda.getValue())));
        return columna;
    }

    private void ocultarAviso() {
        aviso.setVisible(false);
        aviso.setManaged(false);
    }

    private static String etiquetaPeriodo(Duration periodo) {
        if (periodo.toHours() < 24) {
            return "Última hora";
        }
        return periodo.toDays() == 1 ? "Últimas 24 horas" : "Últimos " + periodo.toDays() + " días";
    }

    private static <T> StringConverter<T> convertidor(java.util.function.Function<T, String> texto) {
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
