package co.monitorsiae.ui;

import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import co.monitorsiae.app.CoordinadorMonitores;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.monitoreo.SnapshotInventario;
import co.monitorsiae.notificacion.Alerta;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.Gravedad;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.stage.Window;

/**
 * Vista operativa. Los casos de uso y las reglas permanecen fuera de los controles JavaFX.
 */
public final class VistaPrincipal extends BorderPane {

    private final Window propietario;
    private final ServicioAplicacion servicio;
    private final CoordinadorMonitores monitores;
    private final CentralAlertas alertas;
    private final Consumer<Configuracion> alGuardar;
    private final VBox barraLateral = new VBox(14);
    private final VBox tarjetas = new VBox(8);
    private VBox panelPrincipal;
    private EditorConfiguracion editorConfiguracion;
    private Button navPanel;
    private Button navAjustes;
    private final VBox avisos = new VBox(8);
    private final Label avisoHistorial = new Label(
            "No se pudo leer el historial de inventarios. El archivo se conserva sin cambios; "
                    + "revisa los registros de la aplicación para recuperarlo.");
    private final HBox avisoVisual = new HBox(10);
    private final Label iconoAviso = new Label();
    private final Label textoAviso = new Label();
    private final Label resumen = new Label();
    private final Label modoTexto = new Label();
    private final Label confirmacionReconocimiento = new Label();
    private final Map<String, Label> estados = new HashMap<>();
    private final Map<String, Label> resultados = new HashMap<>();
    private final Map<String, Label> actividades = new HashMap<>();
    private final Map<String, Label> horasActividad = new HashMap<>();
    private final Map<String, Button> accionesPrincipales = new HashMap<>();
    private final Map<String, MenuItem> detenerDesdeMenu = new HashMap<>();
    private final Map<String, MenuItem> historialDesdeMenu = new HashMap<>();
    private final Map<String, MenuItem> enfocarDesdeMenu = new HashMap<>();
    private final Map<String, Region> acentosFila = new HashMap<>();
    private final Map<String, HBox> filasClientes = new HashMap<>();
    private final Map<String, Instant> tiemposActividad = new HashMap<>();
    private final Map<String, Cliente> clientesPorId = new HashMap<>();
    private final Label totalClientes = new Label("0");
    private final Label clientesMonitoreados = new Label("0");
    private final Label sesionesAbiertas = new Label("0");
    private final Label alertasActivas = new Label("0");
    private List<String> problemas = List.of();


    VistaPrincipal(Window propietario, ServicioAplicacion servicio, CoordinadorMonitores monitores,
                   CentralAlertas alertas, Consumer<Configuracion> alGuardar) {
        this.propietario = propietario;
        this.servicio = servicio;
        this.monitores = monitores;
        this.alertas = alertas;
        this.alGuardar = alGuardar;
        getStyleClass().add("app-root");
        avisoHistorial.getStyleClass().add("validation-banner");
        avisoHistorial.setWrapText(true);
        crearBarraLateral();
        crearContenido();
        refrescarConfiguracion();
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            actualizarTiemposRelativos();
            actualizarMetricas();
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    private void crearBarraLateral() {
        barraLateral.getStyleClass().add("sidebar");
        barraLateral.setPadding(new Insets(18, 10, 18, 10));
        barraLateral.setAlignment(Pos.TOP_CENTER);
        StackPane insignia = new StackPane();
        insignia.getStyleClass().add("app-mark");
        insignia.getChildren().add(IconoSvg.crear("monitor", 22, "#1D4ED8"));
        insignia.setAccessibleText("Monitor SIAE");
        navPanel = botonNavegacion("dashboard", "Panel", true, this::solicitarVolverAlPanel);
        navAjustes = botonNavegacion("settings", "Ajustes", false, this::abrirConfiguracion);
        Region separador = new Region();
        VBox.setVgrow(separador, Priority.ALWAYS);
        Label version = new Label("SIAE");
        version.getStyleClass().add("sidebar-caption");
        barraLateral.getChildren().addAll(insignia, navPanel, navAjustes, separador, version);
        setLeft(barraLateral);
    }

    private Button botonNavegacion(String icono, String texto, boolean seleccionado, Runnable accion) {
        Button boton = new Button();
        establecerEstiloNavegacion(boton, icono, texto, seleccionado);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setAccessibleText(texto);
        boton.setTooltip(new Tooltip(texto));
        boton.setOnAction(evento -> accion.run());
        return boton;
    }

    private static void establecerEstiloNavegacion(Button boton, String icono, String texto,
                                                    boolean seleccionado) {
        boton.getStyleClass().setAll(seleccionado ? "rail-item-selected" : "rail-item");
        VBox contenido = new VBox(6, IconoSvg.crear(icono, 20, seleccionado ? "#1D4ED8" : "#64748B"),
                new Label(texto));
        contenido.setAlignment(Pos.CENTER);
        boton.setGraphic(contenido);
    }

    private void crearContenido() {
        VBox cuerpo = new VBox(18);
        cuerpo.setPadding(new Insets(28, 32, 28, 32));
        HBox encabezado = new HBox(14);
        encabezado.getStyleClass().add("page-header");
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox titulos = new VBox(4);
        Label saludo = new Label("Centro de monitoreo");
        saludo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Estado y actividad de cada cliente, en tiempo real.");
        subtitulo.getStyleClass().add("muted");
        titulos.getChildren().addAll(saludo, subtitulo);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        Button reconocerTodas = new Button("Reconocer todas");
        reconocerTodas.getStyleClass().add("secondary-button");
        reconocerTodas.setGraphic(IconoSvg.crear("bell", 16, "#475569"));
        reconocerTodas.setTooltip(new Tooltip(
                "Silencia y cierra todas las notificaciones activas; no detiene el monitoreo."));
        reconocerTodas.setOnAction(event -> reconocerTodas());
        Button probar = new Button("Probar alarma");
        probar.getStyleClass().add("text-button");
        probar.setOnAction(event -> probarAlarma());
        encabezado.getChildren().addAll(titulos, reconocerTodas, probar);

        HBox modo = new HBox(10);
        modo.getStyleClass().add("environment-banner");
        Circle punto = new Circle(4, Color.web("#2563EB"));
        modoTexto.getStyleClass().add("banner-text");
        modo.getChildren().addAll(punto, modoTexto);

        HBox metricas = new HBox(12);
        metricas.getChildren().addAll(
                tarjetaMetrica("Clientes", totalClientes, "activity", "#1D4ED8"),
                tarjetaMetrica("Monitoreando", clientesMonitoreados, "monitor", "#2563EB"),
                tarjetaMetrica("Sesiones abiertas", sesionesAbiertas, "dashboard", "#0D9488"),
                tarjetaMetrica("Alertas activas", alertasActivas, "bell", "#EA580C"));

        iconoAviso.getStyleClass().add("visual-notice-icon");
        textoAviso.getStyleClass().add("visual-notice-text");
        textoAviso.setWrapText(true);
        HBox.setHgrow(textoAviso, Priority.ALWAYS);
        Button cerrarAviso = new Button();
        cerrarAviso.getStyleClass().add("visual-notice-close");
        cerrarAviso.setGraphic(IconoSvg.crear("close", 14, "#64748B"));
        cerrarAviso.setTooltip(new Tooltip("Cerrar aviso"));
        cerrarAviso.setAccessibleText("Cerrar aviso");
        cerrarAviso.setOnAction(event -> ocultarAvisoVisual());
        avisoVisual.getStyleClass().add("visual-notice");
        avisoVisual.setAlignment(Pos.CENTER_LEFT);
        avisoVisual.setPadding(new Insets(11, 14, 11, 14));
        avisoVisual.getChildren().addAll(iconoAviso, textoAviso, cerrarAviso);
        ocultarAvisoVisual();

        resumen.getStyleClass().add("section-title");
        confirmacionReconocimiento.getStyleClass().add("acknowledgement-banner");
        confirmacionReconocimiento.setWrapText(true);
        confirmacionReconocimiento.setVisible(false);
        confirmacionReconocimiento.setManaged(false);
        avisos.setFillWidth(true);
        tarjetas.setFillWidth(true);
        ScrollPane desplazamiento = new ScrollPane(tarjetas);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        HBox tituloClientes = new HBox();
        tituloClientes.setAlignment(Pos.CENTER_LEFT);
        resumen.getStyleClass().add("section-title");
        tituloClientes.getChildren().add(resumen);
        cuerpo.getChildren().addAll(encabezado, modo, metricas, avisos, avisoVisual,
                confirmacionReconocimiento, tituloClientes, desplazamiento);
        panelPrincipal = cuerpo;
        setCenter(panelPrincipal);
    }

    private VBox tarjetaMetrica(String titulo, Label valor, String icono, String color) {
        HBox encabezado = new HBox(8, IconoSvg.crear(icono, 16, color), new Label(titulo));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.getStyleClass().add("metric-heading");
        valor.getStyleClass().add("metric-value");
        VBox tarjeta = new VBox(10, encabezado, valor);
        tarjeta.getStyleClass().add("metric-card");
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    public void refrescarConfiguracion() {
        Configuracion configuracion = servicio.configuracion();
        modoTexto.setText(configuracion.entornoPortal() == EntornoPortal.SIMULADOR
                ? "Simulador · pruebas sin conexión ni acciones en SIAE."
                : "Portal real · inicia sesión en el navegador de cada cliente antes de monitorear.");
        tarjetas.getChildren().clear();
        estados.clear();
        resultados.clear();
        actividades.clear();
        horasActividad.clear();
        accionesPrincipales.clear();
        detenerDesdeMenu.clear();
        historialDesdeMenu.clear();
        enfocarDesdeMenu.clear();
        acentosFila.clear();
        filasClientes.clear();
        tiemposActividad.clear();
        clientesPorId.clear();
        resumen.setText("Clientes configurados");
        problemas = servicio.problemasConfiguracion();
        avisos.getChildren().clear();
        if (!problemas.isEmpty()) {
            Label aviso = new Label("Completa la configuración antes de iniciar el monitoreo.");
            aviso.getStyleClass().add("validation-banner");
            aviso.setWrapText(true);
            avisos.getChildren().add(aviso);
        }
        if (configuracion.clientes().isEmpty()) {
            Label vacio = new Label("Aún no hay clientes.\nAgrega el primero desde Ajustes para empezar.");
            vacio.getStyleClass().add("empty-state");
            tarjetas.getChildren().add(vacio);
            actualizarMetricas();
            return;
        }
        for (Cliente cliente : configuracion.clientes()) {
            clientesPorId.put(cliente.id(), cliente);
            tarjetas.getChildren().add(tarjeta(cliente));
        }
        actualizarMetricas();
    }

    public void actualizarEstado(String clienteId, CoordinadorMonitores.Estado estado) {
        Cliente cliente = clientesPorId.get(clienteId);
        Label etiqueta = estados.get(clienteId);
        if (etiqueta == null) {
            return;
        }
        String estadoAnterior = etiqueta.getText();
        etiqueta.setText(textoEstado(estado));
        etiqueta.getStyleClass().removeAll("status-ready", "status-running", "status-stopped", "status-error",
                "status-alert", "status-session", "status-success", "status-waiting");
        etiqueta.getStyleClass().add(claseEstado(estado));
        Label resultado = resultados.get(clienteId);
        boolean resultadoCambio = false;
        if (resultado != null) {
            String ultimo = monitores.ultimoResultado(clienteId);
            String nuevo = ultimo.isBlank() ? "Todavía no hay resultados." : ultimo;
            resultadoCambio = !resultado.getText().equals(nuevo);
            resultado.setText(nuevo);
        }
        Label actividad = actividades.get(clienteId);
        if (actividad != null) {
            actividad.setText(textoActividad(estado));
        }
        Label hora = horasActividad.get(clienteId);
        if (hora != null) {
            tiemposActividad.put(clienteId, Instant.now());
            actualizarTiempoRelativo(clienteId);
        }
        Region acento = acentosFila.get(clienteId);
        if (acento != null && (!estadoAnterior.equals(textoEstado(estado)) || resultadoCambio)) {
            FadeTransition transicion = new FadeTransition(Duration.millis(600), acento);
            transicion.setFromValue(0.95);
            transicion.setToValue(0.3);
            transicion.playFromStart();
        }
        if (cliente != null && servicio.configuracion().entornoPortal() == EntornoPortal.PORTAL_REAL) {
            switch (estado) {
                case ABRIENDO_SESION -> mostrarAvisoVisual(
                        cliente.nombre() + ": abriendo el navegador del perfil.",
                        TipoAviso.INFO);
                case SESION_ABIERTA -> mostrarAvisoVisual(
                        cliente.nombre() + ": navegador abierto. Completa el inicio de sesión manualmente.",
                        TipoAviso.SUCCESS);
                case SESION_CERRADA -> mostrarAvisoVisual(
                        cliente.nombre() + ": navegador cerrado. Puedes abrir una nueva sesión.",
                        TipoAviso.INFO);
                case DETENIDO -> mostrarAvisoVisual(monitores.sesionAbierta(clienteId)
                        ? cliente.nombre() + ": monitoreo detenido; la sesión del navegador permanece abierta."
                        : cliente.nombre() + ": monitoreo detenido.", TipoAviso.INFO);
                case ERROR -> mostrarAvisoVisual(
                        cliente.nombre() + ": " + monitores.ultimoResultado(clienteId),
                        TipoAviso.ERROR);
                default -> {
                }
            }
        }

        if (cliente != null) {
            Button accionPrincipal = accionesPrincipales.get(clienteId);
            if (accionPrincipal != null) {
                actualizarAccionPrincipal(cliente, accionPrincipal, estado);
            }
        }
        MenuItem detener = detenerDesdeMenu.get(clienteId);
        if (detener != null) {
            detener.setDisable(!puedeDetener(estado));
        }
        MenuItem historial = historialDesdeMenu.get(clienteId);
        if (historial != null) {
            historial.setDisable(consultarHistorial(clienteId).isEmpty());
        }
        MenuItem enfocar = enfocarDesdeMenu.get(clienteId);
        if (enfocar != null) {
            enfocar.setDisable(!esPortalReal() || !monitores.sesionAbierta(clienteId));
        }
        actualizarEstiloFila(clienteId, estado);
        actualizarMetricas();
    }

    private HBox tarjeta(Cliente cliente) {
        HBox fila = new HBox(16);
        fila.setId("cliente-" + cliente.id().replace("-", ""));
        fila.getStyleClass().add("client-row");
        fila.setAlignment(Pos.CENTER_LEFT);
        filasClientes.put(cliente.id(), fila);
        Region acento = new Region();
        acento.getStyleClass().addAll("row-accent", claseAcento(monitores.estado(cliente.id())));
        acentosFila.put(cliente.id(), acento);

        VBox identidad = new VBox(5);
        Label nombre = new Label(cliente.nombre().isBlank() ? "Cliente sin nombre" : cliente.nombre());
        nombre.getStyleClass().add("client-name");
        Label detalle = new Label(cliente.activo() ? "Activo · " + textoModo(cliente)
                : "Inactivo · se puede editar en Ajustes");
        detalle.getStyleClass().add(cliente.activo() ? "client-active" : "client-inactive");
        identidad.getChildren().addAll(nombre, detalle);
        StackPane avatar = new StackPane(new Circle(17, Color.web("#EFF6FF")));
        Label inicial = new Label(cliente.nombre().isBlank() ? "?" :
                cliente.nombre().substring(0, 1).toUpperCase(java.util.Locale.ROOT));
        inicial.getStyleClass().add("client-avatar-label");
        avatar.getChildren().add(inicial);
        HBox identidadCliente = new HBox(11, avatar, identidad);
        identidadCliente.setAlignment(Pos.CENTER_LEFT);
        identidadCliente.setMinWidth(190);
        identidadCliente.setPrefWidth(235);

        CoordinadorMonitores.Estado estadoActual = monitores.estado(cliente.id());
        Label estado = new Label(textoEstado(estadoActual));
        estado.getStyleClass().addAll("status-pill", claseEstado(estadoActual));
        estados.put(cliente.id(), estado);

        VBox actividadReciente = new VBox(4);
        Label actividad = new Label(textoActividad(estadoActual));
        actividad.getStyleClass().add("activity-event");
        actividad.setWrapText(false);
        actividad.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        actividades.put(cliente.id(), actividad);
        String mensaje = monitores.ultimoResultado(cliente.id());
        Label resultado = new Label(mensaje.isBlank() ? "Todavía no hay resultados." : mensaje);
        resultado.getStyleClass().add("activity-detail");
        resultado.setMaxWidth(Double.MAX_VALUE);
        resultado.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        resultado.setWrapText(true);
        resultados.put(cliente.id(), resultado);
        Label hora = new Label("ahora");
        hora.getStyleClass().add("activity-time");
        horasActividad.put(cliente.id(), hora);
        tiemposActividad.put(cliente.id(), Instant.now());
        HBox detalleActividad = new HBox(8, resultado, hora);
        detalleActividad.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(resultado, Priority.ALWAYS);
        actividadReciente.getChildren().addAll(actividad, detalleActividad);
        HBox.setHgrow(actividadReciente, Priority.ALWAYS);

        Button principal = new Button();
        principal.getStyleClass().add("primary-button");
        principal.setMinWidth(150);
        principal.setOnAction(event -> {
            if (esPortalReal() && monitores.estado(cliente.id()) == CoordinadorMonitores.Estado.ALERTA_ENCONTRADA
                    && monitores.sesionAbierta(cliente.id())) {
                irAlNavegador(cliente);
            } else if (esPortalReal() && !monitores.sesionAbierta(cliente.id())) {
                abrirSesion(cliente);
            } else {
                iniciar(cliente);
            }
        });
        accionesPrincipales.put(cliente.id(), principal);
        actualizarAccionPrincipal(cliente, principal, estadoActual);

        MenuItem detener = new MenuItem("Detener monitoreo");
        detener.setOnAction(event -> monitores.detener(cliente.id()));
        detener.setDisable(!puedeDetener(estadoActual));
        MenuItem historial = new MenuItem("Ver historial de inventario");
        historial.setDisable(consultarHistorial(cliente.id()).isEmpty());
        historial.setOnAction(event -> mostrarHistorial(cliente.id()));
        MenuItem irAlNavegador = new MenuItem("Ir al navegador");
        irAlNavegador.setDisable(!esPortalReal() || !monitores.sesionAbierta(cliente.id()));
        irAlNavegador.setOnAction(event -> irAlNavegador(cliente));
        enfocarDesdeMenu.put(cliente.id(), irAlNavegador);
        MenuItem editar = new MenuItem("Editar cliente");
        editar.setOnAction(event -> abrirConfiguracion(cliente.id()));
        detenerDesdeMenu.put(cliente.id(), detener);
        historialDesdeMenu.put(cliente.id(), historial);
        ContextMenu menu = new ContextMenu(detener, historial, irAlNavegador,
                new SeparatorMenuItem(), editar);
        Button mas = new Button();
        mas.getStyleClass().add("overflow-button");
        mas.setGraphic(IconoSvg.crear("more", 18, "#475569"));
        mas.setAccessibleText("Más acciones para " + cliente.nombre());
        mas.setTooltip(new Tooltip("Más acciones"));
        mas.setOnAction(event -> menu.show(mas, javafx.geometry.Side.BOTTOM, 0, 2));

        fila.getChildren().addAll(acento, identidadCliente, actividadReciente, estado, principal, mas);
        if (estadoActual == CoordinadorMonitores.Estado.ALERTA_ENCONTRADA) {
            fila.getStyleClass().add("client-row-alert");
        }
        return fila;
    }

    private boolean esPortalReal() {
        return servicio.configuracion().entornoPortal() == EntornoPortal.PORTAL_REAL;
    }

    private void actualizarMetricas() {
        totalClientes.setText(Integer.toString(clientesPorId.size()));
        long monitoreando = clientesPorId.keySet().stream()
                .filter(id -> monitores.estado(id) == CoordinadorMonitores.Estado.MONITOREANDO)
                .count();
        long sesiones = clientesPorId.keySet().stream().filter(monitores::sesionAbierta).count();
        clientesMonitoreados.setText(Long.toString(monitoreando));
        sesionesAbiertas.setText(Long.toString(sesiones));
        alertasActivas.setText(Integer.toString(alertas.activas().size()));
    }

    private void actualizarTiemposRelativos() {
        tiemposActividad.keySet().forEach(this::actualizarTiempoRelativo);
    }

    private void actualizarTiempoRelativo(String clienteId) {
        Instant actualizado = tiemposActividad.get(clienteId);
        Label hora = horasActividad.get(clienteId);
        if (actualizado == null || hora == null) {
            return;
        }
        long segundos = Math.max(0, java.time.Duration.between(actualizado, Instant.now()).getSeconds());
        String texto;
        if (segundos < 60) {
            texto = segundos == 0 ? "ahora" : "hace " + segundos + " s";
        } else if (segundos < 3600) {
            texto = "hace " + (segundos / 60) + " min";
        } else {
            texto = "hace " + (segundos / 3600) + " h";
        }
        hora.setText(texto);
    }

    private void actualizarEstiloFila(String clienteId, CoordinadorMonitores.Estado estado) {
        Region acento = acentosFila.get(clienteId);
        if (acento != null) {
            acento.getStyleClass().removeAll("accent-neutral", "accent-info", "accent-session",
                    "accent-waiting", "accent-alert", "accent-success", "accent-danger");
            acento.getStyleClass().add(claseAcento(estado));
        }
        HBox fila = filasClientes.get(clienteId);
        if (fila != null) {
            fila.getStyleClass().remove("client-row-alert");
            if (estado == CoordinadorMonitores.Estado.ALERTA_ENCONTRADA) {
                fila.getStyleClass().add("client-row-alert");
            }
        }
    }

    private static String claseAcento(CoordinadorMonitores.Estado estado) {
        return switch (estado) {
            case ABRIENDO_SESION, INICIANDO, MONITOREANDO, DETENIENDO -> "accent-info";
            case SESION_ABIERTA -> "accent-session";
            case FUERA_DE_HORARIO, DETENIDO, SESION_CERRADA -> "accent-waiting";
            case ALERTA_ENCONTRADA -> "accent-alert";
            case ARMA_APARTADA -> "accent-success";
            case ERROR -> "accent-danger";
            default -> "accent-neutral";
        };
    }

    private boolean puedeIniciar(Cliente cliente) {
        if (!cliente.activo() || !problemas.isEmpty()) {
            return false;
        }
        return !esPortalReal() || monitores.sesionAbierta(cliente.id());
    }

    private void actualizarAccionPrincipal(Cliente cliente, Button boton, CoordinadorMonitores.Estado estado) {
        if (esPortalReal() && estado == CoordinadorMonitores.Estado.ALERTA_ENCONTRADA
                && monitores.sesionAbierta(cliente.id())) {
            boton.setText("Ir al navegador");
            boton.setDisable(false);
            boton.setTooltip(new Tooltip("Muestra el navegador de este cliente para revisar la oportunidad."));
            return;
        }
        boolean requiereAbrirSesion = esPortalReal() && !monitores.sesionAbierta(cliente.id());
        if (requiereAbrirSesion) {
            boton.setText("Abrir sesión");
            boton.setDisable(ocupado(estado));
            boton.setTooltip(new Tooltip("Abre el navegador de este cliente para iniciar sesión manualmente."));
        } else if (estado == CoordinadorMonitores.Estado.MONITOREANDO
                || estado == CoordinadorMonitores.Estado.FUERA_DE_HORARIO) {
            boton.setText("Monitoreando");
            boton.setDisable(true);
            boton.setTooltip(new Tooltip("Detén el monitoreo desde el menú de más acciones."));
        } else if (estado == CoordinadorMonitores.Estado.INICIANDO
                || estado == CoordinadorMonitores.Estado.DETENIENDO) {
            boton.setText(estado == CoordinadorMonitores.Estado.INICIANDO ? "Iniciando…" : "Deteniendo…");
            boton.setDisable(true);
            boton.setTooltip(new Tooltip("La acción solicitada está en curso."));
        } else {
            boton.setText("Iniciar / reanudar");
            boton.setDisable(!puedeIniciar(cliente));
            boton.setTooltip(new Tooltip(motivoDeInicio(cliente)));
        }
    }

    private static String textoModo(Cliente cliente) {
        return switch (cliente.modo()) {
            case ALERTA -> "Solo alertar";
            case APARTAR -> "Apartar automáticamente";
        };
    }

    private static boolean puedeDetener(CoordinadorMonitores.Estado estado) {
        return estado == CoordinadorMonitores.Estado.INICIANDO
                || estado == CoordinadorMonitores.Estado.MONITOREANDO
                || estado == CoordinadorMonitores.Estado.FUERA_DE_HORARIO;
    }

    private static String textoActividad(CoordinadorMonitores.Estado estado) {
        return switch (estado) {
            case LISTO -> "Cliente preparado";
            case ABRIENDO_SESION -> "Abriendo sesión del portal";
            case SESION_ABIERTA -> "Sesión del portal abierta";
            case SESION_CERRADA -> "Sesión del portal cerrada";
            case INICIANDO -> "Iniciando monitoreo";
            case MONITOREANDO -> "Consultando inventario";
            case FUERA_DE_HORARIO -> "En espera del horario configurado";
            case ALERTA_ENCONTRADA -> "Inventario disponible; alerta generada";
            case ARMA_APARTADA -> "Arma apartada; monitoreo finalizado";
            case DETENIENDO -> "Deteniendo monitoreo";
            case DETENIDO -> "Monitoreo detenido";
            case ERROR -> "Se produjo un error";
        };
    }

    private String motivoDeInicio(Cliente cliente) {
        if (!cliente.activo()) {
            return "El cliente está inactivo; habilítalo desde configuración.";
        }
        if (!problemas.isEmpty()) {
            return "Completa la configuración antes de iniciar.";
        }
        if (esPortalReal() && !monitores.sesionAbierta(cliente.id())) {
            return "Abre la sesión de este cliente e inicia sesión en la plataforma. "
                    + "Después podrás iniciar el monitoreo.";
        }
        return "Empieza a consultar el inventario de este cliente.";
    }

    private static boolean ocupado(CoordinadorMonitores.Estado estado) {
        return switch (estado) {
            case ABRIENDO_SESION, INICIANDO, MONITOREANDO, DETENIENDO -> true;
            default -> false;
        };
    }

    private void iniciar(Cliente cliente) {
        if (!problemas.isEmpty()) {
            mostrar(Alert.AlertType.WARNING, "Configuración incompleta",
                    String.join(System.lineSeparator(), servicio.problemasConfiguracion()));
            return;
        }
        try {
            monitores.iniciar(cliente);
        } catch (RuntimeException e) {
            mostrar(Alert.AlertType.ERROR, "No se pudo iniciar", e.getMessage());
        }
    }

    private void irAlNavegador(Cliente cliente) {
        monitores.enfocarVentana(cliente.id()).whenComplete((ignorado, error) -> Platform.runLater(() -> {
            if (error == null) {
                mostrarAvisoVisual(cliente.nombre()
                        + ": solicitud de enfoque enviada. Si Windows no la trae al frente, "
                        + "selecciona su icono en la barra de tareas.", TipoAviso.SUCCESS);
            } else {
                mostrarAvisoVisual(cliente.nombre() + ": " + mensajeErrorEnfoque(error), TipoAviso.ERROR);
                CanalAlertaJavaFx.reconocerSiSesionCerrada(cliente.id(), error, alertas::reconocer);
            }
        }));
    }

    private static String mensajeErrorEnfoque(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null
                && (causa instanceof java.util.concurrent.CompletionException
                || causa instanceof java.util.concurrent.ExecutionException)) {
            causa = causa.getCause();
        }
        return causa.getMessage() == null || causa.getMessage().isBlank()
                ? "No se pudo enfocar el navegador."
                : causa.getMessage();
    }

    private void abrirSesion(Cliente cliente) {
        try {
            monitores.abrirSesion(cliente);
        } catch (RuntimeException e) {
            mostrar(Alert.AlertType.ERROR, "No se pudo abrir la sesión", e.getMessage());
        }
    }

    private void abrirConfiguracion() {
        abrirConfiguracion(null);
    }

    private void abrirConfiguracion(String clienteId) {
        if (editorConfiguracion != null) {
            if (getCenter() != editorConfiguracion) {
                setCenter(editorConfiguracion);
                actualizarNavegacion(true);
            }
            if (clienteId != null) {
                editorConfiguracion.seleccionarCliente(clienteId);
            }
            return;
        }
        if (!autorizarEdicion()) {
            return;
        }
        editorConfiguracion = new EditorConfiguracion(propietario, servicio, configuracion -> {
            alGuardar.accept(configuracion);
        }, this::solicitarVolverAlPanel);
        if (clienteId != null) {
            editorConfiguracion.seleccionarCliente(clienteId);
        }
        setCenter(editorConfiguracion);
        actualizarNavegacion(true);
    }

    private void solicitarVolverAlPanel() {
        if (!confirmarDescarteCambios()) {
            return;
        }
        editorConfiguracion = null;
        mostrarPanel();
    }

    boolean confirmarDescarteCambios() {
        if (editorConfiguracion != null && editorConfiguracion.hayCambiosPendientes()) {
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                    "Los cambios que no guardaste se perderán.");
            confirmacion.initOwner(propietario);
            EstilosJavaFx.aplicar(confirmacion.getDialogPane());
            confirmacion.setTitle("Descartar cambios");
            confirmacion.setHeaderText("¿Salir de Configuración?");
            ButtonType descartar = new ButtonType("Descartar cambios", ButtonBar.ButtonData.OK_DONE);
            confirmacion.getButtonTypes().setAll(descartar, ButtonType.CANCEL);
            if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != descartar) {
                return false;
            }
        }
        return true;
    }

    private void mostrarPanel() {
        setCenter(panelPrincipal);
        actualizarNavegacion(false);
    }

    private void actualizarNavegacion(boolean configuracionActiva) {
        establecerEstiloNavegacion(navPanel, "dashboard", "Panel", !configuracionActiva);
        establecerEstiloNavegacion(navAjustes, "settings", "Ajustes", configuracionActiva);
    }

    private boolean autorizarEdicion() {
        try {
            if (servicio.debeCrearContrasena()) {
                var nuevas = DialogosContrasena.pedirNueva(propietario, "Crear contraseña de la aplicación");
                if (nuevas.isEmpty()) {
                    return false;
                }
                char[][] valores = nuevas.get();
                try {
                    servicio.crearContrasena(valores[0], valores[1]);
                } finally {
                    DialogosContrasena.borrar(valores[0]);
                    DialogosContrasena.borrar(valores[1]);
                }
                return true;
            }
            if (!servicio.pideContrasena()) {
                return true;
            }
            var actual = DialogosContrasena.pedirActual(propietario, "Desbloquear configuración");
            if (actual.isEmpty()) {
                return false;
            }
            boolean correcta = servicio.verificarContrasena(actual.get());
            if (!correcta) {
                mostrar(Alert.AlertType.ERROR, "Contraseña incorrecta",
                        "No se desbloqueó la configuración. Verifica la contraseña e inténtalo de nuevo.");
            }
            return correcta;
        } catch (RuntimeException e) {
            mostrar(Alert.AlertType.ERROR, "No se pudo verificar el acceso", e.getMessage());
            return false;
        }
    }

    private void probarAlarma() {
        alertas.emitir(new Alerta("monitor-siae-prueba", Gravedad.OPORTUNIDAD,
                "Alarma de prueba", "Si escuchas la sirena o la voz, los canales locales están activos.",
                "Prueba de alarma de Monitor SIAE."));
    }

    private void reconocerTodas() {
        int cantidad = alertas.activas().size();
        alertas.reconocerTodas();
        mostrarConfirmacionReconocimiento(cantidad == 0
                ? "No había alertas activas."
                : "Se reconocieron " + cantidad + (cantidad == 1 ? " alerta." : " alertas.")
                        + " Se cerraron las notificaciones y se detuvieron las repeticiones; "
                        + "el monitoreo no se detuvo.");
    }

    private void mostrarConfirmacionReconocimiento(String mensaje) {
        confirmacionReconocimiento.setText(mensaje);
        confirmacionReconocimiento.setVisible(true);
        confirmacionReconocimiento.setManaged(true);
    }

    private void mostrarAvisoVisual(String mensaje, TipoAviso tipo) {
        avisoVisual.getStyleClass().removeAll("visual-notice-success", "visual-notice-info",
                "visual-notice-error");
        avisoVisual.getStyleClass().add(switch (tipo) {
            case SUCCESS -> "visual-notice-success";
            case INFO -> "visual-notice-info";
            case ERROR -> "visual-notice-error";
        });
        iconoAviso.setGraphic(switch (tipo) {
            case SUCCESS -> IconoSvg.crear("check", 14, "#15803D");
            case INFO -> IconoSvg.crear("info", 14, "#1D4ED8");
            case ERROR -> IconoSvg.crear("alert", 14, "#B91C1C");
        });
        textoAviso.setText(mensaje);
        avisoVisual.setVisible(true);
        avisoVisual.setManaged(true);
    }

    private void ocultarAvisoVisual() {
        avisoVisual.setVisible(false);
        avisoVisual.setManaged(false);
    }

    private void mostrarHistorial(String clienteId) {
        List<SnapshotInventario> snapshots = consultarHistorial(clienteId);
        if (snapshots.isEmpty()) {
            return;
        }
        if (snapshots.size() == 1) {
            mostrarSnapshot(snapshots.getFirst());
            return;
        }
        Dialog<SnapshotInventario> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle("Historial de inventarios");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getStyleClass().add("history-dialog");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.setResultConverter(boton -> null);
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        dialogo.getDialogPane().lookupButton(ButtonType.CLOSE).getStyleClass().add("secondary-button");

        VBox contenido = new VBox(12);
        contenido.getStyleClass().add("history-content");
        Label titulo = new Label("Historial de inventarios");
        titulo.getStyleClass().add("dialog-title");
        Label descripcion = new Label("Selecciona una consulta para revisar el inventario y la elección del sistema.");
        descripcion.getStyleClass().add("dialog-description");
        descripcion.setWrapText(true);
        VBox lista = new VBox(6);
        for (SnapshotInventario snapshot : snapshots) {
            Button opcion = botonSnapshot(snapshot, dialogo);
            lista.getChildren().add(opcion);
        }
        ScrollPane desplazamiento = new ScrollPane(lista);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("history-scroll");
        desplazamiento.setPrefViewportHeight(Math.min(430, snapshots.size() * 52));
        contenido.getChildren().addAll(titulo, descripcion, desplazamiento);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(620);
        dialogo.showAndWait().ifPresent(this::mostrarSnapshot);
    }

    private List<SnapshotInventario> consultarHistorial(String clienteId) {
        ConsultaHistorial consulta = ConsultaHistorial.leer(() -> monitores.historialSnapshots(clienteId));
        if (consulta.error() != null) {
            if (!avisos.getChildren().contains(avisoHistorial)) {
                org.slf4j.LoggerFactory.getLogger(VistaPrincipal.class)
                        .warn("No se pudo consultar el historial de inventarios.", consulta.error());
                avisos.getChildren().add(avisoHistorial);
            }
        } else {
            avisos.getChildren().remove(avisoHistorial);
        }
        return consulta.snapshots();
    }

    private static Button botonSnapshot(SnapshotInventario snapshot, Dialog<SnapshotInventario> dialogo) {
        Label fecha = new Label(fechaSnapshot(snapshot));
        fecha.getStyleClass().add("history-date");
        Label seccional = new Label(snapshot.seccional());
        seccional.getStyleClass().add("history-location");
        HBox resumen = new HBox(8, fecha, seccional);
        resumen.setAlignment(Pos.CENTER_LEFT);
        Label seleccion = new Label(snapshot.eleccion().arma().descripcion());
        seleccion.getStyleClass().add("history-selection");
        seleccion.setWrapText(true);
        VBox textos = new VBox(2, resumen, seleccion);
        HBox contenido = new HBox(12, IconoSvg.crear("activity", 18, "#1D4ED8"), textos);
        contenido.setAlignment(Pos.CENTER_LEFT);
        Button boton = new Button();
        boton.setGraphic(contenido);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setMinHeight(Region.USE_PREF_SIZE);
        boton.setMaxHeight(Region.USE_PREF_SIZE);
        boton.setAccessibleText("Ver inventario del " + fecha.getText() + ", " + seccional.getText());
        boton.getStyleClass().add("history-entry");
        boton.setOnAction(event -> {
            dialogo.setResult(snapshot);
            dialogo.close();
        });
        return boton;
    }

    private void mostrarSnapshot(SnapshotInventario snapshot) {
        var eleccion = snapshot.eleccion();
        String motivo = switch (eleccion.motivo()) {
            case OBJETIVO -> "Objetivo prioritario #" + (eleccion.prioridad() + 1);
            case ALTERNATIVA -> "Alternativa #" + (eleccion.prioridad() + 1);
            case CUALQUIERA_DEL_TIPO -> "Comodín por tipo";
        };
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle("Inventario analizado");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getStyleClass().add("snapshot-dialog");
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        dialogo.getDialogPane().lookupButton(ButtonType.CLOSE).getStyleClass().add("secondary-button");

        VBox contenido = new VBox(14);
        contenido.getStyleClass().add("snapshot-content");
        Label titulo = new Label("Inventario analizado");
        titulo.getStyleClass().add("dialog-title");
        Label subtitulo = new Label("Evidencia de la decisión del sistema");
        subtitulo.getStyleClass().add("dialog-description");

        VBox eleccionCard = new VBox(6);
        eleccionCard.getStyleClass().add("snapshot-choice-card");
        HBox rotulo = new HBox(8, IconoSvg.crear("check", 17, "#15803D"));
        Label rotuloTexto = new Label("OBJETO ELEGIDO POR EL SISTEMA");
        rotuloTexto.getStyleClass().add("snapshot-choice-label");
        rotulo.getChildren().add(rotuloTexto);
        Label armaElegida = new Label(eleccion.arma().descripcion());
        armaElegida.getStyleClass().add("snapshot-choice-name");
        armaElegida.setWrapText(true);
        Label reglaAplicada = new Label(motivo
                + (eleccion.criterio().isBlank() ? "" : " · " + eleccion.criterio()));
        reglaAplicada.getStyleClass().add("snapshot-choice-reason");
        reglaAplicada.setWrapText(true);
        eleccionCard.getChildren().addAll(rotulo, armaElegida, reglaAplicada);

        HBox datos = new HBox(10,
                datoSnapshot("CLIENTE", snapshot.clienteNombre()),
                datoSnapshot("SECCIONAL", snapshot.seccional()),
                datoSnapshot("FECHA", fechaSnapshot(snapshot)));
        datos.getStyleClass().add("snapshot-metadata");

        Label inventarioTitulo = new Label("Inventario observado · " + snapshot.inventario().size() + " objetos");
        inventarioTitulo.getStyleClass().add("section-title");
        VBox objetos = new VBox(7);
        objetos.getStyleClass().add("snapshot-items");
        snapshot.inventario().forEach(arma -> objetos.getChildren().add(
                filaInventario(arma.posicion() + 1, arma.descripcion(),
                        arma.posicion() == eleccion.arma().posicion())));
        ScrollPane lista = new ScrollPane(objetos);
        lista.setFitToWidth(true);
        lista.getStyleClass().add("history-scroll");
        lista.setPrefViewportHeight(Math.min(360, Math.max(100, snapshot.inventario().size() * 52)));
        contenido.getChildren().addAll(titulo, subtitulo, eleccionCard, datos, inventarioTitulo, lista);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefWidth(760);
        dialogo.showAndWait();
    }

    private static VBox datoSnapshot(String etiqueta, String valor) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("snapshot-meta-label");
        Label contenido = new Label(valor);
        contenido.getStyleClass().add("snapshot-meta-value");
        contenido.setWrapText(true);
        VBox dato = new VBox(5, titulo, contenido);
        dato.getStyleClass().add("snapshot-meta-card");
        HBox.setHgrow(dato, Priority.ALWAYS);
        dato.setMaxWidth(Double.MAX_VALUE);
        return dato;
    }

    private static HBox filaInventario(int numero, String descripcion, boolean elegida) {
        Label posicion = new Label(String.format("%02d", numero));
        posicion.getStyleClass().add("snapshot-item-number");
        Label nombre = new Label(descripcion);
        nombre.getStyleClass().add("snapshot-item-name");
        nombre.setWrapText(true);
        HBox.setHgrow(nombre, Priority.ALWAYS);
        HBox fila;
        if (elegida) {
            Label estado = new Label("ELEGIDO");
            estado.getStyleClass().add("snapshot-item-badge");
            fila = new HBox(10, IconoSvg.crear("check", 17, "#15803D"), posicion, nombre, estado);
            fila.getStyleClass().add("snapshot-item-selected");
        } else {
            fila = new HBox(10, posicion, nombre);
            fila.getStyleClass().add("snapshot-item");
        }
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private static String fechaSnapshot(SnapshotInventario snapshot) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
                .withZone(ZoneId.systemDefault())
                .format(snapshot.registradoEn());
    }

    private static String textoEstado(CoordinadorMonitores.Estado estado) {
        return switch (estado) {
            case LISTO -> "Listo";
            case ABRIENDO_SESION -> "Abriendo navegador";
            case SESION_ABIERTA -> "Sesión abierta";
            case SESION_CERRADA -> "Sesión cerrada";
            case INICIANDO -> "Iniciando";
            case MONITOREANDO -> "Monitoreando";
            case FUERA_DE_HORARIO -> "Fuera del horario";
            case ALERTA_ENCONTRADA -> "Alerta encontrada";
            case ARMA_APARTADA -> "Arma apartada";
            case DETENIENDO -> "Deteniendo";
            case DETENIDO -> "Detenido";
            case ERROR -> "Error";
        };
    }

    private static String claseEstado(CoordinadorMonitores.Estado estado) {
        return switch (estado) {
            case SESION_ABIERTA -> "status-session";
            case SESION_CERRADA -> "status-ready";
            case MONITOREANDO, INICIANDO, DETENIENDO -> "status-running";
            case ALERTA_ENCONTRADA -> "status-alert";
            case ARMA_APARTADA -> "status-success";
            case ERROR -> "status-error";
            case FUERA_DE_HORARIO -> "status-waiting";
            case DETENIDO -> "status-stopped";
            default -> "status-ready";
        };
    }

    private void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.initOwner(propietario);
        EstilosJavaFx.aplicar(alerta.getDialogPane());
        if (tipo == Alert.AlertType.ERROR) {
            alerta.getDialogPane().getStyleClass().add("notice-dialog-error");
        } else if (tipo == Alert.AlertType.WARNING) {
            alerta.getDialogPane().getStyleClass().add("notice-dialog-warning");
        }
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }

    private enum TipoAviso {
        SUCCESS,
        INFO,
        ERROR
    }
}
