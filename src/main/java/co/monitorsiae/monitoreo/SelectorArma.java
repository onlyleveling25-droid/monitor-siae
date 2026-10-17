package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.dominio.Texto;

import java.util.List;
import java.util.Optional;

/**
 * Decide que arma del inventario tomar segun las reglas de un cliente.
 *
 * <p>Orden: primero se descartan las armas excluidas; luego se recorren los objetivos por
 * prioridad y gana el primero que tenga coincidencia. Solo si ningun objetivo esta disponible se
 * aplica la politica de reemplazo.
 */
public final class SelectorArma {

    public Optional<EleccionArma> elegir(List<ArmaInventario> inventario, ReglasSeleccion reglas) {
        List<ArmaInventario> candidatas = inventario.stream()
                .filter(arma -> !Texto.contieneAlguno(arma.descripcion(), reglas.excluir()))
                .toList();

        Optional<EleccionArma> objetivo = buscarPorPrioridad(candidatas, reglas.objetivos(), MotivoEleccion.OBJETIVO);
        if (objetivo.isPresent()) {
            return objetivo;
        }

        return switch (reglas.siNoHayObjetivo()) {
            case SEGUIR_ESPERANDO -> Optional.empty();
            case USAR_ALTERNATIVAS ->
                    buscarPorPrioridad(candidatas, reglas.alternativas(), MotivoEleccion.ALTERNATIVA);
            case CUALQUIERA_DEL_TIPO -> buscarPorTipo(candidatas, reglas.tipoComodin());
        };
    }

    private static Optional<EleccionArma> buscarPorPrioridad(
            List<ArmaInventario> candidatas, List<CriterioArma> criterios, MotivoEleccion motivo) {
        for (int prioridad = 0; prioridad < criterios.size(); prioridad++) {
            List<String> palabras = criterios.get(prioridad).palabrasClave();
            for (ArmaInventario arma : candidatas) {
                if (Texto.contieneTodos(arma.descripcion(), palabras)) {
                    return Optional.of(new EleccionArma(arma, motivo, prioridad));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<EleccionArma> buscarPorTipo(List<ArmaInventario> candidatas, String tipo) {
        if (tipo.isEmpty()) {
            return Optional.empty();
        }
        return candidatas.stream()
                .filter(arma -> Texto.equivalentes(arma.tipoEfectivo(), tipo))
                .findFirst()
                .map(arma -> new EleccionArma(arma, MotivoEleccion.CUALQUIERA_DEL_TIPO, 0));
    }
}
