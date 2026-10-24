package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Texto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Elige, entre las seccionales abiertas, las que el cliente acepta.
 *
 * <p>La plataforma nombra las seccionales como "16 - Usaquén, Bogotá D.C.". El cliente puede
 * configurar el texto completo o solo una parte reconocible ("Bogotá"); se respeta el orden de
 * preferencia de la configuracion, no el de la lista.
 */
public final class SelectorSeccional {

    /** La primera seccional abierta que el cliente acepta, respetando su orden de preferencia. */
    public Optional<String> elegir(List<String> abiertas, List<String> aceptadas) {
        return elegirTodas(abiertas, aceptadas).stream().findFirst();
    }

    /**
     * Todas las seccionales abiertas que el cliente acepta, en su orden de preferencia, sin
     * repetidos. Permite que el llamador intente con la siguiente si la primera no tiene
     * inventario en este ciclo.
     */
    public List<String> elegirTodas(List<String> abiertas, List<String> aceptadas) {
        List<String> resultado = new ArrayList<>();
        for (String aceptada : aceptadas) {
            for (String abierta : abiertas) {
                if ((Texto.equivalentes(abierta, aceptada) || Texto.contieneTermino(abierta, aceptada))
                        && !resultado.contains(abierta)) {
                    resultado.add(abierta);
                }
            }
        }
        return resultado;
    }
}
