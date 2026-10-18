package co.monitorsiae.portal.real;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 */
public final class Reconocimiento {

    private static final String URL = "https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite";
    private static final Path SALIDA = Path.of("reconocimiento");
    private static final BufferedReader TECLADO =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private static final List<String> RED = new ArrayList<>();
    private static int cuerposGuardados;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(SALIDA);
        String canal = args.length > 0 ? args[0] : "msedge";
        Path perfil = Path.of(System.getenv("APPDATA"), "MonitorSIAE", "perfiles", "_reconocimiento");
        System.out.println("Navegador: " + canal);
        System.out.println("Perfil:    " + perfil);
        System.out.println("Salida:    " + SALIDA.toAbsolutePath());

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
                volcar(pagina, "01-paso1-inicial");
                pausa("Revisa '01-paso1-inicial-elementos.txt'. Si algo quedo mal, corrigelo en el navegador.");

                recorrerCombos(pagina);
                volcar(pagina, "02-paso1-con-combos");
                pausa("Revisa 'combos.txt'. Si el script eligio mal alguna opcion, corrigelo a mano ahora.");

                probarGuardar(pagina);
                pausa("Ultimo paso hecho. Revisa la carpeta 'reconocimiento'.");
            } catch (RuntimeException e) {
                System.out.println("\nFALLO: " + e);
                e.printStackTrace();
                pausa("El navegador queda abierto. Pulsa ENTER para cerrar.");
            }
            escribir("red.txt", String.join(System.lineSeparator(), RED));
        }
    }

    private static void recorrerCombos(Page pagina) throws IOException {
        List<Locator> combos = pagina.locator("[role='combobox'], mat-select, select").all();
        StringBuilder informe = new StringBuilder("# Combos detectados: " + combos.size() + "\n\n");
        String[] objetivos = {"defensa", "adquisicion", "arma"};

        for (int i = 0; i < combos.size(); i++) {
            Locator combo = combos.get(i);
            informe.append("## Combo ").append(i).append('\n');
            informe.append("selector: ").append(seguro(() -> selectorDe(combo))).append('\n');
            informe.append("attrs:    ").append(seguro(() -> attrsDe(combo))).append('\n');
            informe.append("texto:    ").append(seguro(() -> limpiar(combo.innerText()))).append('\n');

            try {
                combo.scrollIntoViewIfNeeded();
                combo.click();
            } catch (RuntimeException e) {
                informe.append("NO SE PUDO ABRIR: ").append(e.getMessage()).append("\n\n");
                continue;
            }
            pagina.waitForTimeout(900);

            List<Locator> opciones = pagina.locator("[role='option'], mat-option").all();
            List<String> textos = new ArrayList<>();
            for (Locator opcion : opciones) {
                textos.add(seguro(() -> limpiar(opcion.innerText())));
            }
            informe.append("opciones (").append(textos.size()).append("):\n");
            textos.forEach(texto -> informe.append("   - ").append(texto).append('\n'));
            escribir("overlay-" + i + ".html", overlayHtml(pagina));

            int elegido = -1;
            String objetivo = i < objetivos.length ? objetivos[i] : null;
            if (objetivo != null) {
                for (int j = 0; j < textos.size(); j++) {
                    if (textos.get(j).toLowerCase(Locale.ROOT).contains(objetivo)) {
                        elegido = j;
                        break;
                    }
                }
            }
            if (elegido < 0 && !textos.isEmpty()) {
                elegido = 0;
            }
            if (elegido >= 0 && elegido < opciones.size()) {
                informe.append("ELEGIDO: ").append(textos.get(elegido)).append('\n');
                opciones.get(elegido).click();
                pagina.waitForTimeout(1300);
            } else {
                informe.append("Sin opciones; se cierra la lista.\n");
                pagina.keyboard().press("Escape");
            }
            captura(pagina, "combo-" + i);
            informe.append('\n');
        }
        escribir("combos.txt", informe.toString());
    }

    private static void probarGuardar(Page pagina) throws IOException {
        StringBuilder informe = new StringBuilder();
        Locator descripcion = buscarDescripcion(pagina);
        if (descripcion == null) {
            informe.append("No se encontro el campo de descripcion.\n");
        } else {
            informe.append("Descripcion: ").append(attrsDe(descripcion)).append('\n');
            descripcion.fill("Prueba de reconocimiento de Monitor SIAE para validar el formulario del "
                    + "paso uno. ".repeat(3));
            pagina.waitForTimeout(400);
        }
        Locator guardar = pagina.locator("button:has-text('GUARDAR'), button:has-text('Guardar')").first();
        informe.append("GUARDAR visible=").append(guardar.isVisible())
                .append(" habilitado=").append(guardar.isEnabled()).append('\n');
        informe.append("GUARDAR attrs: ").append(attrsDe(guardar)).append('\n');
        volcar(pagina, "03-paso1-antes-de-guardar");

        String respuesta = pedir("Escribe 's' y ENTER para pulsar GUARDAR (crea un borrador), "
                + "o ENTER solo para no pulsarlo: ");
        if (!respuesta.trim().equalsIgnoreCase("s")) {
            escribir("04-guardar.txt", informe.append("No se pulso GUARDAR.\n").toString());
            return;
        }

        guardar.click();
        pagina.waitForTimeout(4000);
        informe.append("URL despues: ").append(pagina.url()).append('\n');
        escribir("04-guardar.txt", informe.toString());
        escribir("05-tras-guardar.html", pagina.content());
        escribir("05-tras-guardar-elementos.txt", informeElementos(pagina, "tras guardar"));
        captura(pagina, "05-tras-guardar");
        escribir("red.txt", String.join(System.lineSeparator(), RED));
    }

    private static Locator buscarDescripcion(Page pagina) {
        for (String candidato : new String[]{"textarea[formcontrolname]", "textarea",
                "[maxlength='500']", "input[maxlength='500']"}) {
            Locator locator = pagina.locator(candidato);
            if (locator.count() > 0) {
                return locator.first();
            }
        }
        return null;
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
                // El elemento desaparecio del DOM entre la lista y la lectura.
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
                if (contentType != null && contentType.contains("json") && cuerposGuardados < 15) {
                    String cuerpo = respuesta.text();
                    if (cuerpo.length() > 400_000) {
                        cuerpo = cuerpo.substring(0, 400_000);
                    }
                    Files.writeString(SALIDA.resolve(String.format("red-%02d.json", cuerposGuardados++)),
                            cuerpo, StandardCharsets.UTF_8);
                }
            } catch (RuntimeException | IOException ignorado) {
                // El cuerpo puede no estar disponible si la pagina ya navego.
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

    private static String seguro(Proveedor proveedor) {
        try {
            return proveedor.obtener();
        } catch (RuntimeException e) {
            return "<error: " + e.getMessage() + ">";
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