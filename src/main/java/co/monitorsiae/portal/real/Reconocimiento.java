package co.monitorsiae.portal.real;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Herramienta de reconocimiento. No forma parte de la aplicacion: se ejecuta a mano para volcar
 * el DOM del portal y poder escribir los selectores de PlaywrightPortalDriver.
 *
 * <p>Inicia sesion a mano, llena el paso 1, cicla GUARDAR con jitter hasta que aparezca inventario
 * y entonces EXPLORA EL PASO 2 paso a paso: pantalla intermedia, enlace, ventana FG-1, tabla,
 * fila, modal de PORTE, modal de TENENCIA, CONFIRMAR y FINALIZAR. Volca HTML, elementos, modales
 * y capturas de cada sub-paso.
 *
 * <p>OJO: la exploracion del paso 2 deja el arma pre-asignada y puede avanzar al paso 3. Para
 * repetirla hace falta un tramite nuevo.
 */
public final class Reconocimiento {
    public void main() throws Exception {
        main(new String[0]);
    }

    // ==================== PARAMETROS DEL PASO 1 (edita aqui) ====================
    private static final String OBTENER_PERMISO = "Defensa Personal";
    private static final String TIPO_TRAMITE = "Armas personas naturales: Adquisición de arma";
    private static final String DESCRIPCION =
            "Solicito la adquisición de un arma para defensa personal, conforme a los requisitos y "
                    + "procedimientos establecidos por la autoridad competente. Me comprometo a "
                    + "suministrar información veraz y a cumplir las obligaciones aplicables.";

    /**
     * Seccionales a monitorear, en orden de prioridad. Basta una parte reconocible (no importan
     * tildes ni mayusculas): "Bogota" encuentra "16 - Usaquén, Bogotá D.C.".
     * Lista vacia = todas. Tambien por consola: {@code -Dexec.args="chrome Bogota,Medellin"}.
     * Si ninguna coincide, se usan todas las disponibles en vez de abortar.
     */
    private static String[] seccionalesAMonitorear = {"Medellin", "Bogota"};

    // Pistas para localizar cada combo (por placeholder, texto o etiqueta). Se prueban en orden.
    private static final String[] PISTA_PERMISO = {"obtener el permiso", "como desea obtener", "permiso"};
    private static final String[] PISTA_TIPO = {"tipo de tramite", "clase de tramite", "tipo tramite"};
    private static final String[] PISTA_SECCIONAL = {"seccional"};

    // ==================== MONITOREO ====================
    private static final int SEGUNDOS_BASE = 100;
    private static final int JITTER_MAXIMO_SEGUNDOS = 45;
    private static final int MAX_CONSULTAS = 130;

    /** Si el arma se aparta, pulsar el CONFIRMAR final para sellar la reserva (avanza al paso 3). */
    private static final boolean CONFIRMAR_CIERRE = false;

    private static final java.util.Random RANDOM = new java.util.Random();

    private static final String URL = "https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite";
    private static final Path SALIDA = Path.of("reconocimiento");
    private static final String SELECTOR_COMBO = "mat-select, [role='combobox'], select";
    private static final String SELECTOR_OPCION =
            "mat-option, [role='option'], .mat-mdc-option, li[role='option']";

    // --- Deteccion del paso 2 (el aviso va en MAYUSCULAS: la bandera i es obligatoria) ---
    private static final String SIN_INVENTARIO = "text=/no hay inventario/i";
    private static final String GUARDANDO = "text=/guardando/i";
    private static final String ENLACE_INVENTARIO =
            "text=/solicitud\\s+adquisici[oó]n\\s+de\\s+armas/i";

    /**
     * Senal de FG-1 ABIERTA. NO sirve "text=FG-1": la tarjeta de la pantalla intermedia y la
     * cabecera de los modales tambien dicen FG-1. La barra inferior con FINALIZAR es exclusiva
     * de la ventana del inventario.
     */
    private static final String PASO_2 = "button:has-text('FINALIZAR')";

    /** Tras sellar la reserva el portal muestra el formulario de la Fiscalia. */
    private static final String PASO_3 = "text=/Formulario antecedentes Fiscal[ií]a/i";

    private static final String FILAS_INVENTARIO =
            "table tbody tr, [role='row']:has([role='gridcell'])";
    private static final String CELDAS_INVENTARIO = "td, [role='gridcell']";
    private static final String FILA_SELECCIONADA = "text=/\\d+\\s*fila[s]?\\s+seleccionada/i";

    // --- Controles del paso 2 ---
    /** Angular Material (mat-dialog, cdk-overlay) y MUI por si acaso. */
    private static final String MODALES =
            "[role='dialog'], mat-dialog-container, .cdk-overlay-pane, .MuiDialog-container";
    private static final String BOTON_PORTE = "button:has-text('PORTE')";
    private static final String BOTON_TENENCIA = "button:has-text('TENENCIA')";
    private static final String BOTON_FINALIZAR = "button:has-text('FINALIZAR')";
    private static final String BOTON_CONFIRMAR = "button:has-text('CONFIRMAR')";
    private static final String BOTON_VOLVER = "button:has-text('VOLVER')";
    private static final String ERROR_CUPO = "text=/no cuenta con cupo disponible/i";

    private static final BufferedReader TECLADO =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private static final List<String> RED = new ArrayList<>();
    private static final List<String> NAVEGACIONES = new ArrayList<>();
    private static final Set<String> URLS_VISTAS = new HashSet<>();
    private static final StringBuilder MONITOREO = new StringBuilder();
    private static final Set<Page> PAGINAS_ENGANCHADAS = new HashSet<>();
    private static BrowserContext contexto;
    private static boolean seccionalesAnunciadas;
    private static int cuerposGuardados;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(SALIDA);
        String canal = args.length > 0 ? args[0] : "msedge";
        if (args.length > 2) {
            System.out.println(">>> AVISO: sobran argumentos " + List.of(args).subList(2, args.length)
                    + ". Separa las seccionales con coma y sin espacios: chrome Medellin,Bogota");
        }
        if (args.length > 1 && !args[1].isBlank()) {
            String[] pedidas = args[1].split(",");
            for (int i = 0; i < pedidas.length; i++) {
                pedidas[i] = pedidas[i].trim();
            }
            seccionalesAMonitorear = pedidas;
        }

        Path perfil = perfilReconocimiento();
        System.out.println("Navegador: " + canal);
        System.out.println("Perfil:    " + perfil);
        System.out.println("Salida:    " + SALIDA.toAbsolutePath());
        System.out.println("Paso 1 -> permiso='" + OBTENER_PERMISO + "'  tipo='" + TIPO_TRAMITE + "'");
        System.out.println("Seccionales a monitorear: " + (seccionalesAMonitorear.length == 0
                ? "(todas las que ofrezca el portal)" : String.join(", ", seccionalesAMonitorear)));
        System.out.println("Espera: " + SEGUNDOS_BASE + "s base + 0.." + JITTER_MAXIMO_SEGUNDOS + "s jitter");

        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {
            contexto = playwright.chromium().launchPersistentContext(perfil,
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel(canal)
                            .setHeadless(false)
                            .setViewportSize((com.microsoft.playwright.options.ViewportSize) null)
                            .setArgs(List.of("--start-maximized", "--no-first-run",
                                    "--no-default-browser-check")));
            contexto.onPage(Reconocimiento::enganchar);
            Page pagina = contexto.pages().isEmpty() ? contexto.newPage() : contexto.pages().getFirst();
            enganchar(pagina);
            pagina.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            try {
                pausa("PASO 1: inicia sesion y resuelve el captcha en la ventana del navegador.");
                esperarPaso1(pagina);
                diagnosticarCombos(pagina);
                volcar(pagina, "01-paso1-inicial");

                boolean formularioListo = false;
                boolean sinInvCapturado = false;
                boolean encontrado = false;

                for (int intento = 1; intento <= MAX_CONSULTAS && !encontrado; intento++) {
                    System.out.println("\n=== Consulta " + intento + " de " + MAX_CONSULTAS + " ===");
                    try {
                        if (!formularioListo || !formularioVivo(pagina)) {
                            prepararPaso1(pagina);
                            formularioListo = true;
                        } else {
                            asegurarCombo(pagina, PISTA_PERMISO, 0, OBTENER_PERMISO);
                            asegurarCombo(pagina, PISTA_TIPO, 1, TIPO_TRAMITE);
                        }

                        String seccional = cambiarSeccionalYGuardar(pagina, intento);
                        System.out.println("Seccional usada: " + seccional);
                        esperarRespuestaDeGuardar(pagina);

                        if (sesionVencida(pagina)) {
                            System.out.println("Sesion vencida; hay que re-loguear.");
                            formularioListo = false;
                            pausa("Inicia sesion de nuevo y pulsa ENTER.");
                            continue;
                        }

                        if (hayMensajeSinInventario(pagina)) {
                            if (!sinInvCapturado) {
                                capturarSinInventario(pagina);
                                sinInvCapturado = true;
                            }
                            capturarEstadoActual(pagina, intento);
                            registrarIntento(intento, seccional, pagina);
                            volcarInformes();
                            System.out.println("  ultimas peticiones: " + ultimasUrls(4));

                            int jitter = RANDOM.nextInt(JITTER_MAXIMO_SEGUNDOS + 1);
                            int tiempoEsperaTotal = SEGUNDOS_BASE + jitter;
                            System.out.println("Sin inventario. Esperando " + tiempoEsperaTotal
                                    + " segundos (Base: " + SEGUNDOS_BASE + "s + Jitter: " + jitter
                                    + "s) antes del siguiente intento...");
                            pagina.waitForTimeout(tiempoEsperaTotal * 1000L);
                            continue;
                        }

                        if (hayPantallaIntermedia(pagina) || fg1Abierto(pagina)) {
                            encontrado = true;
                            System.out.println(">>> INVENTARIO DETECTADO en " + seccional);
                            pagina = explorarPaso2(pagina);
                            continue;
                        }

                        capturarEstadoActual(pagina, intento);
                        registrarIntento(intento, seccional, pagina);
                        volcarInformes();
                        System.out.println("  Respuesta no reconocida. Ultimas peticiones: "
                                + ultimasUrls(4));
                        pausa("Mira la ventana. Si hay inventario y no lo detecte, corrige y pulsa ENTER.");
                    } catch (RuntimeException e) {
                        System.out.println("FALLO en la consulta " + intento + ": " + e);
                        formularioListo = false;
                        try {
                            volcar(pagina, "90-fallo-" + intento);
                        } catch (RuntimeException ignorado) {
                            // el navegador pudo cerrarse
                        }
                        pausa("Revisa el navegador. ENTER para seguir.");
                    }
                }

                if (!encontrado) {
                    System.out.println("\nNo aparecio inventario en " + MAX_CONSULTAS + " consultas.");
                    volcar(pagina, "99-ultimo-estado");
                }
            } catch (RuntimeException e) {
                System.out.println("\nFALLO: " + e);
                pausa("El navegador queda abierto. Pulsa ENTER para cerrar.");
            }
            volcarInformes();
            pausa("Listo. Pulsa ENTER para cerrar.");
        }
    }

    // ==================== EXPLORACION DEL PASO 2 ====================

    /**
     * Recorre el paso 2 completo volcando todo lo necesario para escribir los selectores.
     * Devuelve la pagina activa (puede ser una pestana nueva si el portal abre FG-1 aparte).
     */
    private static Page explorarPaso2(Page pagina) throws IOException {
        System.out.println("\n########## EXPLORACION DEL PASO 2 ##########");

        // --- A. Pantalla intermedia con el enlace ---
        if (hayPantallaIntermedia(pagina)) {
            volcar(pagina, "10-intermedia");
            volcarEstructura(pagina, "10-intermedia-estructura.txt");
            System.out.println("  Enlace detectado: '" + limpiar(textoDe(enlaceInventario(pagina))) + "'");
            pausa("PANTALLA INTERMEDIA volcada (10-*). Debe aparecer el enlace al inventario. "
                    + "Si el texto es otro, anotalo.");
        } else {
            System.out.println("No hay pantalla intermedia: FG-1 ya esta abierto.");
        }

        // --- B. Clic en el enlace, detectando pestana nueva ---
        if (!fg1Abierto(pagina)) {
            Locator enlace = enlaceInventario(pagina);
            if (enlace.count() == 0) {
                System.out.println("No encontre el enlace. Haz clic TU en el enlace del navegador.");
                pausa("Clic manual en el enlace del inventario. ENTER cuando este abierto.");
            } else {
                Set<Page> antes = new HashSet<>(contexto.pages());
                System.out.println(">>> Pulsando el enlace del inventario.");
                enlace.click();
                System.out.println(">>> Enlace pulsado. Buscando la ventana FG-1...");
                Page nueva = esperarPaginaNueva(antes, pagina, 8000);
                if (nueva != null) {
                    System.out.println(">>> FG-1 se abrio en una PESTANA NUEVA. url=" + nueva.url());
                    escribir("10-pestana-nueva.txt",
                            "FG-1 se abrio en una pestana nueva. url=" + nueva.url());
                    pagina = nueva;
                } else {
                    escribir("10-pestana-nueva.txt",
                            "FG-1 se abrio en la MISMA pestana (no hubo pestana nueva).");
                }
                pagina.waitForTimeout(2500);
            }
        }

        if (!fg1Abierto(pagina)) {
            System.out.println(">>> AVISO: no detecte la barra con FINALIZAR. "
                    + "Botones visibles: " + textosDe(pagina.locator("button")));
            pausa("No detecte FG-1. Acomoda la ventana a mano y pulsa ENTER.");
        }
        volcar(pagina, "11-fg1");
        volcarEstructura(pagina, "11-fg1-estructura.txt");
        pausa("FG-1 volcado (11-*). Revisa que se vean los botones PORTE y TENENCIA y la tabla.");

        // --- C. Tabla de inventario ---
        volcarTabla(pagina, "12-tabla");
        pausa("Estructura de la tabla volcada (12-*). Revisa cuantas filas y columnas detecte.");

        // --- D. Botones de la pantalla ---
        volcarBotones(pagina, "13-botones.txt");
        pausa("Botones volcados (13-botones.txt). Anota los textos EXACTOS de PORTE, TENENCIA "
                + "y FINALIZAR si son distintos de los que espero.");

        // --- E. Seleccionar la primera fila ---
        List<Locator> filas = pagina.locator(FILAS_INVENTARIO).all();
        if (filas.isEmpty()) {
            pausa("No detecte filas. Selecciona una fila TU y pulsa ENTER.");
        } else {
            Locator fila = filas.getFirst();
            fila.scrollIntoViewIfNeeded();
            fila.click();
            pagina.waitForTimeout(900);
            escribir("14-fila-seleccionada.txt",
                    "Texto de confirmacion de seleccion:\n"
                            + textoDe(pagina.locator("body")) + "\n\n"
                            + "Selector de la fila: " + selectorDe(fila) + "\n"
                            + "attrs de la fila:   " + attrsDe(fila) + "\n");
            captura(pagina, "14-fila-seleccionada");
            System.out.println("  fila.click() hecho. Fila: " + limpiar(fila.innerText()));
            System.out.println("  Texto de seleccion: "
                    + limpiar(textoDe(pagina.locator(FILA_SELECCIONADA).first())));
            pausa("Fila seleccionada (14-*). Verifica que la fila quede resaltada en el navegador.");
        }

        // --- F. Modal de PORTE ---
        clicYVolcarModal(pagina, BOTON_PORTE, "PORTE", "15-modal-porte");

        // --- G. Volver y abrir el modal de TENENCIA ---
        clicSiExiste(pagina, BOTON_VOLVER);
        pagina.waitForTimeout(800);
        clicYVolcarModal(pagina, BOTON_TENENCIA, "TENENCIA", "17-modal-tenencia");

        // --- H. Volver, abrir PORTE otra vez y CONFIRMAR ---
        clicSiExiste(pagina, BOTON_VOLVER);
        pagina.waitForTimeout(800);
        Locator botonPorte = pagina.locator(BOTON_PORTE).first();
        if (botonPorte.count() > 0) {
            botonPorte.click();
            pagina.waitForTimeout(1200);
        }
        Locator modal = modalVisible(pagina);
        if (modal != null) {
            Locator confirmar = modal.locator(BOTON_CONFIRMAR).first();
            if (confirmar.count() > 0) {
                confirmar.click();
                // El snackbar de cupo es efimero: se busca enseguida.
                boolean cupo = esperarErrorDeCupo(modal);
                escribir("18-tras-confirmar.txt",
                        "url: " + pagina.url() + "\n"
                                + "errorCupo=" + cupo + "\n"
                                + "modalAbierto=" + (modalVisible(pagina) != null) + "\n\n"
                                + textoDe(pagina.locator("body")));
                captura(pagina, "18-tras-confirmar");
                pausa(cupo
                        ? "SALIO EL ERROR DE CUPO (18-*). Verifica el texto en 18-tras-confirmar.txt."
                        : "CONFIRMAR pulsado (18-*). Verifica si el arma quedo pre-asignada.");
            }
        } else {
            pausa("No encontre el modal para CONFIRMAR. Hazlo a mano y pulsa ENTER.");
        }

        // --- I. FINALIZAR ---
        Locator finalizar = pagina.locator(BOTON_FINALIZAR).first();
        if (finalizar.count() > 0) {
            finalizar.click();
            pagina.waitForTimeout(2000);
            volcarModal(pagina, "19-modal-finalizar");
            volcar(pagina, "19-finalizar");
            pausa("FINALIZAR pulsado (19-*). Aqui aparece el modal de cierre con \"Ver Armas\".");
            if (CONFIRMAR_CIERRE) {
                Locator cierre = modalVisible(pagina);
                if (cierre != null) {
                    cierre.locator(BOTON_CONFIRMAR).first().click();
                    pagina.waitForTimeout(3000);
                    escribir("20-paso3.txt", "url: " + pagina.url() + "\n"
                            + "paso3=" + pagina.locator(PASO_3).count() + "\n\n"
                            + textoDe(pagina.locator("body")));
                    captura(pagina, "20-paso3");
                    System.out.println(">>> CONFIRMAR final pulsado: el tramite avanzo al paso 3.");
                }
            } else {
                System.out.println(">>> Deje el CONFIRMAR final SIN pulsar (CONFIRMAR_CIERRE=false).");
            }
        } else {
            pausa("No encontre FINALIZAR. Pulsalo a mano y luego ENTER.");
        }

        volcar(pagina, "21-estado-final");
        pausa("EXPLORACION TERMINADA. Todo en la carpeta 'reconocimiento'.");
        return pagina;
    }

    private static void clicYVolcarModal(Page pagina, String selector, String nombre, String archivo)
            throws IOException {
        Locator boton = pagina.locator(selector).first();
        if (boton.count() == 0) {
            System.out.println("  No encontre el boton " + nombre + ". Pulsalo a mano.");
            pausa("Abre el modal de " + nombre + " a mano y pulsa ENTER.");
        } else {
            boton.scrollIntoViewIfNeeded();
            boton.click();
            pagina.waitForTimeout(1500);
        }
        volcarModal(pagina, archivo);
        volcar(pagina, archivo + "-pagina");
        Locator modal = modalVisible(pagina);
        if (modal == null) {
            System.out.println("  AVISO: no detecte ningun modal abierto tras " + nombre + ".");
            System.out.println("  Contenedores probados: " + MODALES);
            System.out.println("  Elementos con role=dialog en la pagina: "
                    + pagina.locator("[role='dialog'], mat-dialog-container").count());
        } else {
            System.out.println("  modal " + nombre + " detectado. Botones: "
                    + textosDe(modal.locator("button, [role='button']")));
            System.out.println("  etiquetas: "
                    + textosDe(modal.locator("mat-label, label, .mat-mdc-floating-label")));
        }
        pausa("Modal de " + nombre + " volcado (" + archivo + "-*). Revisa el HTML del modal.");
    }

    /** Volca el HTML de cada modal visible y de sus controles. */
    private static void volcarModal(Page pagina, String nombre) throws IOException {
        List<Locator> modales = pagina.locator(MODALES).all();
        StringBuilder informe = new StringBuilder("# Modales detectados: " + modales.size() + "\n\n");
        int visibles = 0;
        for (int i = 0; i < modales.size(); i++) {
            Locator modal = modales.get(i);
            boolean visible;
            try {
                visible = modal.isVisible();
            } catch (RuntimeException e) {
                continue;
            }
            informe.append("## Modal ").append(i).append(" visible=").append(visible).append('\n');
            informe.append("selector: ").append(seguro(() -> selectorDe(modal))).append('\n');
            informe.append("attrs:    ").append(seguro(() -> attrsDe(modal))).append('\n');
            informe.append("texto:    ").append(seguro(() -> limpiar(modal.innerText()))).append('\n');
            if (!visible) {
                informe.append('\n');
                continue;
            }
            visibles++;
            escribir(nombre + "-modal-" + i + ".html",
                    seguro(() -> (String) modal.evaluate("el => el.outerHTML")));

            informe.append("etiquetas:\n");
            for (Locator l : modal.locator("mat-label, label, .mat-mdc-floating-label").all()) {
                informe.append("   - '").append(seguro(() -> limpiar(l.innerText())))
                        .append("'  selector: ").append(seguro(() -> selectorDe(l))).append('\n');
            }
            informe.append("botones:\n");
            for (Locator b : modal.locator("button, [role='button'], a").all()) {
                informe.append("   - '").append(seguro(() -> limpiar(b.innerText())))
                        .append("'  selector: ").append(seguro(() -> selectorDe(b)))
                        .append("  habilitado=").append(seguro(() -> String.valueOf(b.isEnabled())))
                        .append('\n');
            }
            informe.append("campos:\n");
            for (Locator c : modal.locator(
                    "input, textarea, mat-select, [role='combobox'], [role='spinbutton']").all()) {
                informe.append("   - ").append(seguro(() -> attrsDe(c)))
                        .append("  valor='").append(seguro(() -> valorDe(c))).append("'\n");
            }
            informe.append("cantidad (botones - y +):\n");
            for (Locator c : modal.locator(
                    "button:has-text('+'), button:has-text('-'), button:has-text('−'), "
                            + "[aria-label*='umentar'], [aria-label*='isminuir'], "
                            + "[aria-label*='umar'], input[type='number']").all()) {
                informe.append("   - '").append(seguro(() -> limpiar(c.innerText())))
                        .append("'  attrs: ").append(seguro(() -> attrsDe(c))).append('\n');
            }
            informe.append('\n');
        }
        informe.insert(0, "Visibles: " + visibles + "\n");
        escribir(nombre + ".txt", informe.toString());
    }

    private static void volcarTabla(Page pagina, String nombre) throws IOException {
        StringBuilder informe = new StringBuilder("# Estructura de la tabla de inventario\n\n");
        Locator filas = pagina.locator(FILAS_INVENTARIO);
        informe.append("filas detectadas: ").append(filas.count()).append('\n');
        informe.append("tablas <table>: ").append(pagina.locator("table").count()).append('\n');
        informe.append("mat-table: ").append(pagina.locator("mat-table, table[mat-table]").count()).append('\n');
        informe.append("roles row: ").append(pagina.locator("[role='row']").count()).append('\n');
        informe.append("roles gridcell: ").append(pagina.locator("[role='gridcell']").count()).append('\n');
        informe.append("roles columnheader: ").append(pagina.locator("[role='columnheader']").count()).append('\n');
        informe.append("thead/th: ").append(pagina.locator("th").count()).append('\n');
        informe.append("mat-paginator: ").append(pagina.locator("mat-paginator").count()).append("\n\n");

        informe.append("## Encabezados\n");
        for (Locator th : pagina.locator("th, [role='columnheader']").all()) {
            informe.append("- '").append(seguro(() -> limpiar(th.innerText())))
                    .append("'  selector: ").append(seguro(() -> selectorDe(th))).append('\n');
        }

        informe.append("\n## Primeras 10 filas\n");
        List<Locator> lista = filas.all().stream().limit(10).toList();
        for (int i = 0; i < lista.size(); i++) {
            Locator fila = lista.get(i);
            informe.append("### Fila ").append(i).append('\n');
            informe.append("attrs:    ").append(seguro(() -> attrsDe(fila))).append('\n');
            informe.append("selector: ").append(seguro(() -> selectorDe(fila))).append('\n');
            informe.append("texto:    ").append(seguro(() -> limpiar(fila.innerText()))).append('\n');
            for (Locator celda : fila.locator(CELDAS_INVENTARIO).all()) {
                informe.append("   celda '").append(seguro(() -> limpiar(celda.innerText())))
                        .append("'  attrs: ").append(seguro(() -> attrsDe(celda))).append('\n');
            }
            informe.append('\n');
        }
        informe.append("\n## Texto de seleccion en reposo\n");
        informe.append(seguro(() -> limpiar(textoDe(pagina.locator(FILA_SELECCIONADA).first()))));
        escribir(nombre + ".txt", informe.toString());
        escribir(nombre + "-filas.html", seguro(() -> (String) pagina.evaluate(
                "() => { const t = document.querySelector('table, [role=grid]');"
                        + " return t ? t.outerHTML.slice(0, 200000) : '(sin tabla)'; }")));
    }

    private static void volcarBotones(Page pagina, String nombre) throws IOException {
        StringBuilder informe = new StringBuilder("# Botones y enlaces visibles\n\n");
        for (Locator b : pagina.locator("button, [role='button'], a, input[type='submit']").all()) {
            try {
                if (!b.isVisible()) {
                    continue;
                }
            } catch (RuntimeException e) {
                continue;
            }
            informe.append("- '").append(seguro(() -> limpiar(b.innerText())))
                    .append("'  tag=").append(seguro(() -> (String) b.evaluate("el => el.tagName.toLowerCase()")))
                    .append("  selector: ").append(seguro(() -> selectorDe(b)))
                    .append("  habilitado=").append(seguro(() -> String.valueOf(b.isEnabled())))
                    .append('\n');
        }
        escribir(nombre, informe.toString());
    }

    private static void volcarEstructura(Page pagina, String nombre) throws IOException {
        StringBuilder informe = new StringBuilder("# Roles y controles de la pantalla\n\n");
        informe.append("url: ").append(pagina.url()).append("\n\n");
        informe.append("## Elementos con role\n");
        for (Locator el : pagina.locator("[role]").all()) {
            String texto = seguro(() -> limpiar(el.innerText()));
            informe.append("- role=").append(seguro(() -> String.valueOf(el.getAttribute("role"))))
                    .append("  tag=").append(seguro(() -> (String) el.evaluate("el => el.tagName.toLowerCase()")))
                    .append("  texto='").append(texto, 0, Math.min(60, texto.length()))
                    .append("'\n");
        }
        informe.append("\n## Enlaces\n");
        for (Locator a : pagina.locator("a").all()) {
            informe.append("- '").append(seguro(() -> limpiar(a.innerText())))
                    .append("'  href=").append(seguro(() -> String.valueOf(a.getAttribute("href"))))
                    .append('\n');
        }
        escribir(nombre, informe.toString());
    }

    private static Locator modalVisible(Page pagina) {
        List<Locator> modales = pagina.locator(MODALES).all();
        for (int i = modales.size() - 1; i >= 0; i--) {
            try {
                if (modales.get(i).isVisible()) {
                    return modales.get(i);
                }
            } catch (RuntimeException ignorado) {
                // el modal se re-renderizo
            }
        }
        return null;
    }

    /** El snackbar de cupo vive pocos segundos: bucle corto y se corta al cerrarse el modal. */
    private static boolean esperarErrorDeCupo(Locator modal) {
        long limite = System.currentTimeMillis() + 6000;
        long cierreEn = 0L;
        while (System.currentTimeMillis() < limite) {
            if (paginaDe(modal).locator(ERROR_CUPO).count() > 0) {
                return true;
            }
            boolean visible;
            try {
                visible = modal.isVisible();
            } catch (RuntimeException e) {
                visible = false;
            }
            if (cierreEn == 0L && !visible) {
                cierreEn = System.currentTimeMillis();
            }
            if (cierreEn != 0L && System.currentTimeMillis() - cierreEn > 1500) {
                return false;
            }
            pausaCorta();
        }
        return false;
    }

    private static Page paginaDe(Locator elemento) {
        return (Page) elemento.evaluateHandle("el => el.ownerDocument.defaultView").asElement() != null
                ? paginaActual()
                : paginaActual();
    }

    private static Page PAGINA_ACTUAL;

    private static Page paginaActual() {
        return PAGINA_ACTUAL != null ? PAGINA_ACTUAL : contexto.pages().getFirst();
    }

    private static void pausaCorta() {
        try {
            Thread.sleep(120);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Enlace de la pantalla intermedia, con respaldo por si no es un <a>. */
    private static Locator enlaceInventario(Page pagina) {
        for (String candidato : new String[]{
                "a:has-text('adquisici')",
                ENLACE_INVENTARIO,
                "mat-card:has-text('adquisici')"}) {
            Locator locator = pagina.locator(candidato);
            if (locator.count() > 0) {
                return locator.first();
            }
        }
        return pagina.locator(ENLACE_INVENTARIO).first();
    }

    private static boolean hayPantallaIntermedia(Page pagina) {
        return enlaceInventario(pagina).count() > 0 && !fg1Abierto(pagina);
    }

    private static boolean fg1Abierto(Page pagina) {
        return pagina.locator(PASO_2).count() > 0;
    }

    /** Espera a que GUARDAR termine: desaparece el "GUARDANDO...", o aparece cualquier resultado. */
    private static void esperarRespuestaDeGuardar(Page pagina) {
        long limite = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < limite) {
            if (hayMensajeSinInventario(pagina) || hayPantallaIntermedia(pagina) || fg1Abierto(pagina)) {
                return;
            }
            if (pagina.locator(GUARDANDO).count() == 0
                    && pagina.locator(SelectoresSiae.BOTON_GUARDAR).count() > 0) {
                pagina.waitForTimeout(500);
                return;
            }
            pagina.waitForTimeout(300);
        }
    }

    private static Page esperarPaginaNueva(Set<Page> antes, Page anterior, long timeoutMs) {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            for (Page candidata : contexto.pages()) {
                if (!antes.contains(candidata) && !candidata.isClosed()) {
                    return candidata;
                }
            }
            anterior.waitForTimeout(200);
        }
        return null;
    }

    private static void enganchar(Page pagina) {
        if (!PAGINAS_ENGANCHADAS.add(pagina)) {
            return;
        }
        registrarRed(pagina);
        registrarNavegacion(pagina);
    }

    // ==================== DIAGNOSTICO ====================

    private static void diagnosticarCombos(Page pagina) throws IOException {
        StringBuilder informe = new StringBuilder("# Combos candidatos en la pagina\n\n");
        Locator combos = pagina.locator(SELECTOR_COMBO);
        int total = combos.count();
        informe.append("Total detectado: ").append(total).append("\n\n");
        for (int i = 0; i < total; i++) {
            Locator combo = combos.nth(i);
            informe.append("## Candidato ").append(i).append('\n');
            informe.append("tag:        ").append(seguro(() -> String.valueOf(
                    combo.evaluate("el => el.tagName.toLowerCase()")))).append('\n');
            informe.append("attrs:      ").append(seguro(() -> attrsDe(combo))).append('\n');
            informe.append("placeholder: ").append(seguro(() -> String.valueOf(combo.getAttribute("placeholder")))).append('\n');
            informe.append("value:      ").append(seguro(() -> valorDe(combo))).append('\n');
            informe.append("texto:      ").append(seguro(() -> limpiar(combo.innerText()))).append('\n');
            informe.append("etiqueta:   ").append(seguro(() -> etiquetaDe(combo))).append('\n');
            informe.append("visible:    ").append(seguro(() -> String.valueOf(combo.isVisible()))).append('\n');
            informe.append("selector:   ").append(seguro(() -> selectorDe(combo))).append("\n\n");
        }
        escribir("00-combos.txt", informe.toString());
        System.out.println("Diagnostico de combos -> '00-combos.txt' (" + total + " candidatos).");
    }

    private static String etiquetaDe(Locator elemento) {
        return String.valueOf(elemento.evaluate("""
                el => {
                  const id = el.getAttribute('aria-labelledby');
                  if (id) { const l = document.getElementById(id); if (l) return l.textContent; }
                  const ff = el.closest('mat-form-field, .mat-mdc-form-field');
                  if (ff) { const lab = ff.querySelector('mat-label, label'); if (lab) return lab.textContent; }
                  return el.getAttribute('aria-label') || '';
                }
                """));
    }

    // ==================== PASO 1 ====================

    private static void prepararPaso1(Page pagina) {
        pagina.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        if (sesionVencida(pagina)) {
            pausa("La sesion no esta activa. Inicia sesion y resuelve el captcha; luego pulsa ENTER.");
            pagina.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        }
        esperarPaso1(pagina);
        elegirEnCombo(pagina, PISTA_PERMISO, 0, OBTENER_PERMISO);
        elegirEnCombo(pagina, PISTA_TIPO, 1, TIPO_TRAMITE);
        escribirDescripcion(pagina);
    }

    private static void asegurarCombo(Page pagina, String[] pistas, int indice, String valor) {
        Locator combo = buscarCombo(pagina, pistas, indice);
        if (valorDe(combo).isBlank()) {
            elegirEnCombo(pagina, pistas, indice, valor);
        }
    }

    private static String cambiarSeccionalYGuardar(Page pagina, int intento) {
        abrirCombo(pagina, PISTA_SECCIONAL, 2);
        List<String> seccionales = leerOpciones(pagina);
        String seccional = seccionalDeTurno(seccionales, intento);
        clicarOpcion(pagina, seccional, seccionales);
        escribirDescripcion(pagina);
        pulsarGuardar(pagina);
        return seccional;
    }

    private static void esperarPaso1(Page pagina) {
        pagina.waitForSelector(SELECTOR_COMBO, new Page.WaitForSelectorOptions().setTimeout(45_000));
    }

    private static boolean formularioVivo(Page pagina) {
        Locator combos = pagina.locator(SELECTOR_COMBO);
        int visibles = 0;
        for (int i = 0; i < combos.count(); i++) {
            if (combos.nth(i).isVisible()) {
                visibles++;
            }
        }
        return visibles >= 3;
    }

    private static void elegirEnCombo(Page pagina, String[] pistas, int indice, String valor) {
        abrirCombo(pagina, pistas, indice);
        List<String> opciones = leerOpciones(pagina);
        clicarOpcion(pagina, valor, opciones);
    }

    private static void abrirCombo(Page pagina, String[] pistas, int indice) {
        Locator combo = buscarCombo(pagina, pistas, indice);
        for (int intento = 1; intento <= 3; intento++) {
            combo.scrollIntoViewIfNeeded();
            combo.click(new Locator.ClickOptions().setForce(intento > 1));
            long limite = System.currentTimeMillis() + 4000;
            while (pagina.locator(SELECTOR_OPCION).count() == 0 && System.currentTimeMillis() < limite) {
                pagina.waitForTimeout(150);
            }
            if (pagina.locator(SELECTOR_OPCION).count() > 0) {
                return;
            }
        }
        throw new RuntimeException("No se pudo abrir el combo [" + indice + "].");
    }

    private static List<String> leerOpciones(Page pagina) {
        List<String> textos = new ArrayList<>();
        Locator opciones = pagina.locator(SELECTOR_OPCION);
        for (int i = 0; i < opciones.count(); i++) {
            textos.add(limpiar(opciones.nth(i).innerText()));
        }
        return textos;
    }

    private static void clicarOpcion(Page pagina, String valor, List<String> textos) {
        int idx = buscarOpcion(textos, valor);
        if (idx < 0) {
            pagina.keyboard().press("Escape");
            throw new RuntimeException("Ninguna opcion contiene '" + valor + "'. Opciones: " + textos);
        }
        pagina.locator(SELECTOR_OPCION).nth(idx).click();
        pagina.waitForTimeout(1000);
        System.out.println("  elegido: " + textos.get(idx));
    }

    private static Locator buscarCombo(Page pagina, String[] pistas, int indice) {
        Locator combos = pagina.locator(SELECTOR_COMBO);
        int total = combos.count();
        for (String pista : pistas) {
            String aguja = normalizar(pista);
            for (int i = 0; i < total; i++) {
                Locator c = combos.nth(i);
                String heno = normalizar(seguro(() -> limpiar(c.innerText())) + " "
                        + seguro(() -> limpiar(etiquetaDe(c))) + " "
                        + seguro(() -> String.valueOf(c.getAttribute("placeholder"))));
                if (heno.contains(aguja)) {
                    return c;
                }
            }
        }
        if (indice < total) {
            return combos.nth(indice);
        }
        throw new RuntimeException("No se encontro el combo con pistas " + List.of(pistas)
                + " ni por posicion " + indice + " (candidatos: " + total + ").");
    }

    private static String seccionalDeTurno(List<String> seccionales, int intento) {
        if (seccionales.isEmpty()) {
            throw new RuntimeException("La lista de seccionales esta vacia. Revisa permiso y tipo de tramite.");
        }
        List<String> orden = seccionalesAUsar(seccionales);
        return orden.get((intento - 1) % orden.size());
    }

    private static List<String> seccionalesAUsar(List<String> disponibles) {
        List<String> elegidas = new ArrayList<>();
        List<String> sinMatch = new ArrayList<>();

        if (seccionalesAMonitorear.length == 0) {
            elegidas.addAll(disponibles);
        } else {
            for (String pista : seccionalesAMonitorear) {
                String aguja = normalizar(pista);
                boolean hallada = false;
                for (String opcion : disponibles) {
                    if (normalizar(opcion).contains(aguja)) {
                        if (!elegidas.contains(opcion)) {
                            elegidas.add(opcion);
                        }
                        hallada = true;
                        break;
                    }
                }
                if (!hallada) {
                    sinMatch.add(pista);
                }
            }
        }

        if (elegidas.isEmpty()) {
            System.out.println("  AVISO: ninguna seccional pedida existe en el portal. Pedidas: "
                    + List.of(seccionalesAMonitorear));
            System.out.println("  Disponibles: " + disponibles);
            System.out.println("  Se usaran todas las disponibles.");
            elegidas.addAll(disponibles);
        }
        if (!seccionalesAnunciadas) {
            if (!sinMatch.isEmpty()) {
                System.out.println("  AVISO: estas seccionales pedidas no aparecen en el portal: " + sinMatch);
            }
            System.out.println("Seccionales a monitorear (" + elegidas.size() + "): " + elegidas);
            seccionalesAnunciadas = true;
        }
        return elegidas;
    }

    private static void escribirDescripcion(Page pagina) {
        Locator campo = buscarDescripcion(pagina);
        if (campo == null) {
            return;
        }
        campo.scrollIntoViewIfNeeded();
        campo.click();
        campo.fill(DESCRIPCION);
        pagina.waitForTimeout(200);
    }

    private static Locator buscarDescripcion(Page pagina) {
        for (String candidato : new String[]{SelectoresSiae.CAMPO_DESCRIPCION,
                "textarea[formcontrolname]", "textarea", "[maxlength='500']", "input[maxlength='500']"}) {
            Locator locator = pagina.locator(candidato);
            if (locator.count() > 0) {
                return locator.first();
            }
        }
        return null;
    }

    private static void pulsarGuardar(Page pagina) {
        Locator guardar = pagina.locator(SelectoresSiae.BOTON_GUARDAR).first();
        guardar.scrollIntoViewIfNeeded();
        guardar.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
        if (!guardar.isEnabled()) {
            throw new RuntimeException("El boton GUARDAR esta deshabilitado.");
        }
        guardar.click();
    }

    // ==================== DETECCION ====================

    private static boolean sesionVencida(Page pagina) {
        return pagina.locator(SelectoresSiae.LOGIN_PASSWORD).count() > 0;
    }

    private static boolean hayMensajeSinInventario(Page pagina) {
        return pagina.locator(SIN_INVENTARIO).count() > 0;
    }

    private static void clicSiExiste(Page pagina, String selector) {
        Locator boton = pagina.locator(selector).first();
        if (boton.count() > 0) {
            try {
                boton.scrollIntoViewIfNeeded();
                boton.click();
                pagina.waitForTimeout(700);
            } catch (RuntimeException e) {
                System.out.println("  No pude pulsar " + selector + ": " + e.getMessage());
            }
        }
    }

    // ==================== CAPTURA ====================

    private static void capturarSinInventario(Page pagina) throws IOException {
        System.out.println(">>> Estado 'sin inventario' capturado (05-sin-inventario).");
        escribir("05-sin-inventario.html", pagina.content());
        escribir("05-sin-inventario-elementos.txt", informeElementos(pagina, "sin inventario"));
        escribir("05-sin-inventario-texto.txt", pagina.locator("body").innerText());
        captura(pagina, "05-sin-inventario");
    }

    /** Captura rodante del estado actual, para ver como cambia la pagina ciclo a ciclo. */
    private static void capturarEstadoActual(Page pagina, int intento) throws IOException {
        escribir("05-actual.html", pagina.content());
        escribir("05-actual-texto.txt", "URL: " + pagina.url() + "\n\n" + pagina.locator("body").innerText());
        captura(pagina, String.format("05-actual-%03d", intento));
    }

    // ==================== UTILIDADES ====================

    private static Path perfilReconocimiento() {
        String appdata = System.getenv("APPDATA");
        Path base = (appdata != null && !appdata.isBlank())
                ? Path.of(appdata)
                : Path.of(System.getProperty("user.home"), "Library", "Application Support");
        return base.resolve("MonitorSIAE").resolve("perfiles").resolve("_reconocimiento");
    }

    private static String hora() {
        return LocalTime.now().withNano(0).toString();
    }

    private static List<String> ultimasUrls(int cuantas) {
        int desde = Math.max(0, RED.size() - cuantas);
        return new ArrayList<>(RED.subList(desde, RED.size()));
    }

    private static void registrarIntento(int intento, String seccional, Page pagina) {
        MONITOREO.append(hora()).append("  Consulta ").append(intento)
                .append("  seccional=").append(seccional)
                .append("  url=").append(pagina.url())
                .append("  filas=").append(pagina.locator(FILAS_INVENTARIO).count())
                .append("  enlace=").append(enlaceInventario(pagina).count())
                .append("  fg1=").append(pagina.locator(PASO_2).count())
                .append("  avisoNoInventario=").append(pagina.locator(SIN_INVENTARIO).count())
                .append('\n');
        try {
            escribir("monitoreo.txt", MONITOREO.toString());
        } catch (IOException ignorado) {
            // best-effort
        }
    }

    private static void volcarInformes() {
        try {
            escribir("red.txt", String.join(System.lineSeparator(), RED));
            escribir("navegacion.txt", String.join(System.lineSeparator(), NAVEGACIONES));
        } catch (IOException ignorado) {
            // best-effort
        }
    }

    private static void volcar(Page pagina, String nombre) throws IOException {
        PAGINA_ACTUAL = pagina;
        escribir(nombre + ".html", pagina.content());
        escribir(nombre + "-elementos.txt", informeElementos(pagina, nombre));
        escribir(nombre + "-texto.txt", "URL: " + pagina.url() + "\n\n"
                + pagina.locator("body").innerText());
        captura(pagina, nombre);
    }

    private static String informeElementos(Page pagina, String nombre) {
        List<Locator> elementos = pagina.locator(
                "input, textarea, select, button, a, [role], [formcontrolname], mat-select, "
                        + "mat-option, table, th, td").all();
        StringBuilder informe = new StringBuilder("# ").append(nombre).append('\n');
        informe.append("URL: ").append(pagina.url()).append('\n');
        informe.append("Elementos: ").append(elementos.size()).append("\n\n");
        for (int i = 0; i < elementos.size(); i++) {
            Locator elemento = elementos.get(i);
            try {
                String caja = String.valueOf(elemento.evaluate(
                        "el => { const r = el.getBoundingClientRect();"
                                + " return [Math.round(r.x), Math.round(r.y), Math.round(r.width),"
                                + " Math.round(r.height)].join(','); }"));
                if (caja.startsWith("0,0,0,0")) {
                    continue;
                }
                informe.append(i).append(". ").append(elemento.evaluate("el => el.tagName.toLowerCase()"))
                        .append("  caja=").append(caja).append('\n')
                        .append("   selector: ").append(selectorDe(elemento)).append('\n')
                        .append("   attrs:    ").append(attrsDe(elemento)).append('\n')
                        .append("   texto:    ").append(limpiar(elemento.innerText())).append("\n\n");
            } catch (RuntimeException ignorado) {
                // el elemento desaparecio del DOM
            }
        }
        return informe.toString();
    }

    private static String selectorDe(Locator elemento) {
        return String.valueOf(elemento.evaluate("""
                el => {
                  if (el.id) return '#' + el.id;
                  for (const a of ['formcontrolname','name','aria-label','data-testid','placeholder','role','type']) {
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
                }
                """));
    }

    private static String attrsDe(Locator elemento) {
        return String.valueOf(elemento.evaluate("el => JSON.stringify(Object.fromEntries("
                + "el.getAttributeNames().map(n => [n, el.getAttribute(n)])))"));
    }

    private static String valorDe(Locator elemento) {
        try {
            return elemento.inputValue();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String textoDe(Locator elemento) {
        try {
            return elemento.innerText();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static List<String> textosDe(Locator elementos) {
        List<String> textos = new ArrayList<>();
        for (Locator el : elementos.all()) {
            textos.add(seguro(() -> limpiar(el.innerText())));
        }
        return textos;
    }

    private static void registrarRed(Page pagina) {
        pagina.onResponse(respuesta -> {
            String tipo = respuesta.request().resourceType();
            if (!tipo.equals("xhr") && !tipo.equals("fetch")) {
                return;
            }
            RED.add(hora() + "  " + respuesta.request().method() + " " + respuesta.status() + " "
                    + respuesta.headerValue("content-type") + " " + respuesta.url());
            try {
                if (cuerposGuardados < 400) {
                    String cuerpo = respuesta.text();
                    if (cuerpo.length() > 600_000) {
                        cuerpo = cuerpo.substring(0, 600_000);
                    }
                    String contentType = String.valueOf(respuesta.headerValue("content-type"));
                    String ext = contentType.contains("json") ? "json" : "txt";
                    Files.writeString(SALIDA.resolve(String.format("red-%03d.%s", cuerposGuardados++, ext)),
                            "URL: " + respuesta.url() + "\n\n" + cuerpo, StandardCharsets.UTF_8);
                }
            } catch (RuntimeException | IOException ignorado) {
                // el cuerpo puede no estar disponible
            }
        });
    }

    private static void registrarNavegacion(Page pagina) {
        pagina.onFrameNavigated(frame -> {
            String url = frame.url();
            if (URLS_VISTAS.add(url)) {
                NAVEGACIONES.add(hora() + "  " + url);
            }
        });
    }

    private static void captura(Page pagina, String nombre) throws IOException {
        pagina.screenshot(new Page.ScreenshotOptions().setPath(SALIDA.resolve(nombre + ".png")));
    }

    private static void escribir(String nombre, String contenido) throws IOException {
        Files.writeString(SALIDA.resolve(nombre), contenido, StandardCharsets.UTF_8);
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").strip();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static int buscarOpcion(List<String> textos, String valor) {
        String buscado = normalizar(valor);
        for (int i = 0; i < textos.size(); i++) {
            if (normalizar(textos.get(i)).contains(buscado)) {
                return i;
            }
        }
        return -1;
    }

    private static String seguro(Proveedor proveedor) {
        try {
            return proveedor.obtener();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static void pausa(String mensaje) {
        pedir("\n" + mensaje + "\n>>> ENTER para continuar... ");
    }

    private static String pedir(String mensaje) {
        System.out.print(mensaje);
        try {
            String linea = TECLADO.readLine();
            return linea == null ? "" : linea;
        } catch (IOException e) {
            return "";
        }
    }

    @FunctionalInterface
    private interface Proveedor {
        String obtener();
    }
}