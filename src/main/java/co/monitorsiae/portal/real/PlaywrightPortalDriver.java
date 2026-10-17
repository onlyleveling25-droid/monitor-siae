package co.monitorsiae.portal.real;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalInesperadoException;
import co.monitorsiae.portal.PortalException;
import co.monitorsiae.portal.ResultadoGuardar;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Path;
import java.util.List;

public final class PlaywrightPortalDriver implements PortalDriver {

    public static final String URL_TRAMITE = "https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite";

    private final Playwright playwright;
    private final BrowserContext contexto;
    private final Page pagina;

    public PlaywrightPortalDriver(Path perfil, NavegadorPreferido navegador) {
        Playwright runtime = Playwright.create();
        BrowserContext contextoCreado;
        Page paginaCreada;
        try {
            BrowserType.LaunchPersistentContextOptions opciones =
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel(canal(navegador))
                            .setHeadless(false);
            contextoCreado = runtime.chromium().launchPersistentContext(perfil, opciones);
            paginaCreada = contextoCreado.pages().isEmpty()
                    ? contextoCreado.newPage()
                    : contextoCreado.pages().getFirst();
            paginaCreada.navigate(URL_TRAMITE);
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
    }

    private static String canal(NavegadorPreferido navegador) {
        return switch (navegador) {
            case CHROME -> "chrome";
            case EDGE -> "msedge";
        };
    }

    @Override
    public List<String> abrirFormulario(ParametrosTramite tramite) {
        throw automatizacionPendiente();
    }

    @Override
    public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
        throw automatizacionPendiente();
    }

    @Override
    public List<ArmaInventario> leerInventario() {
        throw automatizacionPendiente();
    }

    @Override
    public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
        throw automatizacionPendiente();
    }

    private static PortalInesperadoException automatizacionPendiente() {
        return new PortalInesperadoException("La sesión real está abierta, pero la automatización del trámite "
                + "aún no está habilitada: faltan validar los selectores del portal.");
    }

    @Override
    public void close() {
        try {
            contexto.close();
        } finally {
            playwright.close();
        }
    }
}
