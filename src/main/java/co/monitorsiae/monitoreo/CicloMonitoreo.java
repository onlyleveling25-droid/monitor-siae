package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ModoReaccion;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.portal.OpcionNoDisponibleException;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalException;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.SesionVencidaException;

import java.util.List;
import java.util.Optional;

/**
 * Una vuelta completa para un cliente: paso 1, lectura del inventario y, si corresponde, apartar.
 * No espera ni repite; eso lo decide {@link MonitorCliente}.
 */
public final class CicloMonitoreo {

    private final SelectorSeccional selectorSeccional;
    private final SelectorArma selectorArma;

    public CicloMonitoreo(SelectorSeccional selectorSeccional, SelectorArma selectorArma) {
        this.selectorSeccional = selectorSeccional;
        this.selectorArma = selectorArma;
    }

    public ResultadoCiclo ejecutar(Cliente cliente, PortalDriver portal) {
        try {
            return recorrer(cliente, portal);
        } catch (OpcionNoDisponibleException e) {
            return new ResultadoCiclo.TramiteNoDisponible(e.getMessage());
        } catch (SesionVencidaException e) {
            return new ResultadoCiclo.SesionVencida(e.getMessage());
        } catch (PortalException e) {
            return new ResultadoCiclo.Inesperado(e.getMessage());
        }
    }
    

    private ResultadoCiclo recorrer(Cliente cliente, PortalDriver portal) {
        ParametrosTramite tramite = cliente.tramite();

        List<String> abiertas = portal.abrirFormulario(tramite);
        if (abiertas.isEmpty()) {
            return new ResultadoCiclo.SeccionalesCerradas();
        }

        Optional<String> seccional = selectorSeccional.elegir(abiertas, tramite.seccionales());
        if (seccional.isEmpty()) {
            return new ResultadoCiclo.SinSeccionalObjetivo(abiertas);
        }

        if (portal.enviarPaso1(seccional.get(), tramite.descripcion()) == ResultadoGuardar.SIN_INVENTARIO) {
            return new ResultadoCiclo.SinInventario(seccional.get());
        }

        List<ArmaInventario> inventario = portal.leerInventario();
        Optional<EleccionArma> eleccion = selectorArma.elegir(inventario, cliente.seleccion());
        if (eleccion.isEmpty()) {
            return new ResultadoCiclo.SinArmaDeInteres(seccional.get(), inventario);
        }

        if (cliente.modo() == ModoReaccion.ALERTA) {
            return new ResultadoCiclo.ArmaEncontrada(seccional.get(), eleccion.get(), inventario);
        }
        return apartar(portal, seccional.get(), eleccion.get(), cliente.seleccion(), inventario);
    }

    private static ResultadoCiclo apartar(
            PortalDriver portal, String seccional, EleccionArma eleccion, ReglasSeleccion reglas,
            List<ArmaInventario> inventario) {
        for (int intento = 1; intento <= reglas.reintentos(); intento++) {
            if (portal.apartar(eleccion.arma(), reglas)) {
                boolean sellado = portal.confirmarCierre();
                return new ResultadoCiclo.ArmaApartada(seccional, eleccion, intento, inventario, sellado);
            }
        }
        return new ResultadoCiclo.ApartadoFallido(seccional, eleccion, reglas.reintentos(), inventario);
    }
}
