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
import co.monitorsiae.portal.SesionVencidaException;

import java.util.ArrayList;
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
        } catch (PortalNoRespondeException e) {
            return new ResultadoCiclo.PortalNoResponde(e.getMessage());
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

            List<ArmaInventario> inventario = portal.leerInventario();
            Optional<EleccionArma> eleccion = selectorArma.elegir(inventario, cliente.seleccion());
            if (eleccion.isEmpty()) {
                return new ResultadoCiclo.SinArmaDeInteres(seccional, inventario);
            }

            if (cliente.modo() == ModoReaccion.ALERTA) {
                return new ResultadoCiclo.ArmaEncontrada(seccional, eleccion.get(), inventario);
            }
            return apartar(portal, seccional, eleccion.get(), cliente.seleccion(), inventario,
                    cliente.alcancePaso2());
        }
        return new ResultadoCiclo.SinInventario(String.join(" | ", probadasSinInventario));
    }

    private static ResultadoCiclo apartar(
            PortalDriver portal, String seccional, EleccionArma eleccion, ReglasSeleccion reglas,
            List<ArmaInventario> inventario, AlcancePaso2 alcancePaso2) {
        for (int intento = 1; intento <= reglas.reintentos(); intento++) {
            if (portal.apartar(eleccion.arma(), reglas)) {
                boolean sellado = alcancePaso2 == AlcancePaso2.SELLAR_TRAMITE_COMPLETO && portal.confirmarCierre();
                return new ResultadoCiclo.ArmaApartada(seccional, eleccion, intento, inventario, sellado);
            }
        }
        return new ResultadoCiclo.ApartadoFallido(seccional, eleccion, reglas.reintentos(), inventario);
    }
}
