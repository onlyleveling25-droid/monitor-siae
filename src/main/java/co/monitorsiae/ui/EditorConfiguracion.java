package co.monitorsiae.ui;

import co.monitorsiae.acceso.ContrasenaRechazadaException;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.AccesoAplicacion;
import co.monitorsiae.dominio.AlcancePaso2;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.DireccionTenencia;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.EscenarioSimulador;
import co.monitorsiae.dominio.FranjaHoraria;
import co.monitorsiae.dominio.HorarioOperacion;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ModoReaccion;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.NivelEscalamiento;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.PerfilEscalamiento;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.dominio.PreferenciaSonidoCliente;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import co.monitorsiae.notificacion.CanalSonido;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.Duration;
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
final class EditorConfiguracion extends BorderPane {

    private static final Map<DayOfWeek, String> DIAS = Map.of(
            DayOfWeek.MONDAY, "Lun", DayOfWeek.TUESDAY, "Mar", DayOfWeek.WEDNESDAY, "Mié",
            DayOfWeek.THURSDAY, "Jue", DayOfWeek.FRIDAY, "Vie", DayOfWeek.SATURDAY, "Sáb",
            DayOfWeek.SUNDAY, "Dom");

    private final Window propietario;
    private final ServicioAplicacion servicio;
    private final Consumer<Configuracion> alGuardar;
    private final Runnable alCancelar;
    private final ComboBox<NavegadorPreferido> navegador = new ComboBox<>();
    private final ComboBox<EntornoPortal> entornoPortal = new ComboBox<>();
    private final Spinner<Integer> intervalo = new Spinner<>(2, 3600, 5);
    private final Spinner<Integer> variacion = new Spinner<>(0, Configuracion.VARIACION_MAXIMA_PORCENTAJE,
            Configuracion.VARIACION_POR_DEFECTO_PORCENTAJE, 5);
    private final Label ayudaVariacion = new Label();
    private final CheckBox restringirHorario = new CheckBox("Limitar el monitoreo a un horario");
    private final Label estadoHorario = new Label();
    private EditorHorarioSemanal horarioSemanal;
    private final Map<DayOfWeek, CheckBox> dias = new EnumMap<>(DayOfWeek.class);
    private final Map<DayOfWeek, TextField> desdePorDia = new EnumMap<>(DayOfWeek.class);
    private final Map<DayOfWeek, TextField> hastaPorDia = new EnumMap<>(DayOfWeek.class);
    private final CheckBox proteger = new CheckBox("Solicitar contraseña para editar la configuración");
    private final CheckBox sonidoHabilitado = new CheckBox("Sonido para clientes que heredan el ajuste general");
    private final CheckBox vozHabilitada = new CheckBox("Leer las alertas en voz alta");
    private final Label detallePreferenciaSonido = new Label();
    private final ComboBox<TonoAlarma> tonoAlarma = new ComboBox<>();
    private final CheckBox escalamientoHabilitado =
            new CheckBox("Insistir más si una alerta sigue sin reconocerse");
    private final EditorNivelesEscalamiento nivelesEscalamiento = new EditorNivelesEscalamiento();
    private final ListView<Cliente> listaClientes = new ListView<>();
    private final TabPane tabsConfiguracion = new TabPane();
    private Tab tabClientes;
    private final HBox avisoGuardado = new HBox(10);
    private final Label textoAvisoGuardado = new Label();
    private final Map<String, Cliente> borradores = new LinkedHashMap<>();
    private final TextField nombre = new TextField();
    private final CheckBox activo = new CheckBox("Cliente incluido en el monitoreo");
    private final ComboBox<PreferenciaSonidoCliente> preferenciaSonido = new ComboBox<>();
    private final TextField permiso = new TextField();
    private final TextField tipoTramite = new TextField();
    private final ListaEditable seccionales = new ListaEditable(
            "Escribe una seccional y agrégala. Puedes cambiar el orden con las flechas.", "Ej. Medellín");
    private final TextArea descripcion = area("Descripción del trámite (150–500 caracteres)");
    private final Label contadorDescripcion = new Label();
    private final ComboBox<ModoReaccion> modo = new ComboBox<>();
    private final Label ayudaReaccion = new Label();
    private final ComboBox<EscenarioSimulador> escenarioSimulador = new ComboBox<>();
    private final Label descripcionEscenario = new Label();
    private final VBox bloqueEscenario = new VBox(5);
    private final Label ayudaSecuencia = new Label();
    private VBox campoAlternativas;
    private VBox campoComodin;
    /** Modalidad, dirección de tenencia, reintentos y paso 2: solo aplican si se aparta el arma. */
    private final List<Node> camposApartar = new ArrayList<>();
    private final ComboBox<Modalidad> modalidad = new ComboBox<>();
    private final ComboBox<AlcancePaso2> alcancePaso2 = new ComboBox<>();
    private final ListaEditable objetivos = new ListaEditable(
            "Cada fila es un objetivo prioritario. Separa con coma las palabras que deben coincidir juntas.",
            "Ej. glock, g-43");
    private final ComboBox<PoliticaSinObjetivo> politica = new ComboBox<>();
    private final ListaEditable alternativas = new ListaEditable(
            "Opcional. Cada fila es una alternativa; se revisan en el orden de la lista.",
            "Ej. cz, p10");
    private final TextField tipoComodin = new TextField("Pistola");
    private final TextField departamento = new TextField();
    private final TextField ciudad = new TextField();
    private final TextField direccion = new TextField();
    private final ListaEditable excluir = new ListaEditable(
            "Si una palabra aparece en el arma, esta se descarta aunque coincida con un objetivo.",
            "Ej. 22 lr");
    private final Spinner<Integer> reintentos = new Spinner<>(1, 5, 2);
    private String clienteActualId;
    private AccesoAplicacion accesoPendiente;

    EditorConfiguracion(Window propietario, ServicioAplicacion servicio, Consumer<Configuracion> alGuardar,
                        Runnable alCancelar) {
        this.propietario = propietario;
        this.servicio = servicio;
        this.alGuardar = alGuardar;
        this.alCancelar = alCancelar;
        accesoPendiente = servicio.configuracion().acceso();
        crearPagina();
        cargarConfiguracion(servicio.configuracion());
    }

    boolean hayCambiosPendientes() {
        try {
            Configuracion actual = servicio.configuracion();
            Configuracion propuesta = construirConfiguracion();
            return !actual.equals(propuesta)
                    || proteger.isSelected() != actual.acceso().requerirContrasena();
        } catch (IllegalArgumentException | DateTimeException e) {
            return true;
        }
    }

    private void crearPagina() {
        getStyleClass().add("configuration-page");
        VBox encabezado = new VBox(10, encabezadoPanel("Configuración",
                "Administra los ajustes generales y los perfiles de cada cliente."), avisoGuardado);
        setTop(encabezado);
        Tab general = new Tab("General", crearGeneral());
        general.setClosable(false);
        tabClientes = new Tab("Clientes", crearClientes());
        tabClientes.setClosable(false);
        Tab alarmas = new Tab("Alarmas", crearAlarmas());
        alarmas.setClosable(false);
        tabsConfiguracion.getTabs().addAll(general, tabClientes, alarmas);
        setCenter(tabsConfiguracion);
        avisoGuardado.getStyleClass().addAll("visual-notice", "visual-notice-success");
        avisoGuardado.setPadding(new Insets(9, 12, 9, 12));
        avisoGuardado.getChildren().addAll(IconoSvg.crear("check", 14, "#15803D"), textoAvisoGuardado);
        avisoGuardado.setVisible(false);
        avisoGuardado.setManaged(false);
        HBox acciones = new HBox(10);
        acciones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        acciones.setPadding(new Insets(14, 22, 14, 22));
        acciones.getStyleClass().add("configuration-actions");
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("secondary-button");
        cancelar.setOnAction(evento -> alCancelar.run());
        Button botonGuardar = new Button("Guardar cambios");
        botonGuardar.getStyleClass().add("primary-button");
        botonGuardar.setOnAction(evento -> {
            avisoGuardado.setVisible(false);
            avisoGuardado.setManaged(false);
            try {
                Configuracion propuesta = construirConfiguracion();
                servicio.validar(propuesta);
                manejarCambioProteccion();
                Configuracion configuracionFinal = propuesta.conAcceso(accesoPendiente);
                servicio.guardar(configuracionFinal);
                alGuardar.accept(servicio.configuracion());
                mostrarGuardadoCorrecto();
            } catch (RuntimeException e) {
                mostrarError("No se pudo guardar la configuración", e.getMessage());
            }

        });
        acciones.getChildren().addAll(cancelar, botonGuardar);
        setBottom(acciones);
        descripcion.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= ParametrosTramite.DESCRIPCION_MAXIMA ? cambio : null));
        descripcion.textProperty().addListener((observable, antes, ahora) ->
                contadorDescripcion.setText(ahora.length() + " / 500 caracteres"));
        restringirHorario.selectedProperty().addListener((observable, antes, ahora) -> aplicarEstadoHorario());
    }

    void seleccionarCliente(String clienteId) {
        tabsConfiguracion.getSelectionModel().select(tabClientes);
        listaClientes.getItems().stream()
                .filter(cliente -> cliente.id().equals(clienteId))
                .findFirst()
                .ifPresent(cliente -> listaClientes.getSelectionModel().select(cliente));
    }

    private void mostrarGuardadoCorrecto() {
        textoAvisoGuardado.setText(
                "Cambios guardados correctamente. Puedes seguir en Ajustes o volver al Panel.");
        avisoGuardado.setVisible(true);
        avisoGuardado.setManaged(true);
    }

    private Node crearGeneral() {
        GridPane formulario = new GridPane();
        formulario.setHgap(14);
        formulario.setVgap(13);
        formulario.setPadding(new Insets(20, 24, 24, 24));
        formulario.getStyleClass().add("settings-form");
        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(200);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setHgrow(Priority.ALWAYS);
        formulario.getColumnConstraints().addAll(etiquetas, valores);

        navegador.setItems(FXCollections.observableArrayList(NavegadorPreferido.values()));
        agregarSeccion(formulario, 0, "Portal y navegador");
        addRow(formulario, 1, "Navegador preferido", navegador);
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
        addRow(formulario, 2, "Entorno del portal", entornoPortal);
        Label notaEntorno = new Label("Abre e inicia sesión manualmente en el navegador de cada cliente "
                + "antes de iniciar o reanudar su monitoreo.");
        notaEntorno.setWrapText(true);
        notaEntorno.getStyleClass().add("field-help");
        Label etiquetaEscenario = new Label("Escenario del simulador");
        etiquetaEscenario.getStyleClass().add("field-label");
        escenarioSimulador.setItems(FXCollections.observableArrayList(EscenarioSimulador.values()));
        textosDe(escenarioSimulador, EscenarioSimulador::etiqueta);
        descripcionEscenario.getStyleClass().add("field-help");
        descripcionEscenario.setWrapText(true);
        bloqueEscenario.getChildren().addAll(etiquetaEscenario, escenarioSimulador, descripcionEscenario);
        escenarioSimulador.valueProperty().addListener((observable, antes, ahora) -> actualizarEscenario());
        entornoPortal.valueProperty().addListener((observable, antes, ahora) -> actualizarEscenario());
        formulario.add(new VBox(10, notaEntorno, bloqueEscenario), 1, 3);
        intervalo.setEditable(true);
        agregarSeccion(formulario, 4, "Frecuencia y disponibilidad");
        variacion.setEditable(true);
        variacion.setPrefWidth(90);
        Label etiquetaVariacion = new Label("Variación aleatoria de cada espera:");
        HBox filaVariacion = new HBox(8, etiquetaVariacion, variacion, new Label("%"));
        filaVariacion.setAlignment(Pos.CENTER_LEFT);
        ayudaVariacion.getStyleClass().add("field-help");
        ayudaVariacion.setWrapText(true);
        intervalo.getEditor().textProperty().addListener((observable, antes, ahora) -> actualizarAyudaVariacion());
        variacion.getEditor().textProperty().addListener((observable, antes, ahora) -> actualizarAyudaVariacion());
        addRow(formulario, 5, "Intervalo entre consultas", new VBox(8, intervalo, filaVariacion, ayudaVariacion));
        restringirHorario.setText(null);
        restringirHorario.setAccessibleText("Limitar el monitoreo a un horario");
        Label nombreInterruptor = new Label("Limitar el monitoreo a un horario");
        nombreInterruptor.getStyleClass().add("field-label");
        nombreInterruptor.setOnMouseClicked(evento -> restringirHorario.setSelected(!restringirHorario.isSelected()));
        estadoHorario.getStyleClass().add("field-help");
        estadoHorario.setWrapText(true);
        VBox interruptorHorario = new VBox(4, new HBox(8, restringirHorario, nombreInterruptor), estadoHorario);
        addRow(formulario, 6, "Horario de consulta", interruptorHorario);
        for (DayOfWeek dia : DayOfWeek.values()) {
            dias.put(dia, new CheckBox(DIAS.get(dia)));
            desdePorDia.put(dia, campoHora("08:00"));
            hastaPorDia.put(dia, campoHora("16:00"));
        }
        EditorHorarioSemanal horarioSemanal = new EditorHorarioSemanal(dias, desdePorDia, hastaPorDia);
        this.horarioSemanal = horarioSemanal;
        aplicarEstadoHorario();
        addRow(formulario, 7, "Días y horas", horarioSemanal);
        Label notaHorario = new Label(
                "El horario está desactivado por defecto. Actívalo solo si deseas limitar la actividad. Cada día "
                        + "marcado tiene su propio rango de horas (formato HH:mm); un día sin marcar no se consulta.");
        notaHorario.setWrapText(true);
        notaHorario.getStyleClass().add("field-help");
        formulario.add(notaHorario, 0, 8, 2, 1);
        agregarSeccion(formulario, 9, "Seguridad");
        formulario.add(proteger, 0, 10, 2, 1);
        Button cambiar = new Button("Cambiar contraseña");
        cambiar.getStyleClass().add("secondary-button");
        cambiar.setOnAction(event -> cambiarProteccion());
        formulario.add(cambiar, 0, 11);
        VBox contenido = new VBox(8, encabezadoPanel("Preferencias generales",
                "Configura cómo se ejecuta la aplicación y protege los cambios."));
        contenido.setPadding(new Insets(22));
        contenido.getChildren().add(formulario);
        ScrollPaneFormulario desplazamiento = new ScrollPaneFormulario(contenido);
        VBox contenedor = new VBox(desplazamiento);
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        contenedor.getStyleClass().add("settings-page");
        return contenedor;
    }

    private Node crearAlarmas() {
        VBox contenido = new VBox(16);
        contenido.setPadding(new Insets(22));
        contenido.getStyleClass().add("settings-page");
        VBox encabezado = encabezadoPanel("Preferencias de alarmas",
                "Configura por separado los canales de aviso. Silenciar el audio no detiene el monitoreo ni oculta "
                        + "las alertas visuales.");
        GridPane formulario = new GridPane();
        formulario.setHgap(14);
        formulario.setVgap(13);
        formulario.setPadding(new Insets(20, 24, 24, 24));
        formulario.getStyleClass().add("settings-form");
        tonoAlarma.setItems(FXCollections.observableArrayList(TonoAlarma.values()));
        tonoAlarma.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(TonoAlarma tono, boolean vacio) {
                super.updateItem(tono, vacio);
                setText(vacio || tono == null ? null : tono.etiqueta());
            }
        });
        tonoAlarma.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TonoAlarma tono, boolean vacio) {
                super.updateItem(tono, vacio);
                setText(vacio || tono == null ? null : tono.etiqueta());
            }
        });
        Button previsualizar = new Button("Escuchar 3 segundos");
        previsualizar.getStyleClass().add("secondary-button");
        previsualizar.setOnAction(event -> CanalSonido.probar(
                tonoAlarma.getValue() == null ? TonoAlarma.SIRENA : tonoAlarma.getValue()));
        sonidoHabilitado.setSelected(true);
        vozHabilitada.setSelected(true);

        agregarSeccion(formulario, 0, "Canales de aviso");
        formulario.add(tarjetaCanal(sonidoHabilitado,
                "Es el valor predeterminado. En Clientes puedes hacer que un perfil lo herede, lo fuerce activado "
                        + "o lo silencie individualmente."), 0, 1, 2, 1);
        formulario.add(tarjetaCanal(vozHabilitada,
                "Lee en voz alta las alertas activas. Este ajuste se aplica a todos los clientes."), 0, 2, 2, 1);
        agregarSeccion(formulario, 3, "Patrón de sonido");
        formulario.add(tonoAlarma, 0, 4, 2, 1);
        formulario.add(previsualizar, 0, 5);
        Label ayuda = new Label("Cada patrón alterna un tono agudo y uno grave. La alarma real repite el patrón "
                + "mientras la alerta siga activa; la previsualización reproduce tres ciclos para que puedas "
                + "compararlos.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().add("field-help");
        formulario.add(ayuda, 0, 6, 2, 1);
        sonidoHabilitado.selectedProperty().addListener((observable, anterior, nuevo) ->
                actualizarAyudaPreferenciaSonido());
        agregarSeccion(formulario, 7, "Escalamiento de insistencia");
        formulario.add(escalamientoHabilitado, 0, 8, 2, 1);
        Label ayudaEscalamiento = new Label(
                "Opcional. Mientras una alerta siga sin reconocerse, va subiendo de insistencia según el tiempo "
                        + "transcurrido: puede forzar el sonido o la voz aunque estén silenciados, y cambiar el tono.");
        ayudaEscalamiento.setWrapText(true);
        ayudaEscalamiento.getStyleClass().add("field-help");
        formulario.add(ayudaEscalamiento, 0, 9, 2, 1);
        nivelesEscalamiento.disableProperty().bind(escalamientoHabilitado.selectedProperty().not());
        formulario.add(nivelesEscalamiento, 0, 10, 2, 1);
        contenido.getChildren().addAll(encabezado, formulario);
        ScrollPaneFormulario desplazamiento = new ScrollPaneFormulario(contenido);
        VBox contenedor = new VBox(desplazamiento);
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        contenedor.getStyleClass().add("settings-page");
        return contenedor;
    }

    private static VBox tarjetaCanal(CheckBox canal, String detalle) {
        Label explicacion = new Label(detalle);
        explicacion.getStyleClass().add("field-help");
        explicacion.setWrapText(true);
        VBox tarjeta = new VBox(7, canal, explicacion);
        tarjeta.getStyleClass().add("alarm-channel-card");
        return tarjeta;
    }

    private Node crearClientes() {
        listaClientes.setPrefWidth(225);
        listaClientes.getStyleClass().add("profile-list");
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
        eliminar.getStyleClass().add("danger-button");
        eliminar.setMaxWidth(Double.MAX_VALUE);
        eliminar.setOnAction(event -> eliminarCliente());
        Label tituloLista = new Label("Perfiles");
        tituloLista.getStyleClass().add("section-title");
        Label ayudaLista = new Label("Crea y administra la configuración de cada cliente.");
        ayudaLista.getStyleClass().add("field-help");
        ayudaLista.setWrapText(true);
        VBox columnaLista = new VBox(10, tituloLista, ayudaLista, agregar, listaClientes, eliminar);
        VBox.setVgrow(listaClientes, Priority.ALWAYS);
        columnaLista.setPadding(new Insets(18));
        columnaLista.setPrefWidth(250);
        columnaLista.getStyleClass().add("client-list");

        VBox editor = new VBox(13);
        editor.setPadding(new Insets(22));
        editor.getStyleClass().add("profile-form");
        VBox encabezado = encabezadoPanel("Perfil del cliente",
                "Ajusta sus datos de trámite y las preferencias de selección.");
        preferenciaSonido.setItems(FXCollections.observableArrayList(PreferenciaSonidoCliente.values()));
        preferenciaSonido.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(PreferenciaSonidoCliente preferencia, boolean vacio) {
                super.updateItem(preferencia, vacio);
                setText(vacio || preferencia == null ? null : textoPreferenciaSonido(preferencia));
            }
        });
        preferenciaSonido.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PreferenciaSonidoCliente preferencia, boolean vacio) {
                super.updateItem(preferencia, vacio);
                setText(vacio || preferencia == null ? null : textoPreferenciaSonido(preferencia));
            }
        });
        detallePreferenciaSonido.getStyleClass().add("field-help");
        detallePreferenciaSonido.setWrapText(true);
        preferenciaSonido.valueProperty().addListener((observable, anterior, nuevo) ->
                actualizarAyudaPreferenciaSonido());
        ayudaSecuencia.getStyleClass().add("field-help");
        ayudaSecuencia.setWrapText(true);
        Label ayudaComodin = new Label("Se toma cualquier arma de este tipo, sea cual sea su modelo (por ejemplo: "
                + "Pistola). Déjalo vacío si no quieres usar comodín.");
        ayudaComodin.getStyleClass().add("field-help");
        ayudaComodin.setWrapText(true);
        campoAlternativas = campo("2 · Alternativas (se prueban en este orden)", alternativas);
        campoComodin = campo("3 · Tipo comodín: cualquier arma de este tipo (último recurso)",
                new VBox(5, tipoComodin, ayudaComodin));
        politica.valueProperty().addListener((observable, antes, ahora) -> actualizarSecuencia());
        camposApartar.addAll(List.of(
                campo("Modalidad (botón que se pulsa en el portal)", modalidad),
                campo("Departamento", departamento),
                campo("Ciudad", ciudad),
                campo("Dirección completa", direccion),
                campo("Reintentos al apartar", reintentos),
                seccion("Paso 2 · Pre-asignación y cierre del trámite"),
                campo("Alcance del paso 2", alcancePaso2)));
        ayudaReaccion.getStyleClass().add("field-help");
        ayudaReaccion.setWrapText(true);
        textosDe(modo, EditorConfiguracion::textoModo);
        modo.valueProperty().addListener((observable, antes, ahora) -> actualizarCamposSegunReaccion());
        modalidad.valueProperty().addListener((observable, antes, ahora) -> actualizarCamposSegunReaccion());
        editor.getChildren().addAll(encabezado, campo("Nombre para mostrar", nombre), activo,
                campo("Sonido de alertas", new VBox(5, preferenciaSonido, detallePreferenciaSonido)),
                seccion("Paso 1 · Datos del trámite"),
                campo("Cómo obtener el permiso (texto del portal)", permiso),
                campo("Tipo de trámite (texto del portal)", tipoTramite),
                campo("Seccionales preferidas (de mayor a menor prioridad)", seccionales),
                descripcionConContador(),
                seccion("Selección del arma"),
                campo("Reacción al encontrar un arma", new VBox(5, modo, ayudaReaccion)),
                camposApartar.get(0),
                camposApartar.get(1), camposApartar.get(2), camposApartar.get(3),
                campo("1 · Objetivos prioritarios (se prueban en este orden)", objetivos),
                campo("Si no aparece ningún objetivo, ¿qué hacer?", new VBox(5, politica, ayudaSecuencia)),
                campoAlternativas, campoComodin,
                campo("Excluir", excluir), camposApartar.get(4),
                camposApartar.get(5),
                camposApartar.get(6));
        ScrollPaneFormulario scroll = new ScrollPaneFormulario(editor);
        BorderPane cuerpo = new BorderPane(scroll);
        cuerpo.setLeft(columnaLista);
        cuerpo.getStyleClass().add("client-editor");
        HBox.setHgrow(scroll, Priority.ALWAYS);
        HBox.setHgrow(editor, Priority.ALWAYS);
        return cuerpo;
    }

    private void actualizarAyudaPreferenciaSonido() {
        PreferenciaSonidoCliente preferencia = preferenciaSonido.getValue();
        if (preferencia == null) {
            preferencia = PreferenciaSonidoCliente.HEREDAR;
        }
        detallePreferenciaSonido.setText(switch (preferencia) {
            case HEREDAR -> "Sigue el ajuste general: "
                    + (sonidoHabilitado.isSelected() ? "sonido activado." : "sonido desactivado.");
            case ACTIVADO -> "Fuerza el sonido para este cliente, aunque el ajuste general esté desactivado.";
            case DESACTIVADO -> "Silencia solo a este cliente; los avisos visuales y los demás clientes no cambian.";
        });
    }

    /** Refleja en la grilla y en el texto de ayuda si el horario se está aplicando o no. */
    private void aplicarEstadoHorario() {
        boolean activo = restringirHorario.isSelected();
        if (horarioSemanal != null) {
            horarioSemanal.setDisable(!activo);
            horarioSemanal.setOpacity(activo ? 1.0 : 0.45);
        }
        estadoHorario.setText(activo
                ? "Activado: el monitor solo consulta dentro de las horas de cada día marcado."
                : "Desactivado: el monitor consulta a cualquier hora y los días y horas de abajo no se aplican. "
                        + "Marca la casilla para usarlos.");
    }

    /** El escenario solo importa con el simulador; se muestra únicamente entonces. */
    private void actualizarEscenario() {
        boolean simulador = entornoPortal.getValue() != EntornoPortal.PORTAL_REAL;
        bloqueEscenario.setVisible(simulador);
        bloqueEscenario.setManaged(simulador);
        EscenarioSimulador elegido = escenarioSimulador.getValue();
        descripcionEscenario.setText(elegido == null ? "" : elegido.descripcion()
                + " Cada vez que inicias un monitoreo el escenario arranca de nuevo.");
    }

    /** Explica con números qué rango de espera resulta del intervalo y la variación escritos. */
    private void actualizarAyudaVariacion() {
        try {
            int segundos = Integer.parseInt(intervalo.getEditor().getText().strip());
            int porcentaje = Integer.parseInt(variacion.getEditor().getText().strip());
            if (porcentaje <= 0) {
                ayudaVariacion.setText("Sin variación: cada espera dura exactamente " + segundos + " s. Un ritmo fijo "
                        + "es fácil de distinguir del de una persona; se recomienda 20 %.");
                return;
            }
            double minimo = segundos * (1 - porcentaje / 100.0);
            double maximo = segundos * (1 + porcentaje / 100.0);
            ayudaVariacion.setText(String.format(java.util.Locale.forLanguageTag("es"),
                    "Cada espera dura al azar entre %.0f y %.0f s (promedio %d s). Evita un ritmo fijo de consultas; "
                            + "se recomienda 20 %%. Con 0 se desactiva.", minimo, maximo, segundos));
        } catch (NumberFormatException e) {
            ayudaVariacion.setText("Escribe números enteros para ver el rango de la espera.");
        }
    }

    private static TextField campoHora(String ejemplo) {
        TextField campo = new TextField();
        campo.setPromptText(ejemplo);
        campo.setPrefColumnCount(4);
        campo.setMaxWidth(66);
        return campo;
    }

    private static LocalTime horaDe(DayOfWeek dia, Map<DayOfWeek, TextField> campos) {
        try {
            return LocalTime.parse(campos.get(dia).getText().strip());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("La hora de "
                    + dia.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"))
                    + " no es válida: usa el formato HH:mm, por ejemplo 08:00.", e);
        }
    }

    private void cargarConfiguracion(Configuracion configuracion) {
        navegador.setValue(configuracion.navegador());
        entornoPortal.setValue(configuracion.entornoPortal());
        escenarioSimulador.setValue(configuracion.escenarioSimulador());
        actualizarEscenario();
        intervalo.getValueFactory().setValue(configuracion.intervaloConsultaSegundos());
        variacion.getValueFactory().setValue(configuracion.variacionEsperaPorcentaje());
        actualizarAyudaVariacion();
        HorarioOperacion horario = configuracion.horario();
        restringirHorario.setSelected(horario.restringir());
        for (DayOfWeek dia : DayOfWeek.values()) {
            FranjaHoraria franja = horario.franja(dia).orElse(null);
            dias.get(dia).setSelected(franja != null);
            desdePorDia.get(dia).setText(franja == null ? "08:00" : franja.desde().toString());
            hastaPorDia.get(dia).setText(franja == null ? "16:00" : franja.hasta().toString());
        }
        aplicarEstadoHorario();
        proteger.setSelected(configuracion.acceso().requerirContrasena());
        sonidoHabilitado.setSelected(configuracion.preferenciasAlertas().sonidoHabilitado());
        vozHabilitada.setSelected(configuracion.preferenciasAlertas().vozHabilitada());
        tonoAlarma.setValue(configuracion.preferenciasAlertas().tono());
        PerfilEscalamiento perfilEscalamiento = configuracion.preferenciasAlertas().perfilEscalamiento();
        escalamientoHabilitado.setSelected(perfilEscalamiento.habilitado());
        nivelesEscalamiento.setNiveles(perfilEscalamiento.niveles());
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
        int porcentajeVariacion;
        try {
            porcentajeVariacion = Integer.parseInt(variacion.getEditor().getText().strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La variación de la espera debe ser un número entero de 0 a "
                    + Configuracion.VARIACION_MAXIMA_PORCENTAJE + ".", e);
        }
        if (porcentajeVariacion < 0 || porcentajeVariacion > Configuracion.VARIACION_MAXIMA_PORCENTAJE) {
            throw new IllegalArgumentException("La variación de la espera debe estar entre 0 y "
                    + Configuracion.VARIACION_MAXIMA_PORCENTAJE + " %.");
        }
        Map<DayOfWeek, FranjaHoraria> franjas = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dia : DayOfWeek.values()) {
            if (dias.get(dia).isSelected()) {
                franjas.put(dia, new FranjaHoraria(horaDe(dia, desdePorDia), horaDe(dia, hastaPorDia)));
            }
        }
        HorarioOperacion horario = new HorarioOperacion(restringirHorario.isSelected(), franjas,
                HorarioOperacion.ZONA_COLOMBIA);
        PerfilEscalamiento perfilEscalamiento =
                new PerfilEscalamiento(escalamientoHabilitado.isSelected(), nivelesEscalamiento.getNiveles());
        return new Configuracion(base.version(), accesoPendiente, navegador.getValue(), entornoPortal.getValue(),
                segundos, horario, new ArrayList<>(borradores.values()),
                new PreferenciasAlertas(sonidoHabilitado.isSelected(), vozHabilitada.isSelected(),
                        tonoAlarma.getValue(), perfilEscalamiento),
                escenarioSimulador.getValue(), porcentajeVariacion);
    }

    private static String textoEntorno(EntornoPortal entorno) {
        return switch (entorno) {
            case SIMULADOR -> "Simulador (pruebas)";
            case PORTAL_REAL -> "Portal real (sesión manual)";
        };
    }

    private void manejarCambioProteccion() {
        boolean estabaActiva = accesoPendiente.requerirContrasena();
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
                accesoPendiente = servicio.prepararCreacionContrasena(valores[0], valores[1]);
            } finally {
                DialogosContrasena.borrar(valores[0]);
                DialogosContrasena.borrar(valores[1]);
            }
        } else {
            if (!servicio.pideContrasena()) {
                accesoPendiente = servicio.prepararDesactivacionProteccion(null);
                return;
            }
            var actual = DialogosContrasena.pedirActual(propietario, "Desactivar protección");
            if (actual.isEmpty()) {
                throw new IllegalArgumentException("Se canceló la desactivación de la protección.");
            }
            try {
                accesoPendiente = servicio.prepararDesactivacionProteccion(actual.get());
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
                accesoPendiente = servicio.prepararCambioContrasena(actual.get(), valores[0], valores[1]);
                proteger.setSelected(true);
                mostrarInfo("Contraseña preparada", "Pulsa Guardar cambios para aplicar la nueva contraseña.");
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
                accesoPendiente = servicio.prepararCreacionContrasena(valores[0], valores[1]);
                proteger.setSelected(true);
                mostrarInfo("Protección preparada", "Pulsa Guardar cambios para activar la contraseña.");
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
        EstilosJavaFx.aplicar(confirmacion.getDialogPane());
        confirmacion.getDialogPane().getStyleClass().add("notice-dialog-warning");
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
        preferenciaSonido.setValue(cliente.preferenciaSonido());
        permiso.setText(cliente.tramite().obtenerPermiso());
        tipoTramite.setText(cliente.tramite().tipoTramite());
        seccionales.setValues(cliente.tramite().seccionales());
        descripcion.setText(cliente.tramite().descripcion());
        modo.setItems(FXCollections.observableArrayList(ModoReaccion.values()));
        modo.setValue(cliente.modo());
        modalidad.setItems(FXCollections.observableArrayList(Modalidad.values()));
        modalidad.setValue(cliente.seleccion().modalidad());
        alcancePaso2.setItems(FXCollections.observableArrayList(AlcancePaso2.values()));
        alcancePaso2.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(AlcancePaso2 item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : textoAlcancePaso2(item));
            }
        });
        alcancePaso2.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AlcancePaso2 item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : textoAlcancePaso2(item));
            }
        });
        alcancePaso2.setValue(cliente.alcancePaso2());
        actualizarCamposSegunReaccion();
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
        actualizarSecuencia();
        alternativas.setValues(formatearCriterios(cliente.seleccion().alternativas()));
        tipoComodin.setText(cliente.seleccion().tipoComodin());
        departamento.setText(cliente.seleccion().direccionTenencia().departamento());
        ciudad.setText(cliente.seleccion().direccionTenencia().ciudad());
        direccion.setText(cliente.seleccion().direccionTenencia().direccion());
        excluir.setValues(cliente.seleccion().excluir());
        reintentos.getValueFactory().setValue(Math.min(5, cliente.seleccion().reintentos()));
    }

    private Cliente capturarCliente(Cliente base) {
        Cliente anterior = base == null ? Cliente.nuevo("") : base;
        ParametrosTramite tramite = new ParametrosTramite(
                permiso.getText(), tipoTramite.getText(), seccionales.values(), descripcion.getText());
        ReglasSeleccion reglas = new ReglasSeleccion(valor(modalidad.getValue()),
                criterios(objetivos.values()), valor(politica.getValue()), criterios(alternativas.values()),
                tipoComodin.getText(), excluir.values(), reintentos.getValue(), new DireccionTenencia(departamento.getText(), ciudad.getText(), direccion.getText()));
        return new Cliente(anterior.id(), nombre.getText(), activo.isSelected(), tramite,
                valor(modo.getValue()), reglas,
                preferenciaSonido.getValue() == null
                        ? PreferenciaSonidoCliente.HEREDAR
                        : preferenciaSonido.getValue(),
                alcancePaso2.getValue() == null
                        ? AlcancePaso2.SELLAR_TRAMITE_COMPLETO
                        : alcancePaso2.getValue());
    }

    private static String textoModo(ModoReaccion modo) {
        return switch (modo) {
            case APARTAR -> "Apartar el arma automáticamente";
            case ALERTA -> "Solo alertar (yo selecciono el arma)";
        };
    }

    private static <T> void textosDe(ComboBox<T> combo, java.util.function.Function<T, String> texto) {
        combo.setCellFactory(ignorado -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : texto.apply(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : texto.apply(item));
            }
        });
    }

    /**
     * Muestra solo lo que aplica a la reacción elegida: la modalidad, los reintentos y el paso 2 existen
     * únicamente cuando se aparta el arma, y la dirección únicamente si además la modalidad es TENENCIA.
     */
    private void actualizarCamposSegunReaccion() {
        boolean apartar = modo.getValue() != ModoReaccion.ALERTA;
        boolean tenencia = apartar && modalidad.getValue() == Modalidad.TENENCIA;
        for (int i = 0; i < camposApartar.size(); i++) {
            boolean direccionTenencia = i >= 1 && i <= 3;
            boolean visible = direccionTenencia ? tenencia : apartar;
            camposApartar.get(i).setVisible(visible);
            camposApartar.get(i).setManaged(visible);
        }
        ayudaReaccion.setText(apartar
                ? "Al encontrar un arma que cumpla, el sistema la aparta (paso 2) y después avisa."
                : "Solo avisa cuando hay un arma que cumple; tú la seleccionas manualmente en el portal, "
                        + "así que no hay modalidad, reintentos ni paso 2 que configurar.");
    }

    private static String textoAlcancePaso2(AlcancePaso2 alcance) {
        return switch (alcance) {
            case PRE_ASIGNAR_ARMA -> "Pre-asignar el arma y dejar el trámite para revisión manual";
            case SELLAR_TRAMITE_COMPLETO -> "Pre-asignar el arma y sellar el trámite automáticamente";
        };
    }

    private static String textoPreferenciaSonido(PreferenciaSonidoCliente preferencia) {
        return switch (preferencia) {
            case HEREDAR -> "Seguir el ajuste general";
            case ACTIVADO -> "Forzar sonido para este cliente";
            case DESACTIVADO -> "Silenciar solo este cliente";
        };
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
            case SEGUIR_ESPERANDO -> "Solo objetivos: si no hay ninguno, seguir esperando";
            case USAR_ALTERNATIVAS -> "Objetivos → alternativas (sin comodín)";
            case CUALQUIERA_DEL_TIPO -> "Objetivos → comodín (sin alternativas)";
            case USAR_ALTERNATIVAS_LUEGO_TIPO -> "Objetivos → alternativas → comodín";
        };
    }

    private static String secuencia(PoliticaSinObjetivo politica) {
        String descartar = "Antes de todo se descartan las armas de la lista Excluir. ";
        return descartar + switch (politica) {
            case SEGUIR_ESPERANDO -> "Orden: 1) Objetivos. Si ninguno aparece no se toma nada y se sigue consultando; "
                    + "las alternativas y el comodín no se usan.";
            case USAR_ALTERNATIVAS -> "Orden: 1) Objetivos → 2) Alternativas. Si ninguna aparece no se toma nada; "
                    + "el comodín no se usa.";
            case CUALQUIERA_DEL_TIPO -> "Orden: 1) Objetivos → 2) Comodín (cualquier arma del tipo indicado). "
                    + "Las alternativas no se usan.";
            case USAR_ALTERNATIVAS_LUEGO_TIPO -> "Orden: 1) Objetivos → 2) Alternativas → 3) Comodín (cualquier "
                    + "arma del tipo indicado). Si nada coincide no se toma nada.";
        };
    }

    /** Explica la secuencia elegida y atenúa las listas que esa opción no utiliza. */
    private void actualizarSecuencia() {
        PoliticaSinObjetivo elegida = politica.getValue() == null
                ? PoliticaSinObjetivo.SEGUIR_ESPERANDO : politica.getValue();
        ayudaSecuencia.setText(secuencia(elegida));
        boolean usaAlternativas = elegida == PoliticaSinObjetivo.USAR_ALTERNATIVAS
                || elegida == PoliticaSinObjetivo.USAR_ALTERNATIVAS_LUEGO_TIPO;
        boolean usaComodin = elegida == PoliticaSinObjetivo.CUALQUIERA_DEL_TIPO
                || elegida == PoliticaSinObjetivo.USAR_ALTERNATIVAS_LUEGO_TIPO;
        if (campoAlternativas != null) {
            campoAlternativas.setDisable(!usaAlternativas);
            campoAlternativas.setOpacity(usaAlternativas ? 1.0 : 0.45);
        }
        if (campoComodin != null) {
            campoComodin.setDisable(!usaComodin);
            campoComodin.setOpacity(usaComodin ? 1.0 : 0.45);
        }
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

    /** Lista editable de niveles de escalamiento: cada fila es espera + qué forzar + tono. */
    private static final class EditorNivelesEscalamiento extends VBox {
        private final javafx.collections.ObservableList<NivelEscalamiento> niveles =
                FXCollections.observableArrayList();
        private final ListView<NivelEscalamiento> lista = new ListView<>(niveles);
        private final Spinner<Integer> minutosEspera = new Spinner<>(0, 180, 1);
        private final CheckBox forzarSonidoNuevo = new CheckBox("Forzar sonido");
        private final CheckBox forzarVozNuevo = new CheckBox("Forzar voz");
        private final ComboBox<TonoAlarma> tonoNuevo = new ComboBox<>();

        private EditorNivelesEscalamiento() {
            super(7);
            Label nota = new Label(
                    "Define qué se vuelve más insistente cuando una alerta lleva mucho tiempo sin reconocerse. "
                            + "Elige el tiempo y las acciones, y pulsa Agregar nivel.");
            nota.getStyleClass().add("muted");
            nota.setWrapText(true);
            tonoNuevo.setItems(FXCollections.observableArrayList(TonoAlarma.values()));
            tonoNuevo.setCellFactory(ignorado -> new ListCell<>() {
                @Override
                protected void updateItem(TonoAlarma tono, boolean vacio) {
                    super.updateItem(tono, vacio);
                    setText(vacio || tono == null ? null : tono.etiqueta());
                }
            });
            tonoNuevo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(TonoAlarma tono, boolean vacio) {
                    super.updateItem(tono, vacio);
                    setText(vacio || tono == null ? null : tono.etiqueta());
                }
            });
            tonoNuevo.setValue(TonoAlarma.SIRENA);
            minutosEspera.setEditable(true);
            minutosEspera.setPrefWidth(84);
            Label etiquetaMinutos = new Label("Cuando lleve");
            Label unidadMinutos = new Label("minutos sin reconocer:");
            forzarSonidoNuevo.setText("Forzar sonido");
            forzarVozNuevo.setText("Forzar voz");
            forzarSonidoNuevo.setTooltip(new Tooltip("Suena aunque el sonido esté silenciado"));
            forzarVozNuevo.setTooltip(new Tooltip("Habla aunque la voz esté desactivada"));
            Label etiquetaTono = new Label("Tono:");
            Button agregar = new Button("Agregar nivel");
            agregar.getStyleClass().add("secondary-button");
            agregar.setOnAction(event -> agregar());
            HBox condicion = new HBox(8, etiquetaMinutos, minutosEspera, unidadMinutos);
            condicion.setAlignment(Pos.CENTER_LEFT);
            HBox acciones = new HBox(14, forzarSonidoNuevo, forzarVozNuevo);
            acciones.setAlignment(Pos.CENTER_LEFT);
            HBox tono = new HBox(8, etiquetaTono, tonoNuevo, agregar);
            tono.setAlignment(Pos.CENTER_LEFT);
            VBox filaAgregar = new VBox(8, condicion, acciones, tono);
            lista.setPrefHeight(110);
            lista.setPlaceholder(new Label("Todavía no hay niveles configurados."));
            lista.setCellFactory(ignorado -> new ListCell<>() {
                @Override
                protected void updateItem(NivelEscalamiento nivel, boolean vacio) {
                    super.updateItem(nivel, vacio);
                    setText(vacio || nivel == null ? null : textoNivel(nivel));
                }
            });
            Button quitar = new Button("Quitar");
            quitar.getStyleClass().add("text-button");
            quitar.setOnAction(event -> {
                int indice = lista.getSelectionModel().getSelectedIndex();
                if (indice >= 0) {
                    niveles.remove(indice);
                }
            });
            HBox accionesLista = new HBox(7, quitar);
            Label tituloLista = new Label("Niveles configurados (cada uno entra en vigor a partir de su tiempo):");
            tituloLista.getStyleClass().add("muted");
            getChildren().addAll(nota, filaAgregar, tituloLista, lista, accionesLista);
        }

        private static String textoNivel(NivelEscalamiento nivel) {
            StringBuilder texto = new StringBuilder();
            texto.append("A los ").append(nivel.espera().toMinutes()).append(" min: ");
            List<String> acciones = new ArrayList<>();
            if (nivel.forzarSonido()) {
                acciones.add("fuerza sonido");
            }
            if (nivel.forzarVoz()) {
                acciones.add("fuerza voz");
            }
            if (acciones.isEmpty()) {
                acciones.add("sin cambios de canal");
            }
            texto.append(String.join(", ", acciones)).append(" · tono ").append(nivel.tono().etiqueta());
            return texto.toString();
        }

        private void agregar() {
            NivelEscalamiento nivel = new NivelEscalamiento(Duration.ofMinutes(minutosEspera.getValue()),
                    forzarSonidoNuevo.isSelected(), forzarVozNuevo.isSelected(), tonoNuevo.getValue());
            niveles.add(nivel);
            lista.getSelectionModel().select(niveles.size() - 1);
        }

        private List<NivelEscalamiento> getNiveles() {
            return List.copyOf(niveles);
        }

        private void setNiveles(List<NivelEscalamiento> nuevos) {
            niveles.setAll(nuevos);
        }
    }

    /**
     * Horario semanal: una columna por día con su nombre, su casilla, la franja horaria resaltada y
     * los campos de hora de ese día. Un día sin marcar no se consulta.
     */
    private static final class EditorHorarioSemanal extends VBox {
        private static final int FRANJAS = 24;
        private static final double ANCHO_DIA = 72;
        private static final double ALTO_FRANJA = 11;

        private final Map<DayOfWeek, CheckBox> dias;
        private final Map<DayOfWeek, TextField> desde;
        private final Map<DayOfWeek, TextField> hasta;
        private final Map<DayOfWeek, VBox> columnasFranjas = new EnumMap<>(DayOfWeek.class);

        private EditorHorarioSemanal(Map<DayOfWeek, CheckBox> dias, Map<DayOfWeek, TextField> desde,
                                     Map<DayOfWeek, TextField> hasta) {
            super(10);
            this.dias = dias;
            this.desde = desde;
            this.hasta = hasta;

            HBox columnas = new HBox(6);
            columnas.getChildren().add(ejeDeHoras());
            for (DayOfWeek dia : DayOfWeek.values()) {
                CheckBox check = dias.get(dia);
                check.setText(null);
                check.setAccessibleText(nombreCompleto(dia));
                check.setTooltip(new Tooltip("Incluir " + nombreCompleto(dia) + " en el horario"));
                Label nombreDia = new Label(abreviatura(dia));
                nombreDia.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
                nombreDia.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                VBox columna = new VBox(3);
                columna.setAlignment(Pos.TOP_CENTER);
                columna.setPrefWidth(ANCHO_DIA);
                columna.getChildren().addAll(nombreDia, check);
                VBox franjas = new VBox(1);
                for (int hora = 0; hora < FRANJAS; hora++) {
                    javafx.scene.layout.Region franja = new javafx.scene.layout.Region();
                    franja.setPrefSize(ANCHO_DIA - 8, ALTO_FRANJA);
                    franja.setMinHeight(ALTO_FRANJA);
                    franja.setMaxHeight(ALTO_FRANJA);
                    franja.getStyleClass().add("schedule-slot");
                    franjas.getChildren().add(franja);
                }
                columna.getChildren().add(franjas);
                columnasFranjas.put(dia, franjas);
                Label etiquetaDesde = etiquetaHora("Desde");
                Label etiquetaHasta = etiquetaHora("Hasta");
                columna.getChildren().addAll(etiquetaDesde, desde.get(dia), etiquetaHasta, hasta.get(dia));
                columnas.getChildren().add(columna);
                check.selectedProperty().addListener((observable, antes, ahora) -> refrescar());
                desde.get(dia).textProperty().addListener((observable, antes, ahora) -> refrescar());
                hasta.get(dia).textProperty().addListener((observable, antes, ahora) -> refrescar());
            }
            Label leyenda = new Label("Marca los días que quieres monitorear y fija la hora de cada uno; "
                    + "la franja azul es la hora de consulta.");
            leyenda.getStyleClass().add("field-help");
            leyenda.setWrapText(true);
            Button copiar = new Button("Copiar Lunes a todos los días");
            copiar.getStyleClass().add("secondary-button");
            copiar.setTooltip(new Tooltip("Aplica las horas de Lunes a los demás días y los marca"));
            copiar.setOnAction(evento -> copiarLunesATodos());
            HBox acciones = new HBox(10, copiar, new Label("Hora local de Colombia (America/Bogota)"));
            acciones.setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(leyenda, columnas, acciones);
            refrescar();
        }

        private void copiarLunesATodos() {
            for (DayOfWeek dia : DayOfWeek.values()) {
                dias.get(dia).setSelected(true);
                desde.get(dia).setText(desde.get(DayOfWeek.MONDAY).getText());
                hasta.get(dia).setText(hasta.get(DayOfWeek.MONDAY).getText());
            }
        }

        private static Label etiquetaHora(String texto) {
            Label etiqueta = new Label(texto);
            etiqueta.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
            return etiqueta;
        }

        private void refrescar() {
            for (Map.Entry<DayOfWeek, VBox> entrada : columnasFranjas.entrySet()) {
                DayOfWeek dia = entrada.getKey();
                boolean diaHabilitado = dias.get(dia).isSelected();
                LocalTime horaDesde = horaValida(desde.get(dia).getText());
                LocalTime horaHasta = horaValida(hasta.get(dia).getText());
                List<Node> franjas = entrada.getValue().getChildren();
                for (int hora = 0; hora < FRANJAS; hora++) {
                    boolean activa = diaHabilitado && horaDesde != null && horaHasta != null
                            && dentroDelRango(hora, horaDesde, horaHasta);
                    franjas.get(hora).pseudoClassStateChanged(
                            javafx.css.PseudoClass.getPseudoClass("active"), activa);
                }
            }
        }

        /** Eje vertical con la hora cada tres horas, alineado con las franjas de cada día. */
        private static VBox ejeDeHoras() {
            VBox eje = new VBox(1);
            eje.setPrefWidth(34);
            Label espacio = new Label(" ");
            espacio.setMinHeight(42);
            eje.getChildren().add(espacio);
            for (int hora = 0; hora < FRANJAS; hora++) {
                Label etiqueta = new Label(hora % 3 == 0 ? hora + "h" : "");
                etiqueta.setMinHeight(ALTO_FRANJA);
                etiqueta.setMaxHeight(ALTO_FRANJA);
                etiqueta.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B;");
                etiqueta.setAlignment(Pos.CENTER_RIGHT);
                etiqueta.setMaxWidth(Double.MAX_VALUE);
                eje.getChildren().add(etiqueta);
            }
            return eje;
        }

        private static String abreviatura(DayOfWeek dia) {
            return switch (dia) {
                case MONDAY -> "Lun";
                case TUESDAY -> "Mar";
                case WEDNESDAY -> "Mié";
                case THURSDAY -> "Jue";
                case FRIDAY -> "Vie";
                case SATURDAY -> "Sáb";
                case SUNDAY -> "Dom";
            };
        }

        private static String nombreCompleto(DayOfWeek dia) {
            return dia.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"));
        }

        private static boolean dentroDelRango(int hora, LocalTime desde, LocalTime hasta) {
            LocalTime inicioFranja = LocalTime.of(hora, 0);
            LocalTime finFranja = hora == 23 ? LocalTime.MAX : LocalTime.of(hora + 1, 0);
            return inicioFranja.isBefore(hasta) && finFranja.isAfter(desde);
        }

        private static LocalTime horaValida(String texto) {
            try {
                return LocalTime.parse(texto.strip());
            } catch (RuntimeException e) {
                return null;
            }
        }
    }

    private static void addRow(GridPane grid, int row, String etiqueta, Node control) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("field-label");
        grid.add(titulo, 0, row);
        grid.add(control, 1, row);
    }

    private static void agregarSeccion(GridPane grid, int row, String titulo) {
        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("form-section-title");
        grid.add(encabezado, 0, row, 2, 1);
    }

    private static VBox encabezadoPanel(String titulo, String detalle) {
        Label nombre = new Label(titulo);
        nombre.getStyleClass().add("panel-heading");
        Label descripcion = new Label(detalle);
        descripcion.getStyleClass().add("field-help");
        descripcion.setWrapText(true);
        return new VBox(5, nombre, descripcion);
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert error = new Alert(Alert.AlertType.ERROR, mensaje == null ? "Error inesperado." : mensaje);
        error.initOwner(propietario);
        EstilosJavaFx.aplicar(error.getDialogPane());
        error.getDialogPane().getStyleClass().add("notice-dialog-error");
        error.setTitle(titulo);
        error.setHeaderText(titulo);
        error.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert info = new Alert(Alert.AlertType.INFORMATION, mensaje);
        info.initOwner(propietario);
        EstilosJavaFx.aplicar(info.getDialogPane());
        info.getDialogPane().getStyleClass().add("notice-dialog-success");
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
