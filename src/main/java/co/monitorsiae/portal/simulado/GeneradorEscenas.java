package co.monitorsiae.portal.simulado;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.EscenarioSimulador;
import co.monitorsiae.dominio.Texto;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma la secuencia de escenas (una por consulta) de cada escenario del simulador para un cliente. Cuando la
 * secuencia termina se repite su última escena.
 */
public final class GeneradorEscenas {

    private static final String SIN_SECCIONAL = "Sin seccional configurada";

    private GeneradorEscenas() {
    }

    public static List<Escena> para(EscenarioSimulador escenario, Cliente cliente) {
        List<String> configuradas = cliente.tramite().seccionales();
        String[] abiertas = configuradas.isEmpty() ? new String[] {SIN_SECCIONAL} : configuradas.toArray(String[]::new);
        String primera = abiertas[0];
        String[] catalogo = CatalogoArmas.armas().toArray(String[]::new);

        List<Escena> escenas = new ArrayList<>();
        switch (escenario) {
            case HALLAZGO_INMEDIATO -> escenas.add(Escena.conInventario(primera, catalogo));
            case SIN_INVENTARIO_Y_LUEGO_HAY -> {
                escenas.add(Escena.seccionalesCerradas());
                escenas.add(Escena.seccionalesCerradas());
                escenas.add(Escena.abiertasSinInventario(abiertas));
                escenas.add(Escena.abiertasSinInventario(abiertas));
                escenas.add(Escena.conInventario(primera, catalogo));
            }
            case INVENTARIO_SIN_OBJETIVOS -> {
                escenas.add(Escena.abiertasSinInventario(abiertas));
                escenas.add(Escena.conInventario(primera, sinObjetivos(cliente)));
            }
            case APARTADO_FALLIDO -> {
                escenas.add(Escena.abiertasSinInventario(abiertas));
                escenas.add(Escena.conInventario(primera, catalogo).conApartadoFallido());
            }
            case TRES_FALLOS_SEGUIDOS -> {
                escenas.add(Escena.seccionalesCerradas());
                escenas.add(Escena.abiertasSinInventario(abiertas));
                escenas.add(Escena.conFalla(Escena.Falla.ERROR_SERVIDOR));
            }
            case SESION_VENCIDA -> {
                escenas.add(Escena.seccionalesCerradas());
                escenas.add(Escena.abiertasSinInventario(abiertas));
                escenas.add(Escena.conFalla(Escena.Falla.SESION_VENCIDA));
            }
            case SECCIONALES_CERRADAS -> escenas.add(Escena.seccionalesCerradas());
        }
        return escenas;
    }

    /** Armas del catálogo que no coinciden con ninguno de los objetivos del cliente. */
    private static String[] sinObjetivos(Cliente cliente) {
        List<CriterioArma> objetivos = cliente.seleccion().objetivos();
        List<String> restantes = CatalogoArmas.armas().stream()
                .filter(arma -> objetivos.stream()
                        .noneMatch(objetivo -> Texto.contieneTodos(arma, objetivo.palabrasClave())))
                .toList();
        return (restantes.isEmpty() ? CatalogoArmas.armas() : restantes).toArray(String[]::new);
    }
}
