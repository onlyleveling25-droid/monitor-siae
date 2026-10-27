package co.monitorsiae.portal.simulado;

import co.monitorsiae.dominio.ArmaInventario;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Estado de la plataforma simulada durante un ciclo: que opciones aparecen, que seccionales estan
 * abiertas, cuales tienen inventario y como responde al apartar.
 *
 * @param tramiteDisponible    si aparecen las opciones de permiso y tramite
 * @param seccionalesAbiertas  opciones de la lista de seccionales
 * @param conInventario        seccionales en las que GUARDAR pasa al paso 2
 * @param inventario           listado de armas del paso 2
 * @param apartadoExitoso      si apartar un arma se confirma
 * @param falla                falla que se produce en lugar de la respuesta normal
 */
public record Escena(
        boolean tramiteDisponible,
        List<String> seccionalesAbiertas,
        Set<String> conInventario,
        List<ArmaInventario> inventario,
        boolean apartadoExitoso,
        Falla falla) {

    public enum Falla {
        NINGUNA,
        SESION_VENCIDA,
        ERROR_SERVIDOR,
        PORTAL_NO_RESPONDE
    }

    public Escena {
        seccionalesAbiertas = List.copyOf(seccionalesAbiertas);
        conInventario = Set.copyOf(conInventario);
        inventario = List.copyOf(inventario);
    }

    public static Escena seccionalesCerradas() {
        return new Escena(true, List.of(), Set.of(), List.of(), true, Falla.NINGUNA);
    }

    public static Escena tramiteNoDisponible() {
        return new Escena(false, List.of(), Set.of(), List.of(), true, Falla.NINGUNA);
    }

    public static Escena abiertasSinInventario(String... seccionales) {
        return new Escena(true, List.of(seccionales), Set.of(), List.of(), true, Falla.NINGUNA);
    }

    /** Una seccional abierta con inventario; las armas toman su posicion del orden recibido. */
    public static Escena conInventario(String seccional, String... armas) {
        List<ArmaInventario> filas = new ArrayList<>();
        for (int i = 0; i < armas.length; i++) {
            filas.add(new ArmaInventario(i, armas[i], ""));
        }
        return new Escena(true, List.of(seccional), Set.of(seccional), filas, true, Falla.NINGUNA);
    }

    public static Escena conFalla(Falla falla) {
        return new Escena(true, List.of(), Set.of(), List.of(), true, falla);
    }

    public Escena conApartadoFallido() {
        return new Escena(tramiteDisponible, seccionalesAbiertas, conInventario, inventario, false, falla);
    }
}
