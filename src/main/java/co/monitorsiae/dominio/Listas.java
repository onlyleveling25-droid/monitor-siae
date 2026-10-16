package co.monitorsiae.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Utilidades para construir listas inmutables a partir de datos que pueden venir incompletos
 * desde el archivo de configuracion.
 */
final class Listas {

    private Listas() {
    }

    /** Copia inmutable sin elementos nulos; una lista nula se convierte en vacia. */
    static <T> List<T> inmutable(List<T> origen) {
        if (origen == null) {
            return List.of();
        }
        return origen.stream().filter(Objects::nonNull).toList();
    }

    /** Copia inmutable de textos sin espacios sobrantes, sin vacios ni nulos. */
    static List<String> textos(List<String> origen) {
        if (origen == null) {
            return List.of();
        }
        return origen.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(texto -> !texto.isEmpty())
                .toList();
    }

    static String texto(String valor) {
        return valor == null ? "" : valor.strip();
    }
}
