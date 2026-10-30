package co.monitorsiae.portal.real;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.DireccionTenencia;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.dominio.Texto;
import co.monitorsiae.portal.OpcionNoDisponibleException;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalException;
import co.monitorsiae.portal.PortalInesperadoException;
import co.monitorsiae.portal.PortalNoRespondeException;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.ResultadoSolicitud;
import co.monitorsiae.portal.SesionPortalCerradaException;
import co.monitorsiae.portal.SesionVencidaException;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PlaywrightPortalDriver implements PortalDriver {

    public static final String URL_TRAMITE = "https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite";
    public static final String URL_ESTADO_TRAMITE = "https://dccae.cgfm.mil.co/dccae/main/EstadoTramite";
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private static final Duration ESPERA_CORTA = Duration.ofSeconds(6);
    private static final Duration ESPERA_MEDIA = Duration.ofSeconds(20);
    private static final Duration ESPERA_LARGA = Duration.ofSeconds(30);
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(PlaywrightPortalDriver.class);

    private final Playwright playwright;
    private final BrowserContext contexto;
    private final Path capturas;
    /** Pagina activa. Cambia si el portal abre el inventario en una pestana nueva. */
    private volatile Page pagina;
    private volatile boolean cerrado;
    private volatile boolean cerrando;
    private volatile Runnable accionAlCerrar = () -> { };
    /** Motivo por el que el servidor no contesto al ultimo GUARDAR; nulo si respondio bien. */
    private volatile String fallaAlGuardar;
    /** El ultimo apartado se confirmo directamente en el cuadro "Ver armas" (sin el cuadro "Esta seguro"). */
    private volatile boolean cierreConfirmadoAlApartar;
    /** El inventario que se esta trabajando se abrio desde "Estado de tramite", no desde el paso 1. */
    private volatile boolean desdeEstadoTramite;
    /** Número de la solicitud que se abrió por última vez en "Estado de tramite"; vacio si no se sabe. */
    private volatile String numeroSolicitudAbierta = "";

    public PlaywrightPortalDriver(Path perfil, NavegadorPreferido navegador, Path capturas) {
        this.capturas = capturas;
        // Se usa el Chrome o Edge ya instalado (canal): no hace falta que Playwright descargue
        // su propio Chromium (~170 MB) la primera vez, y asi funciona sin acceso a su CDN.
        Playwright runtime = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        BrowserContext contextoCreado;
        Page paginaCreada;
        try {
            BrowserType.LaunchPersistentContextOptions opciones =
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel(canal(navegador))
                            .setHeadless(false)
                            .setViewportSize((ViewportSize) null)
                            .setArgs(List.of("--start-maximized", "--no-first-run",
                                    "--no-default-browser-check", "--disable-background-networking"));
            contextoCreado = ReintentoArranque.ejecutar(
                    () -> runtime.chromium().launchPersistentContext(perfil, opciones),
                    tiempo -> Thread.sleep(tiempo.toMillis()));
            paginaCreada = contextoCreado.pages().isEmpty()
                    ? contextoCreado.newPage()
                    : contextoCreado.pages().getFirst();
            paginaCreada.navigate(URL_TRAMITE,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        } catch (RuntimeException e) {
            try {
                runtime.close();
            } catch (RuntimeException cierre) {
                e.addSuppressed(cierre);
            }
            if (ReintentoArranque.seCerroAlArrancar(e)) {
                throw new PortalException("El navegador se cerró al arrancar, incluso al reintentar. "
                        + "Si se está actualizando, espera un minuto; si no, cierra todas sus ventanas "
                        + "y vuelve a intentar.", e);
            }
            throw new PortalException("No se pudo abrir el navegador o el portal SIAE. "
                    + "Verifica que el navegador seleccionado esté instalado.", e);
        }
        playwright = runtime;
        contexto = contextoCreado;
        pagina = paginaCreada;
        contextoCreado.onRequestFailed(peticion -> {
            if (esGuardarPaso1(peticion.url())) {
                fallaAlGuardar = "la petición no llegó a completarse";
            }
        });
        contextoCreado.onResponse(respuesta -> {
            if (!esGuardarPaso1(respuesta.url())) {
                return;
            }
            // Un reintento de la pagina que si responde anula el fallo anterior.
            fallaAlGuardar = respuesta.status() >= 500 ? "el servidor devolvió error " + respuesta.status() : null;
        });
        contextoCreado.onClose(ignorado -> registrarCierre());
        paginaCreada.onClose(ignorado -> registrarCierre());
    }

    private static String canal(NavegadorPreferido navegador) {
        return switch (navegador) {
            case CHROME -> "chrome";
            case EDGE -> "msedge";
        };
    }

    // --- Paso 1 ------------------------------------------------------------------

    @Override
    public List<String> abrirFormulario(ParametrosTramite tramite) {
        verificarDisponible();
        desdeEstadoTramite = false;
        asegurarPaso1();
        seleccionarSiHaceFalta(SelectoresSiae.PLACEHOLDER_PERMISO, tramite.obtenerPermiso(),
                "como desea obtener el permiso");
        seleccionarSiHaceFalta(SelectoresSiae.PLACEHOLDER_TIPO_TRAMITE, tramite.tipoTramite(),
                "tipo de trámite");
        return leerOpciones(SelectoresSiae.PLACEHOLDER_SECCIONAL);
    }

    @Override
    public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
        verificarDisponible();
        elegirCombo(SelectoresSiae.PLACEHOLDER_SECCIONAL, seccional, "seccional");

        Locator campo = pagina.locator(SelectoresSiae.CAMPO_DESCRIPCION).first();
        campo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        campo.fill(descripcion);

        Locator guardar = pagina.locator(SelectoresSiae.BOTON_GUARDAR).first();
        esperarGuardarHabilitado(guardar);
        fallaAlGuardar = null;
        guardar.click();
        return esperarResultadoGuardar();
    }

    private static boolean esGuardarPaso1(String url) {
        return url != null && url.contains(SelectoresSiae.RUTA_GUARDAR_PASO_1);
    }

    private void asegurarPaso1() {
        detectarSesionVencida();
        if (pagina.locator(SelectoresSiae.CAMPO_DESCRIPCION).count() > 0) {
            return;
        }
        pagina.navigate(URL_TRAMITE,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        esperarPaso1();
    }

    private void esperarPaso1() {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (pagina.locator(SelectoresSiae.CAMPO_DESCRIPCION).count() > 0) {
                return;
            }
            pagina.waitForTimeout(250);
        }
        throw inesperado("No se abrió el formulario del paso 1.", null);
    }

    private void seleccionarSiHaceFalta(String placeholder, String valor, String nombre) {
        Locator combo = combo(placeholder);
        combo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        if (Texto.equivalentes(combo.inputValue(), valor)) {
            return;
        }
        elegirCombo(placeholder, valor, nombre);
    }

    private void elegirCombo(String placeholder, String valor, String nombre) {
        Locator combo = combo(placeholder);
        combo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        combo.click();

        List<Locator> utiles = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            utiles.clear();
            textos.clear();
            for (Locator opcion : pagina.locator(SelectoresSiae.OPCIONES).all()) {
                String texto = textoDe(opcion);
                if (esRuido(texto)) {
                    continue;
                }
                utiles.add(opcion);
                textos.add(texto);
            }
            if (!textos.isEmpty()) {
                break;
            }
            pagina.waitForTimeout(250);
        }

        int indice = indiceQueCoincide(textos, valor);
        if (indice < 0) {
            pagina.keyboard().press("Escape");
            throw new OpcionNoDisponibleException("No apareció \"" + valor + "\" en la lista de "
                    + nombre + ". Opciones vistas: " + String.join(" | ", textos));
        }
        utiles.get(indice).click();
        pagina.waitForTimeout(400);
    }

    private List<String> leerOpciones(String placeholder) {
        Locator combo = combo(placeholder);
        combo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        combo.click();

        List<String> textos = new ArrayList<>();
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            textos.clear();
            for (Locator opcion : pagina.locator(SelectoresSiae.OPCIONES).all()) {
                String texto = textoDe(opcion);
                if (!esRuido(texto)) {
                    textos.add(texto);
                }
            }
            if (!textos.isEmpty()) {
                break;
            }
            pagina.waitForTimeout(250);
        }
        pagina.keyboard().press("Escape");
        return textos;
    }

    private Locator combo(String placeholder) {
        return pagina.getByPlaceholder(placeholder).first();
    }

    private void esperarGuardarHabilitado(Locator guardar) {
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            if (guardar.isEnabled()) {
                return;
            }
            pagina.waitForTimeout(200);
        }
        if (hayAvisoNoDisponible()) {
            throw new OpcionNoDisponibleException("El portal no habilita GUARDAR ahora: "
                    + avisoNoDisponible());
        }
        throw inesperado("GUARDAR sigue deshabilitado con el formulario completo. "
                + contadorDescripcion(), null);
    }

    private boolean hayAvisoNoDisponible() {
        return pagina.locator(SelectoresSiae.AVISO_NO_DISPONIBLE).count() > 0;
    }

    private String avisoNoDisponible() {
        return textoDe(pagina.locator(SelectoresSiae.AVISO_NO_DISPONIBLE).first());
    }

    private String contadorDescripcion() {
        Locator contador = pagina.locator(SelectoresSiae.CONTADOR_DESCRIPCION).first();
        return contador.count() == 0 ? "Sin contador visible." : textoDe(contador);
    }

    /** GUARDAR puede dar tres desenlaces: sin inventario, pantalla intermedia, o FG-1 directo. */
    private ResultadoGuardar esperarResultadoGuardar() {
        long limite = System.nanoTime() + ESPERA_LARGA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (hayMensajeSinInventario()) {
                return ResultadoGuardar.SIN_INVENTARIO;
            }
            if (fg1Abierto() || hayEnlaceInventario()) {
                return ResultadoGuardar.AVANZO_PASO_2;
            }
            pagina.waitForTimeout(300);
        }
        String falla = fallaAlGuardar;
        if (falla != null) {
            // Es la plataforma la que no contesta (p. ej. 504 del gateway), no una pantalla nueva.
            throw new PortalNoRespondeException("GUARDAR no tuvo respuesta: " + falla + ".");
        }
        throw inesperado("No se reconoció la respuesta del portal después de GUARDAR.", null);
    }

    // --- Paso 2 ------------------------------------------------------------------

    /** Pulsa el enlace de la pantalla intermedia y sigue la ventana si se abre aparte. */
    private void asegurarPaso2() {
        if (fg1Abierto()) {
            return;
        }
        Locator enlace = pagina.locator(SelectoresSiae.ENLACE_INVENTARIO).first();
        if (enlace.count() == 0) {
            throw inesperado("El portal no mostró el enlace al inventario.", null);
        }
        enlace.scrollIntoViewIfNeeded();
        Set<Page> antes = new HashSet<>(contexto.pages());
        enlace.click();

        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            for (Page candidata : contexto.pages()) {
                if (!antes.contains(candidata) && !candidata.isClosed()) {
                    adoptarPagina(candidata);
                    break;
                }
            }
            if (fg1Abierto()) {
                return;
            }
            pagina.waitForTimeout(250);
        }
        throw inesperado("No se abrió la ventana del inventario.", null);
    }

    private void adoptarPagina(Page nueva) {
        try {
            nueva.waitForLoadState();
        } catch (PlaywrightException ignorado) {
            // la ventana puede estar cargando todavia
        }
        nueva.onClose(ignorado -> registrarCierre());
        pagina = nueva;
        LOG.info("El inventario se abrió en una ventana aparte: {}", nueva.url());
    }

    @Override
    public List<ArmaInventario> leerInventario() {
        verificarDisponible();
        detectarSesionVencida();
        asegurarPaso2();
        esperarFilasInventario();

        List<ArmaInventario> delGrid = leerGridCompleto();
        if (!delGrid.isEmpty()) {
            return delGrid;
        }

        // Respaldo para tablas clasicas, sin virtualizacion.
        List<ArmaInventario> armas = new ArrayList<>();
        List<Locator> filas = pagina.locator(SelectoresSiae.FILAS_INVENTARIO).all();
        for (int i = 0; i < filas.size(); i++) {
            Locator fila = filas.get(i);
            List<Locator> celdas = fila.locator(SelectoresSiae.CELDAS_INVENTARIO).all();
            List<String> textos = new ArrayList<>();
            for (Locator celda : celdas) {
                textos.add(textoDe(celda));
            }
            String descripcion = textos.isEmpty() ? textoDe(fila) : String.join(" ", textos);
            if (descripcion.isBlank()) {
                continue;
            }
            String tipo = textos.isEmpty() ? "" : textos.getFirst();
            armas.add(new ArmaInventario(i, descripcion, tipo));
        }
        return armas;
    }

    private record FilaGrid(int posicion, List<String> celdas) {
    }

    /**
     * El DataGrid solo dibuja las filas visibles (mas un margen), asi que con unas 5 a la vista se
     * leerian 8 de 19 armas. Se recorre el desplazamiento completo y se junta cada fila una sola vez.
     */
    private List<ArmaInventario> leerGridCompleto() {
        Locator grid = pagina.locator(SelectoresSiae.GRID_INVENTARIO).first();
        if (grid.count() == 0) {
            return List.of();
        }
        try {
            String json = String.valueOf(grid.evaluate(JS_LEER_GRID));
            List<FilaGrid> filas = JSON.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<FilaGrid>>() { });
            List<ArmaInventario> armas = new ArrayList<>();
            for (FilaGrid fila : filas) {
                String descripcion = String.join(" ", fila.celdas()).replaceAll("\\s+", " ").strip();
                if (!descripcion.isBlank()) {
                    String tipo = fila.celdas().isEmpty() ? "" : fila.celdas().getFirst();
                    armas.add(new ArmaInventario(fila.posicion(), descripcion, tipo));
                }
            }
            return armas;
        } catch (IOException | RuntimeException e) {
            LOG.warn("No se pudo leer el listado completo del DataGrid; se usa la lectura simple.", e);
            return List.of();
        }
    }

    private static final String JS_LEER_GRID = """
            async (grid) => {
              const pausa = ms => new Promise(r => setTimeout(r, ms));
              const limpiar = t => (t || '').replace(/\\s+/g, ' ').trim();
              const scroller = grid.querySelector('.MuiDataGrid-virtualScroller');
              const total = Math.max(0, (parseInt(grid.getAttribute('aria-rowcount') || '0', 10) || 0) - 1);
              const filas = new Map();
              const recoger = () => {
                grid.querySelectorAll('.MuiDataGrid-row').forEach(f => {
                  const idx = f.getAttribute('data-rowindex');
                  const clave = idx !== null ? 'i' + idx : (f.getAttribute('data-id') || 'n' + filas.size);
                  if (!filas.has(clave)) {
                    const celdas = Array.from(f.querySelectorAll('.MuiDataGrid-cell')).map(c => limpiar(c.innerText));
                    filas.set(clave, { posicion: idx !== null ? parseInt(idx, 10) : filas.size, celdas: celdas });
                  }
                });
              };
              if (scroller) { scroller.scrollTop = 0; await pausa(120); }
              recoger();
              let vueltas = 0;
              while (scroller && (total === 0 || filas.size < total) && vueltas < 80) {
                const antes = scroller.scrollTop;
                scroller.scrollTop = antes + Math.max(40, scroller.clientHeight * 0.7);
                await pausa(120);
                recoger();
                vueltas++;
                if (scroller.scrollTop === antes) break;
              }
              if (scroller) { scroller.scrollTop = 0; }
              return JSON.stringify(Array.from(filas.values()).sort((a, b) => a.posicion - b.posicion));
            }
            """;

    @Override
    public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
        return apartar(arma, modalidad, DireccionTenencia.vacia());
    }

    @Override
    public boolean apartar(ArmaInventario arma, ReglasSeleccion reglas) {
        return apartar(arma, reglas.modalidad(), reglas.direccionTenencia());
    }

    private boolean apartar(ArmaInventario arma, Modalidad modalidad, DireccionTenencia direccion) {
        verificarDisponible();
        detectarSesionVencida();
        if (modalidad == Modalidad.TENENCIA && !direccion.completa()) {
            throw new PortalException("La modalidad TENENCIA exige departamento, ciudad y dirección "
                    + "completa; revisa la configuración del cliente.");
        }
        cierreConfirmadoAlApartar = false;
        try {
            return elegirYConfirmar(arma, modalidad, direccion);
        } catch (PlaywrightException e) {
            if (pagina.isClosed()) {
                throw e;   // el navegador se cerro: lo trata quien vigila la sesion
            }
            // Un paso que no respondio como se esperaba no debe tumbar el monitoreo: se informa
            // con captura y el siguiente ciclo cierra cualquier cuadro que haya quedado abierto.
            throw inesperado("Falló un paso al apartar el arma: " + primeraLinea(e.getMessage()), e);
        }
    }

    private boolean elegirYConfirmar(ArmaInventario arma, Modalidad modalidad, DireccionTenencia direccion) {
        asegurarPaso2();

        Locator fila = buscarFila(arma);
        if (fila == null) {
            return false;   // se la llevaron entre la lectura y el clic
        }
        fila.scrollIntoViewIfNeeded();
        fila.click();
        esperarFilaSeleccionada(arma);

        String nombreBoton = modalidad == Modalidad.PORTE ? "PORTE" : "TENENCIA";
        Locator boton = pagina.locator(modalidad == Modalidad.PORTE
                ? SelectoresSiae.BOTON_PORTE
                : SelectoresSiae.BOTON_TENENCIA).first();
        boolean hayBoton = esperarBoton(boton);
        if (hayBoton) {
            boton.click();
        } else if (!desdeEstadoTramite) {
            throw inesperado("El botón " + nombreBoton + " no está disponible después de elegir el arma.", null);
        } else {
            LOG.info("En Estado de trámite no hay botón {}: se sigue con FINALIZAR del listado.", nombreBoton);
        }

        // Segun el modulo, el portal puede mostrar primero "¿Esta seguro de adquirir el siguiente arma?"
        // o ir directo a "¿Desea confirmar y continuar...?" (Ver armas): se atiende el que salga.
        Locator modal = hayBoton ? esperarCuadroTrasElegirArma(!desdeEstadoTramite) : null;
        if (modal == null) {
            // Solo desde Estado de tramite: sin cuadro intermedio, el arma se confirma con FINALIZAR.
            return finalizarDesdeElListadoYConfirmar(arma);
        }
        if (clasificar(modal) == Cuadro.CIERRE) {
            LOG.info("El portal pidió confirmar directamente el cuadro «Ver armas»; se confirma ahí.");
            confirmarEn(modal);
            cierreConfirmadoAlApartar = true;
            return esperarDesenlacePreasignacion(modal, arma);
        }
        if (modalidad == Modalidad.TENENCIA) {
            llenarDireccion(modal, direccion);  // CONFIRMAR con 17-modal-tenencia.html
        }
        confirmarEn(modal);
        return esperarDesenlacePreasignacion(modal, arma);
    }

    /**
     * Desde "Estado de tramite", cuando elegir el arma no abre el cuadro "Esta seguro", el camino es
     * FINALIZAR del listado y CONFIRMAR en "Ver armas", tanto para pre-asignar como para sellar.
     * Solo se pulsa FINALIZAR con una fila seleccionada y sin el aviso de "sin inventario" (con ese
     * aviso FINALIZAR cancela la solicitud); y solo se confirma el cuadro si es el de "Ver armas".
     */
    private boolean finalizarDesdeElListadoYConfirmar(ArmaInventario arma) {
        verificarArmaSeleccionada(arma);
        if (pagina.locator(SelectoresSiae.AVISO_SIN_INVENTARIO_EN_MODAL).count() > 0) {
            throw inesperado("No se pulsa FINALIZAR: el cuadro dice que no hay inventario.", null);
        }
        Locator finalizar = pagina.locator(SelectoresSiae.BOTON_FINALIZAR).first();
        if (finalizar.count() == 0) {
            throw inesperado("No se encontró FINALIZAR en el listado del inventario.", null);
        }
        Locator encima = ultimoModalVisible();
        if (encima != null && encima.locator(SelectoresSiae.BOTON_FINALIZAR).count() == 0) {
            throw inesperado("No se pulsa FINALIZAR: hay un cuadro desconocido sobre el listado («"
                    + recortar(textoDe(encima), 120) + "»).", null);
        }
        LOG.info("Sin cuadro intermedio al elegir el arma: se pulsa FINALIZAR del listado y se confirma «Ver armas».");
        finalizar.scrollIntoViewIfNeeded();
        finalizar.click();

        Locator modal = esperarModal();
        if (clasificar(modal) != Cuadro.CIERRE) {
            // Se lanza (no se devuelve falso) para que el ciclo no reintente pulsando FINALIZAR otra vez.
            throw inesperado("Tras FINALIZAR salió un cuadro que no es «Ver armas» («"
                    + recortar(textoDe(modal), 120) + "»); no se confirma.", null);
        }
        confirmarEn(modal);
        cierreConfirmadoAlApartar = true;
        return esperarDesenlacePreasignacion(modal, arma);
    }

    private enum Cuadro { ADQUISICION, CIERRE, DESCONOCIDO }

    /** Reconoce el cuadro por su texto: nunca se confirma uno que no se sepa que es. */
    private Cuadro clasificar(Locator modal) {
        String texto = Texto.normalizar(textoDe(modal));
        if (texto.contains("confirmar y continuar") || texto.contains("armas asignadas")) {
            return Cuadro.CIERRE;
        }
        if (texto.contains("esta seguro de adquirir") || texto.contains("siguiente arma")) {
            return Cuadro.ADQUISICION;
        }
        return Cuadro.DESCONOCIDO;
    }

    /**
     * Espera el cuadro de confirmacion del arma, ignorando el listado que sigue abierto debajo.
     *
     * @param obligatorio si es falso y no sale ninguno, devuelve nulo en lugar de fallar
     */
    private Locator esperarCuadroTrasElegirArma(boolean obligatorio) {
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        String visto = "";
        while (System.nanoTime() < limite) {
            List<Locator> modales = pagina.locator(SelectoresSiae.MODALES).all();
            for (int i = modales.size() - 1; i >= 0; i--) {
                Locator modal = modales.get(i);
                if (!estaVisible(modal)) {
                    continue;
                }
                if (clasificar(modal) != Cuadro.DESCONOCIDO) {
                    return modal;
                }
                visto = textoDe(modal);
            }
            pagina.waitForTimeout(150);
        }
        if (!obligatorio) {
            return null;
        }
        throw inesperado("Tras elegir el arma no apareció un cuadro de confirmación conocido"
                + (visto.isBlank() ? "." : " (se vio: «" + recortar(visto, 120) + "»)."), null);
    }

    /** El cuadro visible que esta mas arriba, o nulo si no hay ninguno. */
    private Locator ultimoModalVisible() {
        List<Locator> modales = pagina.locator(SelectoresSiae.MODALES).all();
        for (int i = modales.size() - 1; i >= 0; i--) {
            if (estaVisible(modales.get(i))) {
                return modales.get(i);
            }
        }
        return null;
    }

    private boolean esperarBoton(Locator boton) {
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            if (boton.count() > 0 && estaVisible(boton) && boton.isEnabled()) {
                return true;
            }
            pagina.waitForTimeout(150);
        }
        return false;
    }

    /** Resume el mensaje de un error de Playwright, que suele venir como "Error { message='...' ... }". */
    private static String primeraLinea(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replaceAll("\\s+", " ").replaceFirst("^Error \\{ ?(message=')?", "");
        return recortar(limpio, 160);
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto.replaceAll("\\s+", " ").strip();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo) + "…";
    }

    /**
     * Tras CONFIRMAR, el error de cupo es un snackbar que se autodestruye en segundos: se sondea
     * en bucle corto. Si el modal se cierra sin snackbar, la pre-asignacion quedo aceptada.
     */
    private boolean esperarDesenlacePreasignacion(Locator modal, ArmaInventario arma) {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        long modalCerradoDesde = 0;
        while (System.nanoTime() < limite) {
            if (hayErrorDeCupo()) {
                LOG.info("Sin cupo disponible para {}; se intentará con otra arma.", arma.descripcion());
                volverDelModal();
                return false;
            }
            if (!estaVisible(modal)) {
                if (modalCerradoDesde == 0) {
                    modalCerradoDesde = System.nanoTime();
                } else if (System.nanoTime() - modalCerradoDesde > Duration.ofSeconds(2).toNanos()) {
                    return true;   // modal cerrado y sin snackbar de cupo: aceptada
                }
            }
            pagina.waitForTimeout(120);
        }
        throw inesperado("El modal de confirmación no se cerró después de CONFIRMAR.", null);
    }

    @Override
    public boolean confirmarCierre() {
        verificarDisponible();
        detectarSesionVencida();

        if (cierreConfirmadoAlApartar) {
            LOG.info("El cuadro «Ver armas» ya se confirmó al apartar; no hay nada más que sellar.");
            cierreConfirmadoAlApartar = false;
            return true;
        }
        // Con el aviso de "sin inventario" a la vista, FINALIZAR CANCELARIA la solicitud: jamas se pulsa.
        if (pagina.locator(SelectoresSiae.AVISO_SIN_INVENTARIO_EN_MODAL).count() > 0) {
            LOG.warn("No se pulsa FINALIZAR: el cuadro dice que no hay inventario y cancelaría la solicitud.");
            return false;
        }
        Locator finalizar = pagina.locator(SelectoresSiae.BOTON_FINALIZAR).first();
        if (finalizar.count() == 0) {
            LOG.warn("No se encontró FINALIZAR; la reserva queda sin sellar.");
            return false;
        }
        try {
            finalizar.scrollIntoViewIfNeeded();
            finalizar.click();

            // Modal de cierre general: "¿Desea confirmar y continuar el proceso...?" con Ver Armas.
            Locator modal = esperarModal();
            if (clasificar(modal) != Cuadro.CIERRE) {
                LOG.warn("Tras FINALIZAR salió un cuadro que no es el de cierre («{}»); no se confirma.",
                        recortar(textoDe(modal), 120));
                return false;
            }
            confirmarEn(modal);
            return esperarAvancePaso3(modal);
        } catch (PlaywrightException e) {
            if (pagina.isClosed()) {
                throw e;
            }
            throw inesperado("Falló un paso al sellar el trámite: " + primeraLinea(e.getMessage()), e);
        }
    }

    /** Tras el CONFIRMAR de cierre, el portal avanza al paso 3 (Formulario antecedentes Fiscalia). */
    private boolean esperarAvancePaso3(Locator modal) {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        boolean modalCerrado = false;
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (hayPaso3()) {   // CONFIRMAR con 18-tras-confirmar.txt
                return true;
            }
            if (!estaVisible(modal)) {
                modalCerrado = true;
            }
            pagina.waitForTimeout(300);
        }
        if (modalCerrado) {
            LOG.warn("El modal de cierre se cerró pero no se detectó el paso 3; "
                    + "se asume reserva sellada. Verifícalo en la ventana del cliente.");
            return true;
        }
        return false;
    }

    /** CONFIRMAR del modal. Acotado al modal para no confundirlo con el del cierre general. */
    private void confirmarEn(Locator modal) {
        Locator confirmar = modal.locator(SelectoresSiae.BOTON_CONFIRMAR).first();
        confirmar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        confirmar.click();
        pagina.waitForTimeout(600);
    }

    /** Devuelve el modal visible más reciente, no el primero del DOM. */
    private Locator esperarModal() {
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            List<Locator> modales = pagina.locator(SelectoresSiae.MODALES).all();
            for (int i = modales.size() - 1; i >= 0; i--) {
                if (estaVisible(modales.get(i))) {
                    return modales.get(i);
                }
            }
            pagina.waitForTimeout(150);
        }
        throw inesperado("No apareció el cuadro de confirmación del portal.", null);
    }

    private void esperarModalCerrado(Locator modal) {
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            if (!estaVisible(modal)) {
                return;
            }
            pagina.waitForTimeout(150);
        }
    }

    private void volverDelModal() {
        Locator volver = pagina.locator(SelectoresSiae.BOTON_VOLVER).first();
        if (volver.count() > 0 && estaVisible(volver)) {
            volver.click();
            pagina.waitForTimeout(600);
        }
    }

    private void esperarFilaSeleccionada(ArmaInventario arma) {
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            if (haySeleccion(arma)) {
                return;
            }
            pagina.waitForTimeout(150);
        }
        throw inesperado("El clic en la fila no quedó registrado.", null);
    }

    /**
     * El portal confirma la selección de varias formas según el módulo: el contador "1 fila
     * seleccionada" del listado, la fila marcada o, en "Estado de trámite", un resumen con el arma
     * elegida. Basta cualquiera de las tres.
     */
    private boolean haySeleccion(ArmaInventario arma) {
        return pagina.locator(SelectoresSiae.FILA_SELECCIONADA).count() > 0
                || pagina.locator(SelectoresSiae.FILA_GRID_MARCADA).count() > 0
                || (desdeEstadoTramite && resumenMuestra(arma));
    }

    /** Busca el arma en una tabla del cuadro que no sea el propio listado: el resumen de la selección. */
    private boolean resumenMuestra(ArmaInventario arma) {
        for (Locator fila : pagina.locator(SelectoresSiae.FILAS_RESUMEN_SELECCION).all()) {
            if (Texto.contieneTermino(textoDe(fila), arma.descripcion())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Antes de un paso que no se puede deshacer, comprueba que lo seleccionado sea el arma elegida.
     * Si el portal marca otra fila, se detiene en lugar de confirmar un arma equivocada.
     */
    private void verificarArmaSeleccionada(ArmaInventario arma) {
        List<Locator> marcadas = pagina.locator(SelectoresSiae.FILA_GRID_MARCADA).all();
        for (Locator marcada : marcadas) {
            if (!Texto.contieneTermino(textoDe(marcada), arma.descripcion())) {
                throw inesperado("No se confirma: la fila seleccionada («" + recortar(textoDe(marcada), 120)
                        + "») no es el arma elegida («" + recortar(arma.descripcion(), 120) + "»).", null);
            }
        }
        if (marcadas.isEmpty() && !haySeleccion(arma)) {
            throw inesperado("No se confirma: no hay ningún arma seleccionada en el listado.", null);
        }
    }

    private boolean hayErrorDeCupo() {
        return pagina.locator(SelectoresSiae.ERROR_CUPO).count() > 0;
    }

    private boolean esperarAsignacionExitosa(Locator modal) {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (pagina.locator(SelectoresSiae.ASIGNACION_EXITOSA).count() > 0) {
                return true;
            }
            if (modal != null && !estaVisible(modal)) {
                return true;   // el modal se cerró: se aceptó
            }
            pagina.waitForTimeout(300);
        }
        return false;
    }

    /** CONFIRMAR: los dos desplegables del modal de TENENCIA y el campo de dirección. */
    private void llenarDireccion(Locator modal, DireccionTenencia direccion) {
        List<Locator> combos = modal.locator("mat-select, [role='combobox'], select").all();
        if (combos.size() >= 2) {
            elegirEnModal(combos.get(0), direccion.departamento(), "departamento");
            elegirEnModal(combos.get(1), direccion.ciudad(), "ciudad");
        } else {
            LOG.warn("El modal de TENENCIA no mostró los dos desplegables esperados.");
        }
        Locator campo = modal.locator("textarea, input[type='text']").last();
        if (campo.count() > 0) {
            campo.fill(direccion.direccion());
        }
    }

    private void elegirEnModal(Locator combo, String valor, String nombre) {
        combo.click();
        List<Locator> opciones = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        long limite = System.nanoTime() + ESPERA_CORTA.toNanos();
        while (System.nanoTime() < limite) {
            opciones.clear();
            textos.clear();
            for (Locator opcion : pagina.locator(SelectoresSiae.OPCIONES).all()) {
                String texto = textoDe(opcion);
                if (!esRuido(texto)) {
                    opciones.add(opcion);
                    textos.add(texto);
                }
            }
            if (!textos.isEmpty()) {
                break;
            }
            pagina.waitForTimeout(200);
        }
        int indice = indiceQueCoincide(textos, valor);
        if (indice < 0) {
            pagina.keyboard().press("Escape");
            throw inesperado("No apareció \"" + valor + "\" en el desplegable de " + nombre
                    + ". Opciones vistas: " + String.join(" | ", textos), null);
        }
        opciones.get(indice).click();
        pagina.waitForTimeout(400);
    }

    private Locator buscarFila(ArmaInventario arma) {
        Locator grid = pagina.locator(SelectoresSiae.GRID_INVENTARIO).first();
        if (grid.count() > 0) {
            Locator enGrid = buscarFilaEnGrid(grid, arma);
            if (enGrid != null) {
                return enGrid;
            }
        }
        List<Locator> filas = pagina.locator(SelectoresSiae.FILAS_INVENTARIO).all();
        for (Locator fila : filas) {
            String texto = textoDe(fila);
            if (Texto.equivalentes(texto, arma.descripcion())
                    || Texto.contieneTermino(texto, arma.descripcion())) {
                return fila;
            }
        }
        for (Locator fila : filas) {
            List<Locator> celdas = fila.locator(SelectoresSiae.CELDAS_INVENTARIO).all();
            if (celdas.size() < 3) {
                continue;
            }
            if (Texto.equivalentes(textoDe(celdas.getFirst()), arma.tipo())
                    && Texto.contieneTermino(textoDe(fila), arma.descripcion())) {
                return fila;
            }
        }
        return null;
    }

    /** Desplaza el DataGrid desde arriba hasta que la fila del arma este dibujada en pantalla. */
    private Locator buscarFilaEnGrid(Locator grid, ArmaInventario arma) {
        Locator desplazamiento = grid.locator(SelectoresSiae.SCROLL_GRID).first();
        boolean conScroll = desplazamiento.count() > 0;
        if (conScroll) {
            desplazamiento.evaluate("el => { el.scrollTop = 0; }");
            pagina.waitForTimeout(120);
        }
        for (int vuelta = 0; vuelta < 80; vuelta++) {
            for (Locator fila : grid.locator(SelectoresSiae.FILAS_GRID).all()) {
                String texto = textoDe(fila);
                if (Texto.equivalentes(texto, arma.descripcion())
                        || Texto.contieneTermino(texto, arma.descripcion())) {
                    return fila;
                }
            }
            if (!conScroll) {
                return null;
            }
            double antes = numero(desplazamiento.evaluate("el => el.scrollTop"));
            desplazamiento.evaluate("el => { el.scrollTop = el.scrollTop + Math.max(40, el.clientHeight * 0.7); }");
            pagina.waitForTimeout(120);
            if (numero(desplazamiento.evaluate("el => el.scrollTop")) == antes) {
                return null;
            }
        }
        return null;
    }

    private static double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0;
    }

    private void esperarFilasInventario() {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            if (pagina.locator(SelectoresSiae.FILAS_INVENTARIO).count() > 0) {
                return;
            }
            if (hayMensajeSinInventario()) {
                return;   // FG-1 abierto pero con la tabla vacía
            }
            pagina.waitForTimeout(300);
        }
        throw inesperado("El paso 2 no mostró filas de inventario.", null);
    }

    // --- Solicitud abierta en "Estado de tramite" ------------------------------------

    @Override
    public ResultadoSolicitud abrirSolicitudExistente(ParametrosTramite tramite, String numeroSolicitud) {
        verificarDisponible();
        detectarSesionVencida();
        String numero = numeroDeSolicitud(numeroSolicitud);

        // Si la pagina ya esta en Estado de tramite no se recarga: solo se repite la busqueda. Se
        // navega unicamente la primera vez o si la persona (o el portal) saco la pagina de ahi.
        if (!pagina.url().contains("/EstadoTramite")) {
            LOG.info("Abriendo Estado de trámite para buscar la solicitud ({}).", URL_ESTADO_TRAMITE);
            pagina.navigate(URL_ESTADO_TRAMITE,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        }
        esperarEstadoTramite();
        cerrarCuadroAbierto();

        numeroSolicitudAbierta = "";
        Locator tarjeta = esperarTarjetaDeSolicitud(tramite, numero);
        if (tarjeta == null) {
            // La lista solo se pide al cargar la página: una solicitud que el paso 1 acaba de crear puede
            // tardar unos segundos en existir y no estar en esa lista. Se recarga una vez y se espera de nuevo.
            LOG.info("La solicitud no apareció; se recarga Estado de trámite y se vuelve a buscar.");
            pagina.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            esperarEstadoTramite();
            tarjeta = esperarTarjetaDeSolicitud(tramite, numero);
        }
        if (tarjeta == null) {
            return ResultadoSolicitud.SIN_SOLICITUD;
        }
        String numeroDeLaTarjeta = numeroDeSolicitud(textoDe(tarjeta));
        // Solo se acepta si parece un número de solicitud; si no, se sigue buscando por permiso y tipo.
        numeroSolicitudAbierta = numeroDeLaTarjeta.matches("\\d+(?:-\\d+){2,}") ? numeroDeLaTarjeta : "";
        if (numero.isBlank() && !numeroSolicitudAbierta.isEmpty()) {
            LOG.info("Se vigila la solicitud {} (la primera en progreso del mismo trámite).", numeroSolicitudAbierta);
        }

        // La linea FG-1 queda en pantalla mientras la solicitud siga seleccionada: basta con pulsarla
        // otra vez. Si la pagina se recargo o hay otra solicitud seleccionada, se abre la tarjeta primero.
        Locator linea = pagina.locator(SelectoresSiae.FILA_FORMULARIO).first();
        if (!(tarjetaSeleccionada(tarjeta) && estaVisible(linea))) {
            LOG.info("Abriendo la solicitud en Estado de trámite.");
            tarjeta.scrollIntoViewIfNeeded();
            tarjeta.click();
            try {
                linea.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(ESPERA_MEDIA.toMillis()));
            } catch (PlaywrightException e) {
                throw inesperado("La solicitud no mostró la línea del formulario FG-1.", e);
            }
        }
        linea.locator("td").first().click();
        return esperarResultadoInventarioDeSolicitud();
    }

    @Override
    public String numeroSolicitudAbierta() {
        return numeroSolicitudAbierta;
    }

    @Override
    public boolean cierreConfirmadoAlApartar() {
        return cierreConfirmadoAlApartar;
    }

    /** Del texto que escribe la persona solo importa el numero ("1-01-45-202610-4336"), aunque pegue la linea entera. */
    static String numeroDeSolicitud(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(?:\\s*-\\s*\\d+){2,}").matcher(texto);
        return m.find() ? m.group().replaceAll("\\s+", "") : texto.strip();
    }

    /** La tarjeta seleccionada se pinta de morado (rgb 95, 64, 127); las demas son blancas (visto en el volcado). */
    private static boolean tarjetaSeleccionada(Locator tarjeta) {
        try {
            return "rgb(95, 64, 127)".equals(String.valueOf(
                    tarjeta.evaluate("el => getComputedStyle(el).backgroundColor")));
        } catch (RuntimeException e) {
            return false;   // ante la duda se vuelve a pulsar la tarjeta, que es lo seguro
        }
    }

    /** Si quedo un cuadro de una vuelta anterior abierto, se cierra (sin FINALIZAR) antes de seguir. */
    private void cerrarCuadroAbierto() {
        if (estaVisible(pagina.locator(SelectoresSiae.PAPEL_MODAL).last())) {
            cerrarCuadroSinFinalizar();
        }
    }

    /** Espera a que la pagina cargue: las pestanas CIUDADANO/EMPRESA e HISTORIAL son lo primero que sale. */
    private void esperarEstadoTramite() {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (pagina.locator(SelectoresSiae.PESTANAS).count() > 0) {
                return;
            }
            pagina.waitForTimeout(250);
        }
        throw inesperado("No se abrió el módulo Estado de trámite.", null);
    }

    /**
     * La lista de solicitudes llega del servidor varios segundos despues de que carga la pagina (unos
     * 6 s en el volcado): concluir "no hay solicitud" demasiado pronto detendria el monitor sin
     * motivo. Se espera a la tarjeta que coincide, no a "alguna" tarjeta, porque puede haber otras.
     */
    private Locator esperarTarjetaDeSolicitud(ParametrosTramite tramite, String numero) {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        do {
            detectarSesionVencida();
            Locator tarjeta = buscarTarjetaDeSolicitud(tramite, numero);
            if (tarjeta != null) {
                return tarjeta;
            }
            pagina.waitForTimeout(300);
        } while (System.nanoTime() < limite);
        LOG.info("Estado de trámite: {} solicitudes visibles y ninguna coincide ({}).",
                pagina.locator(SelectoresSiae.TARJETAS_TRAMITE).count(),
                numero.isBlank() ? "EN PROGRESO con permiso \"" + tramite.obtenerPermiso() + "\" y tipo \""
                        + tramite.tipoTramite() + "\"" : "número " + numero + " EN PROGRESO");
        return null;
    }

    /**
     * La solicitud en progreso que busca el cliente: la del numero indicado o, sin numero, la primera
     * cuyo permiso y tipo de tramite coinciden. Nunca se toca nada fuera de las tarjetas: el boton
     * "Cancelar tramite" queda intacto.
     */
    private Locator buscarTarjetaDeSolicitud(ParametrosTramite tramite, String numero) {
        for (Locator tarjeta : pagina.locator(SelectoresSiae.TARJETAS_TRAMITE).all()) {
            String texto = textoDe(tarjeta);
            if (!Texto.contieneTermino(texto, SelectoresSiae.ESTADO_EN_PROGRESO)) {
                continue;
            }
            boolean coincide = numero.isBlank()
                    ? Texto.contieneTermino(texto, tramite.obtenerPermiso())
                            && Texto.contieneTermino(texto, tramite.tipoTramite())
                    : Texto.contieneTermino(texto, numero);
            if (coincide) {
                return tarjeta;
            }
        }
        return null;
    }

    /**
     * Al abrir la linea FG-1 el listado aparece primero vacio ("Sin filas") mientras carga; solo hay
     * inventario cuando llegan filas y solo hay "sin inventario" cuando sale el aviso del cuadro.
     */
    private ResultadoSolicitud esperarResultadoInventarioDeSolicitud() {
        long limite = System.nanoTime() + ESPERA_LARGA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (pagina.locator(SelectoresSiae.FILAS_INVENTARIO).count() > 0) {
                desdeEstadoTramite = true;
                return ResultadoSolicitud.CON_INVENTARIO;
            }
            if (pagina.locator(SelectoresSiae.AVISO_SIN_INVENTARIO_EN_MODAL).count() > 0) {
                cerrarCuadroSinFinalizar();
                return ResultadoSolicitud.SIN_INVENTARIO;
            }
            pagina.waitForTimeout(250);
        }
        throw inesperado("La solicitud no mostró inventario ni el aviso de que no hay.", null);
    }

    /**
     * Cierra el cuadro de la solicitud con un clic fuera de el. NUNCA se pulsa FINALIZAR: con la
     * solicitud sin inventario ese boton CANCELA la solicitud. Si el clic no basta se prueba con
     * Escape; si tampoco, se falla antes que arriesgar la solicitud.
     */
    private void cerrarCuadroSinFinalizar() {
        for (int intento = 0; intento < 2; intento++) {
            Locator papel = pagina.locator(SelectoresSiae.PAPEL_MODAL).last();
            if (!estaVisible(papel)) {
                return;
            }
            if (intento == 0) {
                clicFueraDelCuadro(papel);
            } else {
                pagina.keyboard().press("Escape");
            }
            long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            while (System.nanoTime() < limite) {
                if (!estaVisible(papel)) {
                    return;
                }
                pagina.waitForTimeout(100);
            }
        }
        throw inesperado("No se pudo cerrar el cuadro de la solicitud sin pulsar FINALIZAR.", null);
    }

    /** Clic en la esquina superior izquierda del fondo del cuadro, siempre fuera del recuadro blanco. */
    private void clicFueraDelCuadro(Locator papel) {
        BoundingBox fondo = pagina.locator(SelectoresSiae.CONTENEDOR_MODAL).last().boundingBox();
        BoundingBox recuadro = papel.boundingBox();
        if (fondo == null || recuadro == null) {
            return;
        }
        double x = fondo.x + 10;
        double y = fondo.y + 10;
        boolean dentro = x >= recuadro.x && x <= recuadro.x + recuadro.width
                && y >= recuadro.y && y <= recuadro.y + recuadro.height;
        if (dentro || sobreBotonPeligroso(x, y)) {
            return;   // queda Escape, que no toca ningún botón
        }
        pagina.mouse().click(x, y);
    }

    /** Verdadero si el punto cae sobre "Cancelar trámite" (la X roja), que jamás se pulsa. */
    private boolean sobreBotonPeligroso(double x, double y) {
        for (Locator boton : pagina.locator(SelectoresSiae.BOTON_CANCELAR_TRAMITE).all()) {
            BoundingBox caja = boton.boundingBox();
            if (caja != null && x >= caja.x - 8 && x <= caja.x + caja.width + 8
                    && y >= caja.y - 8 && y <= caja.y + caja.height + 8) {
                LOG.warn("El clic para cerrar el cuadro caería sobre «Cancelar trámite»; se usa Escape.");
                return true;
            }
        }
        return false;
    }

    // --- Comunes -----------------------------------------------------------------

    private boolean hayMensajeSinInventario() {
        return pagina.locator(SelectoresSiae.SIN_INVENTARIO).count() > 0;
    }

    private boolean hayEnlaceInventario() {
        return pagina.locator(SelectoresSiae.ENLACE_INVENTARIO).count() > 0;
    }

    private boolean fg1Abierto() {
        return pagina.locator(SelectoresSiae.PASO_2).count() > 0;
    }

    private boolean hayPaso3() {
        return pagina.locator(SelectoresSiae.PASO_3).count() > 0;
    }

    private void detectarSesionVencida() {
        Locator contrasena = pagina.locator(SelectoresSiae.LOGIN_PASSWORD).first();
        Locator ingresar = pagina.locator(SelectoresSiae.INGRESAR_PORTAL).first();
        if ((contrasena.count() > 0 && estaVisible(contrasena))
                || (ingresar.count() > 0 && estaVisible(ingresar))) {
            throw new SesionVencidaException(
                    "La plataforma pidió iniciar sesión de nuevo en la ventana del cliente.");
        }
    }

    private static boolean estaVisible(Locator elemento) {
        try {
            return elemento.isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String textoDe(Locator elemento) {
        try {
            String texto = elemento.innerText();
            return texto == null ? "" : texto.replaceAll("\\s+", " ").strip();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static boolean esRuido(String texto) {
        String plano = Texto.normalizar(texto);
        return plano.isEmpty() || plano.startsWith("cargando") || plano.startsWith("loading")
                || plano.startsWith("no option") || plano.startsWith("sin opcion")
                || plano.startsWith("no hay opcion");
    }

    private static int indiceQueCoincide(List<String> opciones, String valor) {
        for (int i = 0; i < opciones.size(); i++) {
            if (Texto.equivalentes(opciones.get(i), valor)) {
                return i;
            }
        }
        for (int i = 0; i < opciones.size(); i++) {
            if (Texto.contieneTermino(opciones.get(i), valor)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean estaAbierto() {
        if (cerrado) {
            return false;
        }
        if (pagina.isClosed()) {
            registrarCierre();
            return false;
        }
        try {
            pagina.waitForTimeout(1);
            return !cerrado && !pagina.isClosed();
        } catch (PlaywrightException e) {
            if (cerrado || pagina.isClosed()
                    || (e.getMessage() != null && e.getMessage().toLowerCase(java.util.Locale.ROOT)
                            .contains("has been closed"))) {
                registrarCierre();
                return false;
            }
            throw e;
        }
    }

    @Override
    public void alCerrar(Runnable accion) {
        accionAlCerrar = Objects.requireNonNull(accion);
        if (cerrado && !cerrando) {
            accionAlCerrar.run();
        }
    }

    private void registrarCierre() {
        if (cerrando || cerrado) {
            return;
        }
        if (!pagina.isClosed()) {
            return;
        }
        cerrado = true;
        accionAlCerrar.run();
    }

    private void verificarDisponible() {
        if (!estaAbierto()) {
            throw new SesionPortalCerradaException(
                    "El navegador del cliente se cerró. Abre una nueva sesión para continuar.");
        }
    }

    @Override
    public void enfocarVentana() {
        verificarDisponible();
        pagina.bringToFront();
    }

    private PortalInesperadoException inesperado(String mensaje, Throwable causa) {
        Path captura = capturar(mensaje);
        String sufijo = captura == null ? "" : " (captura: " + captura.getFileName() + ")";
        return new PortalInesperadoException(mensaje + sufijo, captura, causa);
    }

    private Path capturar(String motivo) {
        try {
            Files.createDirectories(capturas);
            String marca = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            Path destino = capturas.resolve("fallo-" + marca + ".png");
            pagina.screenshot(new Page.ScreenshotOptions().setPath(destino));
            return destino;
        } catch (RuntimeException | IOException e) {
            LOG.warn("No se pudo guardar una captura del portal en {}", capturas, e);
            return null;
        }
    }

    @Override
    public void close() {
        cerrando = true;
        cerrado = true;
        try {
            if (!pagina.isClosed()) {
                contexto.close();
            }
        } finally {
            playwright.close();
        }
    }
}


