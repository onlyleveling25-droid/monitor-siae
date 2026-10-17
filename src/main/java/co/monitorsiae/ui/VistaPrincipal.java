package co.monitorsiae.ui;

import co.monitorsiae.app.CoordinadorMonitores;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.monitoreo.SnapshotInventario;
import co.monitorsiae.notificacion.Alerta;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.Gravedad;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Vista operativa. Los casos de uso y las reglas permanecen fuera de los controles JavaFX.
 */
public final class VistaPrincipal extends BorderPane {

    private final Window propietario;
    private final ServicioAplicacion servicio;
    private final CoordinadorMonitores monitores;
    private final CentralAlertas alertas;
    private final Consumer<Configuracion> alGuardar;
    private final VBox barraLateral = new VBox(18);
    private final VBox tarjetas = new VBox(14);
    private final VBox avisos = new VBox(8);
    private final HBox avisoVisual = new HBox(10);
    private final Label iconoAviso = new Label();
    private final Label textoAviso = new Label();
    private final Label resumen = new Label();
    private final Label modoTexto = new Label();
    private final Label confirmacionReconocimiento = new Label();
    private final Map<String, Label> estados = new HashMap<>();
    private final Map<String, Label> resultados = new HashMap<>();
    private final Map<String, Button> botonesInventario = new HashMap<>();
    private boolean menuVisible = true;

    VistaPrincipal(Window propietario, ServicioAplicacion servicio, CoordinadorMonitores monitores,
                   CentralAlertas alertas, Consumer<Configuracion> alGuardar) {
        this.propietario = propietario;
        this.servicio = servicio;
        this.monitores = monitores;
        this.alertas = alertas;
        this.alGuardar = alGuardar;
        getStyleClass().add("app-root");
        crearBarraLateral();
        crearContenido();
        refrescarConfiguracion();
    }

    private void crearBarraLateral() {
        barraLateral.getStyleClass().add("sidebar");
        barraLateral.setPadding(new Insets(24, 16, 24, 16));
        HBox marca = new HBox(11);
        marca.setAlignment(Pos.CENTER_LEFT);
        StackPane insignia = new StackPane();
        insignia.getStyleClass().add("app-mark");
        Label letra = new Label("M");
        letra.getStyleClass().add("app-mark-letter");
        insignia.getChildren().add(letra);
        VBox identidad = new VBox(3);
        Label nombre = new Label("Monitor SIAE");
        nombre.getStyleClass().add("brand");
        Label version = new Label("CENTRO DE OPERACIONES");
        version.getStyleClass().add("sidebar-caption");
        identidad.getChildren().addAll(nombre, version);
        marca.getChildren().addAll(insignia, identidad);
        Button inicio = new Button("⌂   Panel principal");
        inicio.getStyleClass().add("nav-selected");
        inicio.setMaxWidth(Double.MAX_VALUE);
        Button configuracion = new Button("⚙   Configuración");
        configuracion.getStyleClass().add("nav-button");
        configuracion.setMaxWidth(Double.MAX_VALUE);
        configuracion.setOnAction(event -> abrirConfiguracion());
        Region separador = new Region();
        VBox.setVgrow(separador, Priority.ALWAYS);
        Label portal = new Label("PORTAL REAL\nSesión Playwright en validación");
        portal.getStyleClass().add("sidebar-note");
        portal.setWrapText(true);
        barraLateral.getChildren().addAll(marca, inicio, configuracion, separador, portal);
        setLeft(barraLateral);
    }

    private void crearContenido() {
        VBox cuerpo = new VBox(20);
        cuerpo.setPadding(new Insets(28, 34, 30, 34));
        HBox encabezado = new HBox(16);
        encabezado.getStyleClass().add("page-header");
        encabezado.setAlignment(Pos.CENTER_LEFT);
        Button menu = new Button("☰");
        menu.getStyleClass().add("icon-button");
        menu.setTooltip(new Tooltip("Ocultar menú"));
        menu.setAccessibleText("Alternar visibilidad del menú lateral");
        menu.setOnAction(event -> alternarMenu(menu));
        VBox titulos = new VBox(4);
        Label saludo = new Label("Centro de monitoreo");
        saludo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Controla tus perfiles y consulta su actividad desde un solo lugar.");
        subtitulo.getStyleClass().add("muted");
        titulos.getChildren().addAll(saludo, subtitulo);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        Button reconocerTodas = new Button("Reconocer todas");
        reconocerTodas.getStyleClass().add("secondary-button");
        reconocerTodas.setTooltip(new Tooltip(
                "Silencia y cierra todas las notificaciones activas; no detiene el monitoreo."));
        reconocerTodas.setOnAction(event -> reconocerTodas());
        Button probar = new Button("Probar alarma");
        probar.getStyleClass().add("secondary-button");
        probar.setOnAction(event -> probarAlarma());
        encabezado.getChildren().addAll(menu, titulos, reconocerTodas, probar);

        HBox modo = new HBox(10);
        modo.getStyleClass().add("environment-banner");
        Label punto = new Label("●");
        punto.getStyleClass().add("banner-dot");
        modoTexto.getStyleClass().add("banner-text");
        modo.getChildren().addAll(punto, modoTexto);

        iconoAviso.getStyleClass().add("visual-notice-icon");
        textoAviso.getStyleClass().add("visual-notice-text");
        textoAviso.setWrapText(true);
        HBox.setHgrow(textoAviso, Priority.ALWAYS);
        Button cerrarAviso = new Button("×");
        cerrarAviso.getStyleClass().add("visual-notice-close");
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
        cuerpo.getChildren().addAll(encabezado, avisoVisual, modo, avisos, confirmacionReconocimiento,
                resumen, desplazamiento);
        setCenter(cuerpo);
    }

    private void alternarMenu(Button boton) {
        menuVisible = !menuVisible;
        setLeft(menuVisible ? barraLateral : null);
        boton.setTooltip(new Tooltip(menuVisible ? "Ocultar menú" : "Mostrar menú"));
    }

    public void refrescarConfiguracion() {
        Configuracion configuracion = servicio.configuracion();
        modoTexto.setText(configuracion.entornoPortal() == EntornoPortal.SIMULADOR
                ? "Simulador · pruebas sin conexión ni acciones en SIAE."
                : "Portal real · sesión manual; monitoreo deshabilitado hasta validar selectores y acciones.");
        tarjetas.getChildren().clear();
        estados.clear();
        resultados.clear();
        botonesInventario.clear();
        resumen.setText("Clientes configurados  ·  " + configuracion.clientes().size());
        var problemas = servicio.problemasConfiguracion();
        avisos.getChildren().clear();
        if (!problemas.isEmpty()) {
            Label aviso = new Label("Completa la configuración antes de iniciar el monitoreo.");
            aviso.getStyleClass().add("validation-banner");
            aviso.setWrapText(true);
            avisos.getChildren().add(aviso);
        }
        if (configuracion.clientes().isEmpty()) {
            Label vacio = new Label("Aún no hay clientes. Abre Configuración para agregar el primero.");
            vacio.getStyleClass().add("empty-state");
            tarjetas.getChildren().add(vacio);
            return;
        }
        for (Cliente cliente : configuracion.clientes()) {
            tarjetas.getChildren().add(tarjeta(cliente));
        }
    }

    public void actualizarEstado(String clienteId, CoordinadorMonitores.Estado estado) {
        Label etiqueta = estados.get(clienteId);
        if (etiqueta == null) {
            return;
        }
        etiqueta.setText(textoEstado(estado));
        etiqueta.getStyleClass().removeAll("status-ready", "status-running", "status-stopped", "status-error",
                "status-alert");
        etiqueta.getStyleClass().add(claseEstado(estado));
        Label resultado = resultados.get(clienteId);
        if (resultado != null) {
            String ultimo = monitores.ultimoResultado(clienteId);
            resultado.setText(ultimo.isBlank() ? "Todavía no hay resultados." : ultimo);
        }
        Button inventario = botonesInventario.get(clienteId);
        if (inventario != null) {
            inventario.setDisable(monitores.ultimoSnapshot(clienteId).isEmpty());
        }
        if (servicio.configuracion().entornoPortal() == EntornoPortal.PORTAL_REAL) {
            switch (estado) {
                case ABRIENDO_SESION -> mostrarAvisoVisual("Abriendo el navegador para este perfil.",
                        TipoAviso.INFO);
                case SESION_ABIERTA -> mostrarAvisoVisual(
                        "Navegador abierto. Completa el inicio de sesión manualmente en Edge o Chrome.",
                        TipoAviso.SUCCESS);
                case DETENIDO -> mostrarAvisoVisual("La sesión del navegador se cerró.", TipoAviso.INFO);
                case ERROR -> mostrarAvisoVisual(
                        "No se pudo abrir el navegador. Revisa el resultado en la tarjeta del perfil.",
                        TipoAviso.ERROR);
                default -> {
                }
            }
        }
    }

    private VBox tarjeta(Cliente cliente) {
        VBox tarjeta = new VBox(12);
        tarjeta.getStyleClass().add("client-card");
        HBox fila = new HBox(12);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox identidad = new VBox(5);
        Label nombre = new Label(cliente.nombre().isBlank() ? "Cliente sin nombre" : cliente.nombre());
        nombre.getStyleClass().add("client-name");
        Label detalle = new Label(cliente.activo() ? "Activo · " + cliente.modo().name()
                : "Inactivo · habilítalo desde configuración");
        detalle.getStyleClass().add("muted");
        identidad.getChildren().addAll(nombre, detalle);
        HBox.setHgrow(identidad, Priority.ALWAYS);
        Label estado = new Label(textoEstado(monitores.estado(cliente.id())));
        estado.getStyleClass().addAll("status-pill", claseEstado(monitores.estado(cliente.id())));
        estados.put(cliente.id(), estado);
        fila.getChildren().addAll(identidad, estado);

        Label seccional = new Label("Preferencias: "
                + (cliente.tramite().seccionales().isEmpty()
                ? "sin configurar" : String.join(" · ", cliente.tramite().seccionales())));
        seccional.getStyleClass().add("muted");
        Label resultado = new Label(monitores.ultimoResultado(cliente.id()).isBlank()
                ? "Todavía no hay resultados." : "Último ciclo · " + monitores.ultimoResultado(cliente.id()));
        resultado.getStyleClass().add("muted");
        resultado.setWrapText(true);
        resultados.put(cliente.id(), resultado);

        HBox acciones = new HBox(9);
        Button sesion = new Button("Abrir sesión");
        sesion.getStyleClass().add("secondary-button");
        boolean portalReal = servicio.configuracion().entornoPortal() == EntornoPortal.PORTAL_REAL;
        sesion.setDisable(!portalReal);
        sesion.setTooltip(new Tooltip(portalReal
                ? "Abre el navegador de este cliente; inicia sesión y resuelve el captcha manualmente."
                : "Selecciona Portal real en Configuración para abrir una sesión."));
        sesion.setOnAction(event -> abrirSesion(cliente));
        Button iniciar = new Button("Iniciar / reanudar");
        iniciar.getStyleClass().add("primary-button");
        iniciar.setDisable(!cliente.activo() || !servicio.problemasConfiguracion().isEmpty() || portalReal);
        if (portalReal) {
            iniciar.setTooltip(new Tooltip("El monitoreo real se habilitará después de validar los selectores "
                    + "y el flujo de apartado del portal."));
        }
        iniciar.setOnAction(event -> iniciar(cliente));
        Button detener = new Button("Detener");
        detener.getStyleClass().add("secondary-button");
        detener.setOnAction(event -> monitores.detener(cliente.id()));
        Button verInventario = new Button("Historial");
        verInventario.getStyleClass().add("secondary-button");
        verInventario.setTooltip(new Tooltip("Consultar los snapshots locales del inventario analizado."));
        verInventario.setDisable(monitores.ultimoSnapshot(cliente.id()).isEmpty());
        verInventario.setOnAction(event -> mostrarHistorial(cliente.id()));
        botonesInventario.put(cliente.id(), verInventario);
        acciones.getChildren().addAll(sesion, iniciar, detener, verInventario);
        tarjeta.getChildren().addAll(fila, seccional, resultado, acciones);
        return tarjeta;
    }

    private void iniciar(Cliente cliente) {
        if (!servicio.problemasConfiguracion().isEmpty()) {
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

    private void abrirSesion(Cliente cliente) {
        try {
            monitores.abrirSesion(cliente);
        } catch (RuntimeException e) {
            mostrar(Alert.AlertType.ERROR, "No se pudo abrir la sesión", e.getMessage());
        }
    }

    private void abrirConfiguracion() {
        if (!autorizarEdicion()) {
            return;
        }
        boolean guardada = new EditorConfiguracion(propietario, servicio, alGuardar).showAndWait();
        refrescarConfiguracion();
        if (guardada) {
            mostrarAvisoVisual("Configuración guardada correctamente.", TipoAviso.SUCCESS);
        }
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
        iconoAviso.setText(switch (tipo) {
            case SUCCESS -> "✓";
            case INFO -> "i";
            case ERROR -> "!";
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
        List<OpcionSnapshot> opciones = monitores.historialSnapshots(clienteId).stream()
                .map(OpcionSnapshot::new)
                .toList();
        if (opciones.isEmpty()) {
            return;
        }
        if (opciones.size() == 1) {
            mostrarSnapshot(opciones.getFirst().snapshot());
            return;
        }
        ChoiceDialog<OpcionSnapshot> dialogo = new ChoiceDialog<>(opciones.getFirst(), opciones);
        dialogo.initOwner(propietario);
        EstilosJavaFx.aplicar(dialogo.getDialogPane());
        dialogo.setTitle("Historial de inventarios");
        dialogo.setHeaderText("Selecciona un snapshot para revisar la decisión");
        dialogo.setContentText("Inventarios guardados localmente:");
        dialogo.showAndWait().ifPresent(opcion -> mostrarSnapshot(opcion.snapshot()));
    }

    private void mostrarSnapshot(SnapshotInventario snapshot) {
        TextArea contenido = new TextArea(textoSnapshot(snapshot));
        contenido.setEditable(false);
        contenido.setWrapText(true);
        contenido.setPrefColumnCount(72);
        contenido.setPrefRowCount(22);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.initOwner(propietario);
        EstilosJavaFx.aplicar(alerta.getDialogPane());
        alerta.setTitle("Inventario analizado");
        alerta.setHeaderText("Evidencia de la elección del sistema");
        alerta.getDialogPane().setContent(contenido);
        alerta.showAndWait();
    }

    private static String textoSnapshot(SnapshotInventario snapshot) {
        String fecha = fechaSnapshot(snapshot);
        var eleccion = snapshot.eleccion();
        String motivo = switch (eleccion.motivo()) {
            case OBJETIVO -> "Objetivo prioritario #" + (eleccion.prioridad() + 1);
            case ALTERNATIVA -> "Alternativa #" + (eleccion.prioridad() + 1);
            case CUALQUIERA_DEL_TIPO -> "Comodín por tipo";
        };
        StringBuilder texto = new StringBuilder()
                .append("Cliente: ").append(snapshot.clienteNombre()).append(System.lineSeparator())
                .append("Fecha: ").append(fecha).append(System.lineSeparator())
                .append("Seccional: ").append(snapshot.seccional()).append(System.lineSeparator())
                .append("Elección: ").append(eleccion.arma().descripcion()).append(System.lineSeparator())
                .append("Regla aplicada: ").append(motivo)
                .append(eleccion.criterio().isBlank() ? "" : " · " + eleccion.criterio())
                .append(System.lineSeparator())
                .append("Inventario observado (").append(snapshot.inventario().size()).append("):")
                .append(System.lineSeparator());
        snapshot.inventario().forEach(arma -> texto
                .append(arma.posicion() == eleccion.arma().posicion() ? "→ " : "  ")
                .append(arma.posicion() + 1).append(". ")
                .append(arma.descripcion()).append(System.lineSeparator()));
        return texto.toString();
    }

    private static String fechaSnapshot(SnapshotInventario snapshot) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
                .withZone(ZoneId.systemDefault())
                .format(snapshot.registradoEn());
    }

    private record OpcionSnapshot(SnapshotInventario snapshot) {
        @Override
        public String toString() {
            return fechaSnapshot(snapshot) + " · " + snapshot.seccional() + " · "
                    + snapshot.eleccion().arma().descripcion();
        }
    }

    private static String textoEstado(CoordinadorMonitores.Estado estado) {
        return switch (estado) {
            case LISTO -> "Listo";
            case ABRIENDO_SESION -> "Abriendo navegador";
            case SESION_ABIERTA -> "Sesión abierta";
            case INICIANDO -> "Iniciando";
            case MONITOREANDO -> "Monitoreando · simulador";
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
            case SESION_ABIERTA -> "status-running";
            case MONITOREANDO -> "status-running";
            case ALERTA_ENCONTRADA -> "status-alert";
            case ARMA_APARTADA -> "status-running";
            case ERROR -> "status-error";
            case FUERA_DE_HORARIO -> "status-stopped";
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
