package co.monitorsiae.portal.simulado;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.portal.OpcionNoDisponibleException;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.PortalInesperadoException;
import co.monitorsiae.portal.PortalNoRespondeException;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.SesionVencidaException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Plataforma falsa que recorre una secuencia de escenas, una por ciclo. Cuando se acaban, repite
 * la ultima. Registra lo que el monitor hizo para poder comprobarlo.
 *
 * <p>Imita las reglas conocidas del portal: GUARDAR no funciona con una descripcion fuera de 150 a
 * 500 caracteres, y no se puede elegir una seccional que no este en la lista.
 */
public final class PortalSimulado implements PortalDriver {

    private final Deque<Escena> pendientes;
    private Escena actual;

    private int formulariosAbiertos;
    private final List<String> seccionalesEnviadas = new ArrayList<>();
    private final List<ArmaInventario> intentosDeApartar = new ArrayList<>();

    public PortalSimulado(List<Escena> escenas) {
        if (escenas.isEmpty()) {
            throw new IllegalArgumentException("Se necesita al menos una escena.");
        }
        this.pendientes = new ArrayDeque<>(escenas);
        this.actual = pendientes.peekFirst();
    }

    public static PortalSimulado con(Escena... escenas) {
        return new PortalSimulado(List.of(escenas));
    }

    @Override
    public List<String> abrirFormulario(ParametrosTramite tramite) {
        actual = pendientes.size() > 1 ? pendientes.pollFirst() : pendientes.peekFirst();
        formulariosAbiertos++;
        lanzarFallaSiCorresponde();
        if (!actual.tramiteDisponible()) {
            throw new OpcionNoDisponibleException("No apareció \"" + tramite.tipoTramite() + "\"");
        }
        return actual.seccionalesAbiertas();
    }

    @Override
    public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
        if (!actual.seccionalesAbiertas().contains(seccional)) {
            throw new PortalInesperadoException("La seccional \"" + seccional + "\" no está en la lista.");
        }
        if (descripcion.length() < ParametrosTramite.DESCRIPCION_MINIMA
                || descripcion.length() > ParametrosTramite.DESCRIPCION_MAXIMA) {
            throw new PortalInesperadoException("GUARDAR está deshabilitado: la descripción no cumple el largo.");
        }
        seccionalesEnviadas.add(seccional);
        return actual.conInventario().contains(seccional) ? ResultadoGuardar.AVANZO_PASO_2 : ResultadoGuardar.SIN_INVENTARIO;
    }

    @Override
    public List<ArmaInventario> leerInventario() {
        return actual.inventario();
    }

    @Override
    public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
        intentosDeApartar.add(arma);
        return actual.apartadoExitoso();
    }

    private void lanzarFallaSiCorresponde() {
        switch (actual.falla()) {
            case SESION_VENCIDA -> throw new SesionVencidaException("La plataforma pidió iniciar sesión.");
            case ERROR_SERVIDOR -> throw new PortalInesperadoException("La plataforma respondió con error 503.");
            case PORTAL_NO_RESPONDE -> throw new PortalNoRespondeException(
                    "El servidor no contestó a GUARDAR (error 504).");
            case NINGUNA -> {
                // respuesta normal
            }
        }
    }

    public int formulariosAbiertos() {
        return formulariosAbiertos;
    }

    public List<String> seccionalesEnviadas() {
        return Collections.unmodifiableList(seccionalesEnviadas);
    }

    public List<ArmaInventario> intentosDeApartar() {
        return Collections.unmodifiableList(intentosDeApartar);
    }
}
