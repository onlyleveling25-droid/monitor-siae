package co.monitorsiae.portal.real;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.Texto;
import co.monitorsiae.portal.OpcionNoDisponibleException;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalException;
import co.monitorsiae.portal.PortalInesperadoException;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.SesionPortalCerradaException;
import co.monitorsiae.portal.SesionVencidaException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
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
import java.util.List;
import java.util.Objects;

public final class PlaywrightPortalDriver implements PortalDriver {

    public static final String URL_TRAMITE = "https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite";

    private static final Duration ESPERA_CORTA = Duration.ofSeconds(6);
    private static final Duration ESPERA_MEDIA = Duration.ofSeconds(20);
    private static final Duration ESPERA_LARGA = Duration.ofSeconds(30);
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(PlaywrightPortalDriver.class);

    private final Playwright playwright;
    private final BrowserContext contexto;
    private final Page pagina;
    private final Path capturas;
    private volatile boolean cerrado;
    private volatile boolean cerrando;
    private volatile Runnable accionAlCerrar = () -> { };

    public PlaywrightPortalDriver(Path perfil, NavegadorPreferido navegador, Path capturas) {
        this.capturas = capturas;
        Playwright runtime = Playwright.create();
        BrowserContext contextoCreado;
        Page paginaCreada;
        try {
            BrowserType.LaunchPersistentContextOptions opciones
                    = new BrowserType.LaunchPersistentContextOptions()
                            .setChannel(canal(navegador))
                            .setHeadless(false)
                            .setViewportSize((ViewportSize) null)
                            .setArgs(List.of("--start-maximized", "--no-first-run",
                                    "--no-default-browser-check", "--disable-background-networking"));
            contextoCreado = runtime.chromium().launchPersistentContext(perfil, opciones);
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
            throw new PortalException("No se pudo abrir el navegador o el portal SIAE. "
                    + "Verifica que el navegador seleccionado esté instalado.", e);
        }
        playwright = runtime;
        contexto = contextoCreado;
        pagina = paginaCreada;
        contexto.onClose(ignorado -> registrarCierre());
        pagina.onClose(ignorado -> registrarCierre());
    }

    private static String canal(NavegadorPreferido navegador) {
        return switch (navegador) {
            case CHROME ->
                "chrome";
            case EDGE ->
                "msedge";
        };
    }

    // --- Paso 1 ---------------------------------------------------------------
    @Override
    public List<String> abrirFormulario(ParametrosTramite tramite) {
        verificarDisponible();
        asegurarPaso1();
        seleccionarSiHaceFalta(SelectoresSiae.PLACEHOLDER_PERMISO, tramite.obtenerPermiso(),
                "como desea obtener el permiso");
        seleccionarSiHaceFalta(SelectoresSiae.PLACEHOLDER_TIPO_TRAMITE, tramite.tipoTramite(),
                "tipo de trámite");
        return leerOpciones(SelectoresSiae.PLACEHOLDER_SECCIONAL);
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

    private void seleccionarSiHaceFalta(String placeholder, String valor, String nombre) {
        Locator combo = combo(placeholder);
        combo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        if (Texto.equivalentes(combo.inputValue(), valor)) {
            return;
        }
        elegirCombo(placeholder, valor, nombre);
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
        guardar.click();
        return esperarResultadoGuardar();
    }

    private void recargarPaso1() {
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

    private void elegirCombo(String placeholder, String valor, String nombre) {
        int indice = -1;
        List<Locator> utiles = new ArrayList<>();
        List<String> textos = new ArrayList<>();

        Locator combo = combo(placeholder);
        combo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        combo.click();

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

        indice = indiceQueCoincide(textos, valor);
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

    private static String textoDe(Locator elemento) {
        String texto = elemento.innerText();
        return texto == null ? "" : texto.replaceAll("\\s+", " ").strip();
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

    private ResultadoGuardar esperarResultadoGuardar() {
        long limite = System.nanoTime() + ESPERA_LARGA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (pagina.locator(SelectoresSiae.PASO_2).count() > 0) {
                return ResultadoGuardar.AVANZO_PASO_2;
            }
            if (pagina.locator(SelectoresSiae.SIN_INVENTARIO).count() > 0) {
                return ResultadoGuardar.SIN_INVENTARIO;
            }
            pagina.waitForTimeout(300);
        }
        throw inesperado("No se reconoció la respuesta del portal después de GUARDAR.", null);
    }

    // --- Paso 2 ---------------------------------------------------------------
    @Override
    public List<ArmaInventario> leerInventario() {
        verificarDisponible();
        detectarSesionVencida();
        esperarFilasInventario();

        List<ArmaInventario> armas = new ArrayList<>();
        List<Locator> filas = pagina.locator(SelectoresSiae.FILAS_INVENTARIO).all();
        for (int i = 0; i < filas.size(); i++) {
            Locator fila = filas.get(i);
            String descripcion = textoDe(fila);
            if (descripcion.isEmpty()) {
                continue;
            }
            String tipo = "";
            List<Locator> celdas = fila.locator("td").all();
            if (!celdas.isEmpty()) {
                tipo = textoDe(celdas.getFirst());
            }
            armas.add(new ArmaInventario(i, descripcion, tipo));
        }
        return armas;
    }

    @Override
    public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
        verificarDisponible();
        detectarSesionVencida();

        Locator fila = buscarFila(arma);
        if (fila == null) {
            throw inesperado("El arma ya no está en el listado: " + arma.descripcion(), null);
        }
        fila.click();

        Locator botonModalidad = pagina.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(etiqueta(modalidad)).setExact(true)).first();
        botonModalidad.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        botonModalidad.click();

        Locator confirmar = pagina.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Confirmar").setExact(true)).first();
        confirmar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                .setTimeout(ESPERA_CORTA.toMillis()));
        confirmar.click();

        return esperarAsignacionExitosa();
    }

    private static String etiqueta(Modalidad modalidad) {
        return switch (modalidad) {
            case PORTE ->
                "Porte";
            case TENENCIA ->
                "Tenencia";
        };
    }

    private Locator buscarFila(ArmaInventario arma) {
        List<Locator> filas = pagina.locator(SelectoresSiae.FILAS_INVENTARIO).all();
        for (Locator fila : filas) {
            String texto = textoDe(fila);
            if (Texto.equivalentes(texto, arma.descripcion())
                    || Texto.contieneTermino(texto, arma.descripcion())) {
                return fila;
            }
        }
        return null;
    }

    private void esperarFilasInventario() {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            if (pagina.locator(SelectoresSiae.FILAS_INVENTARIO).count() > 0) {
                return;
            }
            pagina.waitForTimeout(300);
        }
        throw inesperado("El paso 2 no mostró filas de inventario.", null);
    }

    private boolean esperarAsignacionExitosa() {
        long limite = System.nanoTime() + ESPERA_MEDIA.toNanos();
        while (System.nanoTime() < limite) {
            detectarSesionVencida();
            if (pagina.locator(SelectoresSiae.ASIGNACION_EXITOSA).count() > 0) {
                return true;
            }
            pagina.waitForTimeout(300);
        }
        return false;
    }

    // --- Comunes --------------------------------------------------------------
    private void detectarSesionVencida() {
        Locator contrasena = pagina.locator(SelectoresSiae.LOGIN_PASSWORD).first();
        Locator ingresarPortal = pagina.locator(SelectoresSiae.INGRESAR_PORTAL).first();
        if ((contrasena.count() > 0 && contrasena.isVisible())
                || (ingresarPortal.count() > 0 && ingresarPortal.isVisible())) {
            throw new SesionVencidaException(
                    "La plataforma pidió iniciar sesión de nuevo en la ventana del cliente.");
        }
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
                    || (e.getMessage() != null
                    && e.getMessage().toLowerCase(java.util.Locale.ROOT).contains("has been closed"))) {
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
