package co.monitorsiae.portal.real;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 * <p>Inicia sesion a mano, llena el paso 1 una vez, y luego solo cambia la seccional (de la lista
 * configurada) y pulsa GUARDAR con un intervalo aleatorio (base + jitter) hasta que alguna de esas
 * seccionales tenga inventario. Cuando aparece, vuelca la estructura del paso 2 sin apartar nada.
 */
public final class Reconocimiento {

    // ==================== PARAMETROS DEL PASO 1 (edita aqui) ====================
    private static final String OBTENER_PERMISO = "Defensa Personal";
    private static final String TIPO_TRAMITE = "Armas personas naturales: Adquisición de arma";
    private static final String DESCRIPCION =
            "Solicito la adquisición de un arma para defensa personal, conforme a los requisitos y "
                    + "procedimientos establecidos por la autoridad competente. Me comprometo a "
                    + "suministrar información veraz y a cumplir las obligaciones aplicables.";
    private static final String MODALIDAD = "PORTE";

    /**
     * Seccionales a monitorear, en orden de prioridad. Basta una parte reconocible (no importan
     * tildes ni mayusculas): "Bogota" encuentra "16 - Usaquén, Bogotá D.C.".
     *
     * <p>Lista vacia = se monitorean TODAS las seccionales que ofrezca el portal.
     * Tambien se puede pasar por consola como segundo argumento, separadas por coma:
     * {@code -Dexec.args="chrome Bogota,Medellin"}.
     */
    private static String[] seccionalesAMonitorear = {"Bogota", "Medellin"};

    // Pistas para localizar cada combo (por placeholder, texto o etiqueta). Se prueban en orden.
    private static final String[] PISTA_PERMISO = {"obtener el permiso", "como desea obtener", "permiso"};
    private static final String[] PISTA_TIPO = {"tipo de tramite", "clase de tramite", "tipo tramite"};
    private static final String[] PISTA_SECCIONAL = {"seccional"};

    // ==================== MONITOREO ====================
    // Intervalo base entre consultas (3 minutos) mas un jitter aleatorio para no ser predecible.
    private static final int SEGUNDOS_BASE = 180;
    // Variacion aleatoria maxima adicional (0 a 45 segundos).
    private static final int JITTER_MAXIMO_SEGUNDOS = 45;
    private static final int MAX_CONSULTAS = 120;

    private static final java.util.Random RANDOM = new java.util.Random();

    private static final String URL = "https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite";
    private static final Path SALIDA = Path.of("reconocimiento");
    private static final String SELECTOR_COMBO = "mat-select, [role='combobox'], select";
    private static final String SELECTOR_OPCION =
            "mat-option, [role='option'], .mat-mdc-option, li[role='option']";
    /** Mensaje real del portal (mayusculas): se compara sin distinguir mayusculas. */
    private static final String SIN_INVENTARIO = "text=no hay inventario";
    private static final String FILAS_INVENTARIO = "table tbody tr";
    private static final BufferedReader TECLADO =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private static final List<String> RED = new ArrayList<>();
    private static final StringBuilder MONITOREO = new StringBuilder();
    private static boolean seccionalesAnunciadas;
    private static int cuerposGuardados;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(SALIDA);
        String canal = args.length > 0 ? args[0] : "chrome";
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

        try (Playwright playwright = Playwright.create()) {
            BrowserContext contexto = playwright.chromium().launchPersistentContext(perfil,
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel(canal)
                            .setHeadless(false)
                            .setViewportSize(1600, 900));
            Page pagina = contexto.pages().isEmpty() ? contexto.newPage() : contexto.pages().getFirst();
            registrarRed(pagina);
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
                        // Solo re-llenamos el paso 1 cuando hace falta; lo demas es cambiar la seccional.
                        if (!formularioListo || !formularioVivo(pagina)) {
                            prepararPaso1(pagina);
                            formularioListo = true;
                        } else {
                            asegurarCombo(pagina, PISTA_PERMISO, 0, OBTENER_PERMISO);
                            asegurarCombo(pagina, PISTA_TIPO, 1, TIPO_TRAMITE);
                        }

                        String seccional = cambiarSeccionalYGuardar(pagina, intento);
                        System.out.println("Seccional usada: " + seccional);
                        pagina.waitForTimeout(2000);

                        if (sesionVencida(pagina)) {
                            System.out.println("Sesion vencida; hay que re-loguear.");
                            formularioListo = false;
                            pausa("Inicia sesion de nuevo y pulsa ENTER.");
                            continue;
                        }

                        if (hayInventario(pagina)) {
                            encontrado = true;
                            System.out.println(">>> INVENTARIO DETECTADO en " + seccional);
                            capturarPaso2(pagina, true);
                        } else {
                            if (!sinInvCapturado && hayMensajeSinInventario(pagina)) {
                                capturarSinInventario(pagina);
                                sinInvCapturado = true;
                            }
                            registrarIntento(intento, seccional, pagina);

                            // Espera dinamica y aleatoria para no ser predecible con el portal.
                            int jitter = RANDOM.nextInt(JITTER_MAXIMO_SEGUNDOS + 1);
                            int tiempoEsperaTotal = SEGUNDOS_BASE + jitter;
                            System.out.println("Sin inventario. Esperando " + tiempoEsperaTotal
                                    + " segundos (Base: " + SEGUNDOS_BASE + "s + Jitter: " + jitter
                                    + "s) antes del siguiente intento...");
                            pagina.waitForTimeout(tiempoEsperaTotal * 1000L);
                        }
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
                    pausa("Pulsa ENTER para cerrar.");
                }
            } catch (RuntimeException e) {
                System.out.println("\nFALLO: " + e);
                pausa("El navegador queda abierto. Pulsa ENTER para cerrar.");
            }
            try {
                escribir("red.txt", String.join(System.lineSeparator(), RED));
            } catch (RuntimeException ignorado) {
                // el navegador pudo cerrarse con Ctrl+C
            }
        }
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

    /** Devuelve la seccional que toca en este intento, rotando SOLO por las de la lista. */
    private static String seccionalDeTurno(List<String> seccionales, int intento) {
        if (seccionales.isEmpty()) {
            throw new RuntimeException("La lista de seccionales esta vacia. Revisa permiso y tipo de tramite.");
        }
        List<String> orden = seccionalesAUsar(seccionales);
        return orden.get((intento - 1) % orden.size());
    }

    /** Filtra las seccionales disponibles a las pedidas, en su orden. Lista vacia = todas. */
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
            throw new RuntimeException("Ninguna seccional de la lista existe en el portal. Pedidas: "
                    + List.of(seccionalesAMonitorear) + ". Disponibles: " + disponibles);
        }
        if (!seccionalesAnunciadas) {
            if (!sinMatch.isEmpty()) {
                System.out.println("  AVISO: estas seccionales pedidas no aparecen en el portal: " + sinMatch);
                System.out.println("  Disponibles: " + disponibles);
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

    // ==================== DETECCION DEL PASO 2 ====================

    private static boolean sesionVencida(Page pagina) {
        return pagina.locator(SelectoresSiae.LOGIN_PASSWORD).count() > 0;
    }

    private static boolean hayMensajeSinInventario(Page pagina) {
        return pagina.locator(SIN_INVENTARIO).count() > 0;
    }

    private static boolean hayInventario(Page pagina) {
        if (hayMensajeSinInventario(pagina)) {
            return false;
        }
        return pagina.locator(FILAS_INVENTARIO).count() > 0;
    }

    private static boolean intentarModalidad(Page pagina) {
        Locator combos = pagina.locator(SELECTOR_COMBO);
        for (int i = 0; i < combos.count(); i++) {
            Locator combo = combos.nth(i);
            try {
                combo.scrollIntoViewIfNeeded();
                combo.click();
                pagina.waitForTimeout(600);
                List<String> textos = leerOpciones(pagina);
                int idx = buscarOpcion(textos, MODALIDAD);
                if (idx >= 0) {
                    pagina.locator(SELECTOR_OPCION).nth(idx).click();
                    pagina.waitForTimeout(1500);
                    return true;
                }
                pagina.keyboard().press("Escape");
                pagina.waitForTimeout(300);
            } catch (RuntimeException ignorado) {
                // no era un combo con opciones
            }
        }
        return false;
    }

    // ==================== CAPTURA ====================

    private static void capturarSinInventario(Page pagina) throws IOException {
        System.out.println(">>> Estado 'sin inventario' capturado (05-sin-inventario).");
        escribir("05-sin-inventario.html", pagina.content());
        escribir("05-sin-inventario-elementos.txt", informeElementos(pagina, "sin inventario"));
        escribir("05-sin-inventario-texto.txt", pagina.locator("body").innerText());
        captura(pagina, "05-sin-inventario");
    }

    private static void capturarPaso2(Page pagina, boolean pausar) throws IOException {
        volcar(pagina, "10-paso2-inventario");
        explorarControlesPaso2(pagina);
        volcar(pagina, "11-paso2-final");
        if (pausar) {
            pausa("PASO 2 capturado en 'reconocimiento'. Revisa 10-* y 11-*. ENTER para cerrar.");
        }
    }

    private static void explorarControlesPaso2(Page pagina) throws IOException {
        StringBuilder informe = new StringBuilder("# Controles interactivos del paso 2\n\n");
        Locator combos = pagina.locator(SELECTOR_COMBO);
        informe.append("Combos detectados: ").append(combos.count()).append("\n\n");
        for (int i = 0; i < combos.count(); i++) {
            Locator combo = combos.nth(i);
            informe.append("## Combo ").append(i).append('\n');
            informe.append("selector: ").append(seguro(() -> selectorDe(combo))).append('\n');
            informe.append("placeholder: ").append(seguro(() -> String.valueOf(combo.getAttribute("placeholder")))).append('\n');
            informe.append("texto:    ").append(seguro(() -> limpiar(combo.innerText()))).append('\n');
            try {
                combo.scrollIntoViewIfNeeded();
                combo.click();
                pagina.waitForTimeout(800);
                List<String> opciones = leerOpciones(pagina);
                informe.append("opciones (").append(opciones.size()).append("):\n");
                opciones.forEach(o -> informe.append("   - ").append(o).append('\n'));
                escribir("12-paso2-overlay-" + i + ".html", overlayHtml(pagina));
                captura(pagina, "12-paso2-combo-" + i);
                pagina.keyboard().press("Escape");
                pagina.waitForTimeout(400);
            } catch (RuntimeException e) {
                informe.append("NO SE PUDO ABRIR: ").append(e.getMessage()).append('\n');
            }
            informe.append('\n');
        }
        informe.append("# Radios, checkboxes, botones y pestanas\n");
        for (Locator el : pagina.locator(
                "input[type='radio'], input[type='checkbox'], button, [role='radio'], [role='tab']").all()) {
            informe.append("- ").append(seguro(() -> (String) el.evaluate("el => el.tagName.toLowerCase()")))
                    .append("  texto='").append(seguro(() -> limpiar(el.innerText())))
                    .append("'  selector: ").append(seguro(() -> selectorDe(el))).append('\n');
        }
        informe.append("\n# Encabezados de tabla\n");
        for (Locator th : pagina.locator("table th, table thead td, [role='columnheader']").all()) {
            informe.append("- ").append(seguro(() -> limpiar(th.innerText())))
                    .append("  selector: ").append(seguro(() -> selectorDe(th))).append('\n');
        }
        informe.append("\n# Filas de inventario (primeras 10)\n");
        for (Locator tr : pagina.locator(FILAS_INVENTARIO).all().stream().limit(10).toList()) {
            informe.append("- ").append(seguro(() -> limpiar(tr.innerText()))).append('\n');
        }
        escribir("12-paso2-controles.txt", informe.toString());
    }

    // ==================== UTILIDADES ====================

    private static Path perfilReconocimiento() {
        String appdata = System.getenv("APPDATA");
        Path base = (appdata != null && !appdata.isBlank())
                ? Path.of(appdata)
                : Path.of(System.getProperty("user.home"), "Library", "Application Support");
        return base.resolve("MonitorSIAE").resolve("perfiles").resolve("_reconocimiento");
    }

    private static void registrarIntento(int intento, String seccional, Page pagina) {
        MONITOREO.append("Consulta ").append(intento)
                .append("  seccional=").append(seccional)
                .append("  filas=").append(pagina.locator(FILAS_INVENTARIO).count())
                .append('\n');
        try {
            escribir("monitoreo.txt", MONITOREO.toString());
        } catch (IOException ignorado) {
            // best-effort
        }
    }

    private static void volcar(Page pagina, String nombre) throws IOException {
        escribir(nombre + ".html", pagina.content());
        escribir(nombre + "-elementos.txt", informeElementos(pagina, nombre));
        captura(pagina, nombre);
    }

    private static String informeElementos(Page pagina, String nombre) {
        List<Locator> elementos = pagina.locator(
                "input, textarea, select, button, [role], [formcontrolname], mat-select, "
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

    private static String overlayHtml(Page pagina) {
        return String.valueOf(pagina.evaluate("() => {"
                + " const c = document.querySelector('.cdk-overlay-container, .overlay-container');"
                + " return c ? c.outerHTML : ''; }"));
    }

    private static void registrarRed(Page pagina) {
        pagina.onResponse(respuesta -> {
            String tipo = respuesta.request().resourceType();
            if (!tipo.equals("xhr") && !tipo.equals("fetch")) {
                return;
            }
            RED.add(respuesta.request().method() + " " + respuesta.status() + " "
                    + respuesta.headerValue("content-type") + " " + respuesta.url());
            try {
                String contentType = respuesta.headerValue("content-type");
                if (contentType != null && contentType.contains("json") && cuerposGuardados < 40) {
                    String cuerpo = respuesta.text();
                    if (cuerpo.length() > 400_000) {
                        cuerpo = cuerpo.substring(0, 400_000);
                    }
                    Files.writeString(SALIDA.resolve(String.format("red-%02d.json", cuerposGuardados++)),
                            cuerpo, StandardCharsets.UTF_8);
                }
            } catch (RuntimeException | IOException ignorado) {
                // el cuerpo puede no estar disponible
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