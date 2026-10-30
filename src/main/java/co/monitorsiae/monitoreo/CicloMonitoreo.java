package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.AlcancePaso2;
import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ModoReaccion;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.portal.OpcionNoDisponibleException;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalException;
import co.monitorsiae.portal.PortalNoRespondeException;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.ResultadoSolicitud;
import co.monitorsiae.portal.SesionVencidaException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Una vuelta completa para un cliente: paso 1, lectura del inventario y, si corresponde, apartar.
 * No espera ni repite; eso lo decide {@link MonitorCliente}.
 */
public final class CicloMonitoreo {

    /** Como se nombra la fuente del inventario en los resultados cuando viene de la solicitud abierta. */
    static final String ETIQUETA_SOLICITUD = "Estado de trámite";

    private final SelectorSeccional selectorSeccional;
    private final SelectorArma selectorArma;

    public CicloMonitoreo(SelectorSeccional selectorSeccional, SelectorArma selectorArma) {
        this.selectorSeccional = selectorSeccional;
        this.selectorArma = selectorArma;
    }

    /** Una vuelta sin memoria entre llamadas: siempre empieza por el paso 1. */
    public ResultadoCiclo ejecutar(Cliente cliente, PortalDriver portal) {
        return ejecutar(cliente, portal, new SeguimientoSolicitud());
    }

    /**
     * Una vuelta que respeta el seguimiento: si hay una solicitud abierta se busca inventario desde
     * "Estado de tramite"; si no, desde el paso 1.
     */
    public ResultadoCiclo ejecutar(Cliente cliente, PortalDriver portal, SeguimientoSolicitud seguimiento) {
        try {
            return recorrer(cliente, portal, seguimiento);
        } catch (OpcionNoDisponibleException e) {
            return new ResultadoCiclo.TramiteNoDisponible(e.getMessage());
        } catch (SesionVencidaException e) {
            return new ResultadoCiclo.SesionVencida(e.getMessage());
        } catch (PortalNoRespondeException e) {
            return new ResultadoCiclo.PortalNoResponde(e.getMessage());
        } catch (PortalException e) {
            return new ResultadoCiclo.Inesperado(e.getMessage());
        }
    }

    private ResultadoCiclo recorrer(Cliente cliente, PortalDriver portal, SeguimientoSolicitud seguimiento) {
        if (seguimiento.siguiendoSolicitud()) {
            return recorrerSolicitud(cliente, portal, seguimiento);
        }

        ParametrosTramite tramite = cliente.tramite();

        List<String> abiertas = portal.abrirFormulario(tramite);
        if (abiertas.isEmpty()) {
            return new ResultadoCiclo.SeccionalesCerradas();
        }

        List<String> candidatas = selectorSeccional.elegirTodas(abiertas, tramite.seccionales());
        if (candidatas.isEmpty()) {
            return new ResultadoCiclo.SinSeccionalObjetivo(abiertas);
        }

        // Prueba cada seccional aceptada en orden de preferencia: si una no tiene inventario,
        // sigue con la siguiente antes de darse por vencido en este ciclo.
        List<String> probadasSinInventario = new ArrayList<>();
        for (String seccional : candidatas) {
            if (portal.enviarPaso1(seccional, tramite.descripcion()) == ResultadoGuardar.SIN_INVENTARIO) {
                probadasSinInventario.add(seccional);
                continue;
            }
            // Con inventario, GUARDAR ya dejo la solicitud abierta en el portal.
            return resolverInventario(cliente, portal, seccional, portal.leerInventario(), seguimiento);
        }
        return new ResultadoCiclo.SinInventario(String.join(" | ", probadasSinInventario));
    }

    /**
     * Busca inventario desde la solicitud abierta. Si no hay ninguna NO se improvisa el paso 1: el
     * monitor se detiene y la persona decide si revisar el portal o iniciar desde el paso 1.
     */
    private ResultadoCiclo recorrerSolicitud(Cliente cliente, PortalDriver portal, SeguimientoSolicitud seguimiento) {
        String numero = seguimiento.numeroSolicitud();
        ResultadoSolicitud encontrada = portal.abrirSolicitudExistente(cliente.tramite(), numero);
        if (encontrada != ResultadoSolicitud.SIN_SOLICITUD) {
            seguimiento.solicitudEncontrada();
            // Sin número se tomó la primera que coincidía: desde ahora se vigila siempre esa.
            seguimiento.fijarNumeroSiFalta(portal.numeroSolicitudAbierta());
        }
        return switch (encontrada) {
            case SIN_SOLICITUD -> {
                ResultadoCiclo.SolicitudNoEncontrada noEncontrada = solicitudNoEncontrada(cliente, seguimiento);
                seguimiento.volverAlPaso1();
                yield noEncontrada;
            }
            case SIN_INVENTARIO -> new ResultadoCiclo.SinInventarioEnSolicitud(seguimiento.numeroSolicitud());
            case CON_INVENTARIO ->
                    resolverInventario(cliente, portal, ETIQUETA_SOLICITUD, portal.leerInventario(), seguimiento);
        };
    }

    /**
     * Explica por qué se detuvo y qué hacer, según cómo se llegó aquí: la solicitud la acababa de
     * crear el paso 1, la persona dio un número o pidió la primera en progreso.
     */
    static ResultadoCiclo.SolicitudNoEncontrada solicitudNoEncontrada(Cliente cliente,
                                                                       SeguimientoSolicitud seguimiento) {
        String nombre = cliente.nombre();
        String tramite = "«" + cliente.tramite().obtenerPermiso() + " · " + cliente.tramite().tipoTramite() + "»";
        String numero = seguimiento.numeroSolicitud();
        String reanudar = "Si no está: deja «Nueva solicitud (paso 1)» y pulsa «Iniciar / reanudar». El programa "
                + "volverá a buscar inventario desde el paso 1, como al principio.";

        if (seguimiento.recienCreada()) {
            String donde = seguimiento.seccionalOrigen().isBlank() ? "" : " en " + seguimiento.seccionalOrigen();
            return new ResultadoCiclo.SolicitudNoEncontrada(
                    "Monitoreo detenido: el paso 1 encontró inventario" + donde + " sin armas de interés, pero la "
                            + "solicitud no apareció en Estado de trámite.",
                    "Por qué importa: cuando el paso 1 encuentra inventario pero ninguna arma sirve, el portal deja "
                            + "una solicitud EN PROGRESO. El programa la vigila desde «Estado de trámite» para no "
                            + "crear solicitudes repetidas. Esta vez no apareció, ni esperando ni recargando la "
                            + "página, así que el programa se detuvo para que decidas.\n\n"
                            + "Qué hacer:\n"
                            + "1. En la ventana del navegador de " + nombre + ", abre «Estado de trámite» y busca una "
                            + "tarjeta EN PROGRESO de " + tramite + ".\n"
                            + "2. Si la ves: en el Panel, en la fila de " + nombre + ", elige «Solicitud existente», "
                            + "escribe su número (por ejemplo 1-01-45-202610-4336) y pulsa «Iniciar / reanudar». El "
                            + "programa la vigilará desde ahí.\n"
                            + "3. " + reanudar);
        }
        if (!numero.isBlank()) {
            return new ResultadoCiclo.SolicitudNoEncontrada(
                    "Monitoreo detenido: la solicitud " + numero + " no está EN PROGRESO en Estado de trámite.",
                    "Puede que el número esté mal escrito o que la solicitud ya se haya cerrado o cancelado.\n\n"
                            + "Qué hacer:\n"
                            + "1. En la ventana del navegador de " + nombre + ", abre «Estado de trámite» y mira "
                            + "las tarjetas EN PROGRESO de " + tramite + ".\n"
                            + "2. Si hay una con otro número: escríbelo en el Panel, junto a «Solicitud existente», "
                            + "y pulsa «Iniciar / reanudar».\n"
                            + "3. " + reanudar);
        }
        return new ResultadoCiclo.SolicitudNoEncontrada(
                "Monitoreo detenido: no hay ninguna solicitud EN PROGRESO de " + tramite + " en Estado de trámite.",
                "Qué hacer:\n"
                        + "1. En la ventana del navegador de " + nombre + ", abre «Estado de trámite» y busca una "
                        + "tarjeta EN PROGRESO de ese trámite.\n"
                        + "2. Si la ves (quizá con otro permiso o tipo): escribe su número en el Panel, junto a "
                        + "«Solicitud existente», y pulsa «Iniciar / reanudar».\n"
                        + "3. " + reanudar);
    }

    private ResultadoCiclo resolverInventario(Cliente cliente, PortalDriver portal, String seccional,
                                              List<ArmaInventario> inventario, SeguimientoSolicitud seguimiento) {
        Optional<EleccionArma> eleccion = selectorArma.elegir(inventario, cliente.seleccion());
        if (eleccion.isEmpty()) {
            // La solicitud sigue abierta en el portal: repetir el paso 1 no sirve, se vigila desde alli.
            seguimiento.seguirSolicitud(seccional);
            return new ResultadoCiclo.SinArmaDeInteres(seccional, inventario);
        }

        if (cliente.modo() == ModoReaccion.ALERTA) {
            return new ResultadoCiclo.ArmaEncontrada(seccional, eleccion.get(), inventario);
        }
        ResultadoCiclo resultado = apartar(portal, seccional, eleccion.get(), cliente.seleccion(), inventario,
                cliente.alcancePaso2());
        if (resultado instanceof ResultadoCiclo.ApartadoFallido) {
            seguimiento.seguirSolicitud(seccional);   // alguien se llevo el arma; la solicitud sigue abierta
        }
        return resultado;
    }

    /**
     * Intenta apartar hasta el número de reintentos del cliente. Entre un intento y otro se vuelve a
     * leer el listado y a elegir: si el arma desapareció se pasa a la siguiente que cumpla (otro
     * objetivo, una alternativa o el comodín); si sigue ahí se reintenta con ella.
     */
    private ResultadoCiclo apartar(
            PortalDriver portal, String seccional, EleccionArma primera, ReglasSeleccion reglas,
            List<ArmaInventario> inventario, AlcancePaso2 alcancePaso2) {
        EleccionArma eleccion = primera;
        List<ArmaInventario> listado = inventario;
        for (int intento = 1; intento <= reglas.reintentos(); intento++) {
            if (portal.apartar(eleccion.arma(), reglas)) {
                // Desde Estado de trámite el único cuadro es «Ver armas», que ya sella el trámite.
                boolean sellado = portal.cierreConfirmadoAlApartar()
                        || (alcancePaso2 == AlcancePaso2.SELLAR_TRAMITE_COMPLETO && portal.confirmarCierre());
                return new ResultadoCiclo.ArmaApartada(seccional, eleccion, intento, listado, sellado);
            }
            if (intento == reglas.reintentos()) {
                break;
            }
            try {
                listado = portal.leerInventario();
            } catch (SesionVencidaException e) {
                throw e;
            } catch (PortalException e) {
                // No se pudo repasar el listado: se avisa del apartado fallido, que es lo importante.
                return new ResultadoCiclo.ApartadoFallido(seccional, eleccion, intento, listado);
            }
            Optional<EleccionArma> siguiente = selectorArma.elegir(listado, reglas);
            if (siguiente.isEmpty()) {
                // Ya no queda ninguna que cumpla: alguien se llevó el arma.
                return new ResultadoCiclo.ApartadoFallido(seccional, eleccion, intento, listado);
            }
            eleccion = siguiente.get();
        }
        return new ResultadoCiclo.ApartadoFallido(seccional, eleccion, reglas.reintentos(), listado);
    }
}
