package co.monitorsiae.ui;

import co.monitorsiae.app.CoordinadorMonitores;
import co.monitorsiae.app.ServicioAplicacion;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
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
    private final VBox tarjetas = new VBox(14);
    private final VBox avisos = new VBox(8);
    private final Label resumen = new Label();
    private final Map<String, Label> estados = new HashMap<>();
    private final Map<String, Label> resultados = new HashMap<>();
    private final Map<String, Button> botonesInventario = new HashMap<>();

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
        VBox barra = new VBox(18);
        barra.getStyleClass().add("sidebar");
        barra.setPadding(new Insets(24, 16, 24, 16));
        Label marca = new Label("MONITOR\nSIAE");
        marca.getStyleClass().add("brand");
        Label version = new Label("CENTRO DE OPERACIONES");
        version.getStyleClass().add("sidebar-caption");
        Button inicio = new Button("⌂   Panel principal");
        inicio.getStyleClass().add("nav-selected");
        inicio.setMaxWidth(Double.MAX_VALUE);
        Button configuracion = new Button("⚙   Configuración");
        configuracion.getStyleClass().add("nav-button");
        configuracion.setMaxWidth(Double.MAX_VALUE);
        configuracion.setOnAction(event -> abrirConfiguracion());
        VBox.setVgrow(version, Priority.ALWAYS);
        Label portal = new Label("PORTAL REAL\nPendiente · Fase 7");
        portal.getStyleClass().add("sidebar-note");
        barra.getChildren().addAll(marca, version, inicio, configuracion, portal);
        setLeft(barra);
    }

    private void crearContenido() {
        VBox cuerpo = new VBox(20);
        cuerpo.setPadding(new Insets(28, 34, 30, 34));
        HBox encabezado = new HBox(16);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox titulos = new VBox(4);
        Label saludo = new Label("Monitoreo");
        saludo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Administra tus clientes y revisa su estado en tiempo real.");
        subtitulo.getStyleClass().add("muted");
        titulos.getChildren().addAll(saludo, subtitulo);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        Button reconocerTodas = new Button("Reconocer todas");
        reconocerTodas.getStyleClass().add("secondary-button");
        reconocerTodas.setOnAction(event -> alertas.reconocerTodas());
        Button probar = new Button("Probar alarma");
        probar.getStyleClass().add("secondary-button");
        probar.setOnAction(event -> probarAlarma());
        encabezado.getChildren().addAll(titulos, reconocerTodas, probar);

        HBox modo = new HBox(10);
        modo.getStyleClass().add("environment-banner");
        Label punto = new Label("●");
        punto.getStyleClass().add("banner-dot");
        Label modoTexto = new Label("Modo simulador · La conexión con SIAE real se habilitará en la fase 7.");
        modoTexto.getStyleClass().add("banner-text");
        modo.getChildren().addAll(punto, modoTexto);

        resumen.getStyleClass().add("section-title");
        avisos.setFillWidth(true);
        tarjetas.setFillWidth(true);
        ScrollPane desplazamiento = new ScrollPane(tarjetas);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);
        cuerpo.getChildren().addAll(encabezado, modo, avisos, resumen, desplazamiento);
        setCenter(cuerpo);
    }

    public void refrescarConfiguracion() {
        Configuracion configuracion = servicio.configuracion();
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
        sesion.setDisable(true);
        sesion.setTooltip(new Tooltip("Disponible al integrar Playwright con el portal real en la fase 7."));
        Button iniciar = new Button("Iniciar / reanudar");
        iniciar.getStyleClass().add("primary-button");
        iniciar.setDisable(!cliente.activo() || !servicio.problemasConfiguracion().isEmpty());
        iniciar.setOnAction(event -> iniciar(cliente));
        Button detener = new Button("Detener");
        detener.getStyleClass().add("secondary-button");
        detener.setOnAction(event -> monitores.detener(cliente.id()));
        Button reconocer = new Button("Reconocer alerta");
        reconocer.getStyleClass().add("secondary-button");
        reconocer.setOnAction(event -> alertas.reconocer(cliente.id()));
        Button verInventario = new Button("Historial");
        verInventario.getStyleClass().add("secondary-button");
        verInventario.setTooltip(new Tooltip("Consultar los snapshots locales del inventario analizado."));
        verInventario.setDisable(monitores.ultimoSnapshot(cliente.id()).isEmpty());
        verInventario.setOnAction(event -> mostrarHistorial(cliente.id()));
        botonesInventario.put(cliente.id(), verInventario);
        acciones.getChildren().addAll(sesion, iniciar, detener, reconocer, verInventario);
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

    private void abrirConfiguracion() {
        if (!autorizarEdicion()) {
            return;
        }
        new EditorConfiguracion(propietario, servicio, alGuardar).showAndWait();
        refrescarConfiguracion();
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
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}
