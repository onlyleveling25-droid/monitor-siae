package co.monitorsiae.portal.real;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Herramienta de reconocimiento del modulo "Estado de tramite" del portal. No forma parte de la
 * aplicacion: se ejecuta a mano para volcar el DOM de ese modulo y poder escribir sus selectores.
 *
 * <p><b>Es de solo lectura.</b> No hace clic, no escribe en ningun campo y no navega (salvo la
 * apertura inicial del modulo). Quien navega es la persona, en la ventana del navegador, con un
 * usuario que ya tiene un tramite guardado: abre la solicitud, el enlace "FG-1...", la tabla de
 * inventario, etc. El script solo observa y guarda lo que va apareciendo.
 *
 * <p>Como captura:
 * <ul>
 *   <li><b>Automatica:</b> cada ~0,7 s mira cada pestana y, cuando su contenido cambia y se
 *       mantiene estable, guarda una captura numerada (HTML, texto, elementos con sus selectores,
 *       modales, resumen y imagen). Tambien registra los avisos efimeros (snackbars) en
 *       {@code mensajes.txt}.</li>
 *   <li><b>Manual:</b> ENTER guarda una captura al instante; escribir un texto y ENTER la guarda con
 *       esa etiqueta (por ejemplo {@code sin-inventario}); {@code salir} termina.</li>
 * </ul>
 *
 * <p>Salida en {@code reconocimiento/estado-tramite/}. Incluye <b>datos personales</b> del usuario
 * (nombre, identificacion, numero de tramite): revisa los archivos antes de compartirlos o
 * subirlos al repositorio. La red se guarda solo como metodo, estado y direccion sin parametros;
 * nunca los cuerpos de las peticiones ni de las respuestas.
 *
 * <p>Uso: {@code mvn -o compile exec:java -Dexec.mainClass=co.monitorsiae.portal.real.ReconocimientoEstadoTramite
 * -Dexec.args="chrome"}
 */
public final class ReconocimientoEstadoTramite {

    private static final String URL_MODULO = "https://dccae.cgfm.mil.co/dccae/main/EstadoTramite";
    private static final Path SALIDA = Path.of("reconocimiento", "estado-tramite");

    private static final long SONDEO_MS = 700;
    private static final long MINIMO_ENTRE_AUTOMATICAS_MS = 4_000;
    private static final int MAXIMO_AUTOMATICAS_POR_MINUTO = 8;
    private static final int MAXIMO_CAPTURAS = 120;
    private static final String SEP = "@@@";

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final Queue<String> ORDENES = new ConcurrentLinkedQueue<>();
    private static final List<String> RED = new ArrayList<>();
    private static final List<String> MENSAJES = new ArrayList<>();
    private static final List<String> INDICE = new ArrayList<>();
    private static final List<Long> MARCAS_AUTOMATICAS = new ArrayList<>();

    private static volatile boolean cerrado;
    private static BrowserContext contexto;
    private static int contador;

    private ReconocimientoEstadoTramite() {
    }

    public static void main(String[] args) throws Exception {
        String canal = args.length > 0 && !args[0].isBlank() ? args[0] : "chrome";
        String url = args.length > 1 && !args[1].isBlank() ? args[1] : URL_MODULO;
        boolean sinVentana = Boolean.getBoolean("reconocimiento.headless");
        Files.createDirectories(SALIDA);

        Path perfil = perfil();
        System.out.println("Navegador: " + canal);
        System.out.println("Perfil:    " + perfil);
        System.out.println("Salida:    " + SALIDA.toAbsolutePath());
        System.out.println("Direccion: " + url);

        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {
            contexto = playwright.chromium().launchPersistentContext(perfil,
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel(canal)
                            .setHeadless(sinVentana)
                            .setViewportSize(sinVentana ? new ViewportSize(1440, 900) : null)
                            .setArgs(List.of("--start-maximized", "--no-first-run",
                                    "--no-default-browser-check")));
            contexto.onClose(ignorado -> cerrado = true);
            contexto.onResponse(respuesta -> registrarRed(respuesta.request().method(),
                    String.valueOf(respuesta.status()), respuesta.url()));
            contexto.onRequestFailed(peticion -> registrarRed(peticion.method(), "FALLO", peticion.url()));

            Page inicial = contexto.pages().isEmpty() ? contexto.newPage() : contexto.pages().getFirst();
            inicial.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            System.out.println();
            System.out.println("1) En la ventana del navegador, inicia sesion y resuelve el captcha.");
            System.out.println("2) Cuando veas el modulo 'Estado de tramite', pulsa ENTER aqui para EMPEZAR a capturar.");
            System.out.println("3) Navega tu: abre la solicitud, el enlace FG-1, la tabla, los modales... El script no hace clic.");
            System.out.println("   ENTER = captura ahora | texto + ENTER = captura con esa etiqueta | salir = terminar.");
            leerOrdenesEnSegundoPlano();
            // Si se corta con Ctrl+C, los informes de red, avisos e indice igual quedan guardados.
            Runtime.getRuntime().addShutdownHook(new Thread(ReconocimientoEstadoTramite::guardarInformes));

            try {
                observar();
            } finally {
                guardarInformes();
            }
        }
        System.out.println("Listo. Revisa " + SALIDA.toAbsolutePath());
    }

    // ==================== BUCLE ====================

    private static void observar() {
        boolean activo = false;
        Map<Page, String> previa = new HashMap<>();
        Map<Page, String> capturada = new HashMap<>();

        while (!cerrado) {
            List<Page> paginas = paginasAbiertas();
            if (paginas.isEmpty()) {
                return;
            }

            String orden;
            while ((orden = ORDENES.poll()) != null) {
                String texto = orden.strip();
                if (texto.equalsIgnoreCase("salir")) {
                    return;
                }
                if (!activo) {
                    activo = true;
                    System.out.println(">>> Capturando. Navega con calma; cada cambio estable se guarda solo.");
                    continue;
                }
                Page destino = paginas.getLast();
                capturar(destino, "manual", texto, leerFirma(destino));
            }

            for (Page pagina : paginas) {
                String firma = leerFirma(pagina);
                if (firma == null) {
                    continue;
                }
                registrarMensajes(pagina, firma);
                if (activo && !firma.equals(capturada.get(pagina)) && firma.equals(previa.get(pagina))
                        && puedeCapturarAutomatica()) {
                    capturar(pagina, "auto", "", firma);
                    capturada.put(pagina, firma);
                }
                previa.put(pagina, firma);
            }
            previa.keySet().removeIf(Page::isClosed);
            capturada.keySet().removeIf(Page::isClosed);

            try {
                paginas.getFirst().waitForTimeout(SONDEO_MS);
            } catch (PlaywrightException e) {
                // la pagina se cerro mientras esperaba: el bucle comprueba el estado al volver
            }
        }
    }

    private static List<Page> paginasAbiertas() {
        try {
            List<Page> abiertas = new ArrayList<>();
            for (Page pagina : contexto.pages()) {
                if (!pagina.isClosed()) {
                    abiertas.add(pagina);
                }
            }
            return abiertas;
        } catch (PlaywrightException e) {
            return List.of();
        }
    }

    private static boolean puedeCapturarAutomatica() {
        if (contador >= MAXIMO_CAPTURAS) {
            return false;
        }
        long ahora = System.currentTimeMillis();
        MARCAS_AUTOMATICAS.removeIf(marca -> ahora - marca > 60_000);
        if (!MARCAS_AUTOMATICAS.isEmpty()
                && ahora - MARCAS_AUTOMATICAS.getLast() < MINIMO_ENTRE_AUTOMATICAS_MS) {
            return false;
        }
        if (MARCAS_AUTOMATICAS.size() >= MAXIMO_AUTOMATICAS_POR_MINUTO) {
            return false;
        }
        MARCAS_AUTOMATICAS.add(ahora);
        return true;
    }

    private static void leerOrdenesEnSegundoPlano() {
        Thread lector = new Thread(() -> {
            try (var entrada = new java.util.Scanner(System.in, StandardCharsets.UTF_8)) {
                while (entrada.hasNextLine()) {
                    ORDENES.add(entrada.nextLine());
                }
            }
        }, "lector-ordenes");
        lector.setDaemon(true);
        lector.start();
    }

    // ==================== CAPTURA (solo lectura) ====================

    private static String leerFirma(Page pagina) {
        try {
            return String.valueOf(pagina.evaluate(JS_FIRMA));
        } catch (PlaywrightException e) {
            return null;   // navegando o cerrandose
        }
    }

    private static void registrarMensajes(Page pagina, String firma) {
        String[] partes = firma.split(SEP, -1);
        if (partes.length < 3 || partes[2].isBlank()) {
            return;
        }
        String linea = HORA.format(LocalTime.now()) + "  " + partes[2];
        if (MENSAJES.isEmpty() || !MENSAJES.getLast().endsWith(partes[2])) {
            MENSAJES.add(linea);
            System.out.println(">>> Aviso en pantalla: " + partes[2]);
            guardarInformes();
        }
    }

    private static void capturar(Page pagina, String modo, String etiqueta, String firma) {
        if (contador >= MAXIMO_CAPTURAS) {
            System.out.println(">>> Se alcanzo el maximo de " + MAXIMO_CAPTURAS + " capturas.");
            return;
        }
        contador++;
        String[] partes = firma == null ? new String[] {"", "0", "", "", ""} : firma.split(SEP, -1);
        String url = partes[0];
        int dialogos = partes.length > 1 && partes[1].matches("\\d+") ? Integer.parseInt(partes[1]) : 0;
        String nombre = String.format(Locale.ROOT, "%02d-%s-%s%s%s", contador, modo, segmento(url),
                dialogos > 0 ? "-modal" : "", etiqueta.isBlank() ? "" : "-" + limpiarNombre(etiqueta));
        try {
            escribir(nombre + ".html", pagina.content());
            escribir(nombre + "-texto.txt", "URL: " + pagina.url() + "\n\n" + pagina.locator("body").innerText());
            escribir(nombre + "-elementos.txt", "# " + nombre + "\nURL: " + pagina.url() + "\n\n"
                    + pagina.evaluate(JS_ELEMENTOS));
            escribir(nombre + "-resumen.txt", "# " + nombre + "\nURL: " + pagina.url() + "\n\n"
                    + pagina.evaluate(JS_RESUMEN));
            Object modales = pagina.evaluate(JS_MODALES);
            if (modales instanceof List<?> lista) {
                for (int i = 0; i < lista.size(); i++) {
                    escribir(nombre + "-modal-" + (i + 1) + ".html", String.valueOf(lista.get(i)));
                }
            }
            pagina.screenshot(new Page.ScreenshotOptions().setPath(SALIDA.resolve(nombre + ".png")));
            INDICE.add(HORA.format(LocalTime.now()) + "  " + nombre + "  modales=" + dialogos + "  " + url);
            RED.add("--- captura " + nombre + " (" + url + ")");
            System.out.println(">>> Guardado " + nombre);
            guardarInformes();
        } catch (IOException | PlaywrightException e) {
            System.out.println(">>> No se pudo guardar " + nombre + ": " + e.getMessage());
        }
    }

    private static void registrarRed(String metodo, String estado, String direccion) {
        if (!direccion.contains("cgfm.mil.co") || esEstatico(direccion)) {
            return;
        }
        int consulta = direccion.indexOf('?');
        String limpia = consulta >= 0 ? direccion.substring(0, consulta) : direccion;
        RED.add(HORA.format(LocalTime.now()) + "  " + metodo + "  " + estado + "  " + limpia);
    }

    private static boolean esEstatico(String direccion) {
        String minuscula = direccion.toLowerCase(Locale.ROOT);
        int consulta = minuscula.indexOf('?');
        String ruta = consulta >= 0 ? minuscula.substring(0, consulta) : minuscula;
        for (String extension : List.of(".js", ".css", ".png", ".jpg", ".jpeg", ".svg", ".gif", ".ico",
                ".woff", ".woff2", ".ttf", ".map", ".webp", ".json")) {
            if (ruta.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static synchronized void guardarInformes() {
        try {
            escribir("indice.txt", String.join(System.lineSeparator(), INDICE));
            escribir("red.txt", String.join(System.lineSeparator(), RED));
            escribir("mensajes.txt", String.join(System.lineSeparator(), MENSAJES));
        } catch (IOException e) {
            System.out.println("No se pudieron guardar los informes: " + e.getMessage());
        }
    }

    // ==================== UTILIDADES ====================

    private static Path perfil() {
        String propio = System.getProperty("reconocimiento.perfil");
        if (propio != null && !propio.isBlank()) {
            return Path.of(propio);
        }
        String appdata = System.getenv("APPDATA");
        Path base = appdata != null && !appdata.isBlank()
                ? Path.of(appdata)
                : Path.of(System.getProperty("user.home"), "Library", "Application Support");
        return base.resolve("MonitorSIAE").resolve("perfiles").resolve("_reconocimiento");
    }

    private static String segmento(String url) {
        String sinConsulta = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        int barra = sinConsulta.lastIndexOf('/');
        String ultimo = barra >= 0 ? sinConsulta.substring(barra + 1) : sinConsulta;
        return ultimo.isBlank() ? "pagina" : limpiarNombre(ultimo);
    }

    private static String limpiarNombre(String texto) {
        String limpio = texto.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return limpio.isEmpty() ? "x" : limpio.substring(0, Math.min(limpio.length(), 40));
    }

    private static void escribir(String nombre, String contenido) throws IOException {
        Files.writeString(SALIDA.resolve(nombre), contenido, StandardCharsets.UTF_8);
    }

    // ==================== JAVASCRIPT (todo de lectura) ====================

    private static final String JS_VISIBLE = """
            const visible = el => {
              const r = el.getBoundingClientRect();
              const s = getComputedStyle(el);
              return r.width > 0 && r.height > 0 && s.visibility !== 'hidden' && s.display !== 'none';
            };
            const limpiar = t => (t || '').replace(/\\s+/g, ' ').trim();
            const MODALES = "[role='dialog'], mat-dialog-container, .cdk-overlay-pane, .MuiDialog-container";
            const AVISOS = "[role='alert'], simple-snack-bar, snack-bar-container, .mat-mdc-snack-bar-container,"
              + " .MuiSnackbar-root, .MuiAlert-root";
            """;

    /** url, cantidad de modales, avisos visibles y un resumen del texto para detectar cambios. */
    private static final String JS_FIRMA = "() => {" + JS_VISIBLE + """
              const modales = Array.from(document.querySelectorAll(MODALES)).filter(visible);
              const avisos = Array.from(document.querySelectorAll(AVISOS)).filter(visible)
                .map(e => limpiar(e.innerText)).filter(Boolean);
              const texto = document.body ? (document.body.innerText || '') : '';
              let h = 0;
              for (let i = 0; i < texto.length; i++) { h = (h * 31 + texto.charCodeAt(i)) | 0; }
              return [location.href, modales.length, avisos.join(' || '), h, texto.length].join('@@@');
            }""";

    private static final String JS_MODALES = "() => {" + JS_VISIBLE + """
              return Array.from(document.querySelectorAll(MODALES)).filter(visible).map(e => e.outerHTML);
            }""";

    private static final String JS_RESUMEN = "() => {" + JS_VISIBLE + """
              const NL = String.fromCharCode(10);
              const salida = [];
              salida.push('Titulo: ' + document.title);
              const modales = Array.from(document.querySelectorAll(MODALES)).filter(visible);
              salida.push('Modales visibles: ' + modales.length);
              modales.forEach((m, i) => salida.push('  [' + (i + 1) + '] ' + limpiar(m.innerText).slice(0, 300)));
              const avisos = Array.from(document.querySelectorAll(AVISOS)).filter(visible)
                .map(e => limpiar(e.innerText)).filter(Boolean);
              salida.push('Avisos: ' + (avisos.length ? avisos.join(' || ') : '(ninguno)'));
              const botones = Array.from(document.querySelectorAll("button, [role='button'], a, [role='tab']"))
                .filter(visible).map(e => limpiar(e.innerText)).filter(Boolean);
              salida.push('Botones y enlaces visibles: ' + botones.join(' | '));
              const tablas = Array.from(document.querySelectorAll("table, [role='grid'], mat-table")).filter(visible);
              salida.push('Tablas visibles: ' + tablas.length);
              tablas.forEach((t, i) => {
                const cab = Array.from(t.querySelectorAll("th, [role='columnheader']")).map(e => limpiar(e.innerText));
                const filas = t.querySelectorAll("tbody tr, [role='row'], mat-row");
                salida.push('  tabla ' + (i + 1) + ': ' + filas.length + ' filas; columnas: ' + cab.join(' | '));
              });
              const seleccion = limpiar(document.body ? document.body.innerText : '').match(/\\d+\\s*fila[s]?\\s+seleccionada[s]?/i);
              salida.push('Seleccion: ' + (seleccion ? seleccion[0] : '(no hay texto de seleccion)'));
              return salida.join(NL);
            }""";

    private static final String JS_ELEMENTOS = "() => {" + JS_VISIBLE + """
              const NL = String.fromCharCode(10);
              const selector = el => {
                if (el.id) return '#' + el.id;
                for (const a of ['formcontrolname', 'name', 'aria-label', 'data-testid', 'placeholder', 'role', 'type']) {
                  const v = el.getAttribute && el.getAttribute(a);
                  if (v) return el.tagName.toLowerCase() + '[' + a + '="' + v + '"]';
                }
                const partes = [];
                let n = el;
                while (n && n.nodeType === 1 && partes.length < 5) {
                  let s = n.tagName.toLowerCase();
                  if (n.parentElement) {
                    const hermanos = Array.from(n.parentElement.children).filter(c => c.tagName === n.tagName);
                    if (hermanos.length > 1) s += ':nth-of-type(' + (hermanos.indexOf(n) + 1) + ')';
                  }
                  partes.unshift(s);
                  n = n.parentElement;
                }
                return partes.join(' > ');
              };
              const elementos = Array.from(document.querySelectorAll(
                "input, textarea, select, button, a, [role], [formcontrolname], mat-select, mat-option, "
                + "table, th, td, tr, mat-row, mat-cell, li, h1, h2, h3, h4, label"))
                .filter(visible).slice(0, 1500);
              const lineas = ['Elementos visibles: ' + elementos.length, ''];
              elementos.forEach((el, i) => {
                const r = el.getBoundingClientRect();
                const attrs = {};
                el.getAttributeNames().forEach(n => { attrs[n] = el.getAttribute(n); });
                lineas.push(i + '. ' + el.tagName.toLowerCase() + '  caja='
                  + [Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height)].join(','));
                lineas.push('   selector: ' + selector(el));
                lineas.push('   attrs:    ' + JSON.stringify(attrs));
                lineas.push('   texto:    ' + limpiar(el.innerText).slice(0, 200));
                lineas.push('');
              });
              return lineas.join(NL);
            }""";
}
