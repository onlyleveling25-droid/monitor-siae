package co.monitorsiae.dominio;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comparacion de textos tolerante a como escribe la plataforma: sin tildes, sin mayusculas y sin
 * depender de guiones, puntos o espacios entre letras y numeros.
 *
 * <p>Asi, "G-43", "G43" y "g 43" se consideran iguales, y "Medellín" coincide con "MEDELLIN".
 */
public final class Texto {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LETRA_NUMERO = Pattern.compile("(?<=[a-z])(?=[0-9])|(?<=[0-9])(?=[a-z])");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private Texto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        String soloAlfanumerico = NO_ALFANUMERICO.matcher(sinTildes.toLowerCase(Locale.ROOT)).replaceAll(" ");
        String separado = LETRA_NUMERO.matcher(soloAlfanumerico).replaceAll(" ");
        return ESPACIOS.matcher(separado).replaceAll(" ").strip();
    }

    public static boolean equivalentes(String a, String b) {
        return normalizar(a).equals(normalizar(b));
    }

    /**
     * Indica si el termino aparece en el texto como palabras completas. "22" no coincide dentro de
     * "1922", pero "g43" si coincide dentro de "pistola glock mod g-43 rail". Un termino de varias
     * palabras debe aparecer seguido: para exigir palabras sueltas use {@link #contieneTodos}.
     */
    public static boolean contieneTermino(String texto, String termino) {
        String terminoNormalizado = normalizar(termino);
        if (terminoNormalizado.isEmpty()) {
            return false;
        }
        return (" " + normalizar(texto) + " ").contains(" " + terminoNormalizado + " ");
    }

    public static boolean contieneTodos(String texto, List<String> terminos) {
        return !terminos.isEmpty() && terminos.stream().allMatch(termino -> contieneTermino(texto, termino));
    }

    public static boolean contieneAlguno(String texto, List<String> terminos) {
        return terminos.stream().anyMatch(termino -> contieneTermino(texto, termino));
    }
}
