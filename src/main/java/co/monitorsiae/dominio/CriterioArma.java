package co.monitorsiae.dominio;

import java.util.List;

/**
 * Describe un arma buscada mediante palabras clave que deben aparecer todas en su nombre.
 *
 * <p>Se usan palabras clave en lugar del nombre completo porque la plataforma escribe el mismo
 * modelo de formas distintas y con errores de digitacion.
 */
public record CriterioArma(List<String> palabrasClave) {

    public CriterioArma {
        palabrasClave = Listas.textos(palabrasClave);
    }

    public static CriterioArma de(String... palabrasClave) {
        return new CriterioArma(List.of(palabrasClave));
    }
}
