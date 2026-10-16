package co.monitorsiae.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Reglas para decidir que arma del inventario interesa a un cliente.
 *
 * @param modalidad       boton que se pulsa al apartar el arma
 * @param objetivos       armas buscadas, en orden de prioridad
 * @param siNoHayObjetivo comportamiento cuando ningun objetivo esta disponible
 * @param alternativas    armas aceptables si la politica es {@link PoliticaSinObjetivo#USAR_ALTERNATIVAS}
 * @param tipoComodin     tipo de arma aceptable si la politica es
 *                        {@link PoliticaSinObjetivo#CUALQUIERA_DEL_TIPO}
 * @param excluir         palabras que descartan un arma aunque coincida con un criterio
 * @param reintentos      intentos de apartar un arma antes de rendirse
 */
public record ReglasSeleccion(
        Modalidad modalidad,
        List<CriterioArma> objetivos,
        PoliticaSinObjetivo siNoHayObjetivo,
        List<CriterioArma> alternativas,
        String tipoComodin,
        List<String> excluir,
        int reintentos) {

    public static final int REINTENTOS_POR_DEFECTO = 2;

    public ReglasSeleccion {
        modalidad = Objects.requireNonNullElse(modalidad, Modalidad.PORTE);
        objetivos = Listas.inmutable(objetivos);
        siNoHayObjetivo = Objects.requireNonNullElse(siNoHayObjetivo, PoliticaSinObjetivo.SEGUIR_ESPERANDO);
        alternativas = Listas.inmutable(alternativas);
        tipoComodin = Listas.texto(tipoComodin);
        excluir = Listas.textos(excluir);
        reintentos = reintentos <= 0 ? REINTENTOS_POR_DEFECTO : reintentos;
    }

    public static ReglasSeleccion porDefecto() {
        return new ReglasSeleccion(Modalidad.PORTE, List.of(), PoliticaSinObjetivo.SEGUIR_ESPERANDO,
                List.of(), "Pistola", List.of(), REINTENTOS_POR_DEFECTO);
    }
}
