package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Texto;

import java.util.List;
import java.util.Optional;

/**
 * Elige, entre las seccionales abiertas, la primera que el cliente acepta.
 *
 * <p>La plataforma nombra las seccionales como "16 - Usaquén, Bogotá D.C.". El cliente puede
 * configurar el texto completo o solo una parte reconocible ("Bogotá"); se respeta el orden de
 * preferencia de la configuracion, no el de la lista.
 */
public final class SelectorSeccional {

    public Optional<String> elegir(List<String> abiertas, List<String> aceptadas) {
        for (String aceptada : aceptadas) {
            for (String abierta : abiertas) {
                if (Texto.equivalentes(abierta, aceptada) || Texto.contieneTermino(abierta, aceptada)) {
                    return Optional.of(abierta);
                }
            }
        }
        return Optional.empty();
    }
}
