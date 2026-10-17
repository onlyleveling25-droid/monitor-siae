package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.CriterioArma;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los inventarios de estas pruebas son copias de listados reales vistos en capturas.
 */
class SelectorArmaTest {

    private static final List<ArmaInventario> INVENTARIO_CON_GLOCK = inventario(
            "Escopeta Hatsan Cal. 12 7T Folding Stock HATSAN 12",
            "Revolver S&W CAL .357 4\" M66 SKU 162662 SMITH & WESSON 357",
            "Pistola Glock Mod G-44 CAL 22 LR 9T 2P GLOCK 22LR",
            "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM",
            "Revovler Martial Ultra Cal 38 4\" Cacha Plástica INDUMIL 38L");

    private static final List<ArmaInventario> INVENTARIO_SIN_GLOCK = inventario(
            "Pistola Cordova Compacta Cal 9MM 9T 3P (Pequeña) CORDOVA 9MM",
            "Pistola CZ P10 CAL 9 mm 2P 9T (Grande) CZ 9MM",
            "Pistola Beretta 92 FS COMPACT CAL 9MM BERETTA 9MM",
            "Pistola Beretta 92X Performance Cal 9mm 2P 9T BERETTA 9MM");

    private static final List<ArmaInventario> INVENTARIO_SOLO_REVOLVERES = inventario(
            "Revolver Martial Ultra Cal 38 4\" Cacha Madera INDUMIL 38L",
            "Escopeta Indumil Cap 1T Cal 12 (Defens Per) INDUMIL 12",
            "Pistola Glock Mod G-44 CAL 22 LR 9T 2P GLOCK 22LR");

    private final SelectorArma selector = new SelectorArma();

    @Test
    void eligeElObjetivoAunqueNoSeaLaPrimeraFila() {
        Optional<EleccionArma> eleccion = selector.elegir(INVENTARIO_CON_GLOCK,
                reglas(List.of(CriterioArma.de("glock", "g43")), PoliticaSinObjetivo.SEGUIR_ESPERANDO));

        assertEquals(3, eleccion.orElseThrow().arma().posicion());
        assertEquals(MotivoEleccion.OBJETIVO, eleccion.orElseThrow().motivo());
    }

    @Test
    void respetaLaPrioridadDeLosObjetivos() {
        List<CriterioArma> objetivos = List.of(CriterioArma.de("beretta", "92x"), CriterioArma.de("cordova"));

        EleccionArma eleccion = selector.elegir(INVENTARIO_SIN_GLOCK,
                reglas(objetivos, PoliticaSinObjetivo.SEGUIR_ESPERANDO)).orElseThrow();

        assertTrue(eleccion.arma().descripcion().contains("92X"));
        assertEquals(0, eleccion.prioridad());
    }

    @Test
    void seguirEsperandoIgnoraOtrasPistolas() {
        // Reproduce lo que reportaba el script de terceros: cuatro pistolas y ninguna permitida.
        Optional<EleccionArma> eleccion = selector.elegir(INVENTARIO_SIN_GLOCK,
                reglas(List.of(CriterioArma.de("glock", "g43")), PoliticaSinObjetivo.SEGUIR_ESPERANDO));

        assertTrue(eleccion.isEmpty());
    }

    @Test
    void usaAlternativasCuandoNoHayObjetivo() {
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(CriterioArma.de("glock", "g43")),
                PoliticaSinObjetivo.USAR_ALTERNATIVAS,
                List.of(CriterioArma.de("jericho"), CriterioArma.de("cz", "p10")),
                "", List.of(), 1);

        EleccionArma eleccion = selector.elegir(INVENTARIO_SIN_GLOCK, reglas).orElseThrow();

        assertEquals(MotivoEleccion.ALTERNATIVA, eleccion.motivo());
        assertEquals(1, eleccion.prioridad());
        assertTrue(eleccion.arma().descripcion().contains("CZ P10"));
    }

    @Test
    void cualquieraDelTipoTomaLaPrimeraPistolaNoExcluida() {
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(CriterioArma.de("glock", "g43")),
                PoliticaSinObjetivo.CUALQUIERA_DEL_TIPO, List.of(), "Pistola", List.of("22 lr"), 1);

        assertTrue(selector.elegir(INVENTARIO_SOLO_REVOLVERES, reglas).isEmpty());

        EleccionArma eleccion = selector.elegir(INVENTARIO_SIN_GLOCK, reglas).orElseThrow();
        assertEquals(MotivoEleccion.CUALQUIERA_DEL_TIPO, eleccion.motivo());
        assertEquals(0, eleccion.arma().posicion());
    }

    @Test
    void lasExclusionesGananAunqueElArmaSeaObjetivo() {
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(CriterioArma.de("glock")),
                PoliticaSinObjetivo.SEGUIR_ESPERANDO, List.of(), "", List.of("22 lr"), 1);

        EleccionArma eleccion = selector.elegir(INVENTARIO_CON_GLOCK, reglas).orElseThrow();

        assertTrue(eleccion.arma().descripcion().contains("G-43"));
    }

    @Test
    void reconoceElTipoAunqueVengaEnMayusculasAlInicio() {
        List<ArmaInventario> filas = inventario("PISTOLA Pistola Glock Mod. G-26 Cal 9mm 9T 2P (Pequeña) 9MM PORTE 1");
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(),
                PoliticaSinObjetivo.CUALQUIERA_DEL_TIPO, List.of(), "pistola", List.of(), 1);

        assertTrue(selector.elegir(filas, reglas).isPresent());
        assertTrue(selector.elegir(filas,
                reglas(List.of(CriterioArma.de("glock", "g-26")), PoliticaSinObjetivo.SEGUIR_ESPERANDO)).isPresent());
    }

    @Test
    void inventarioVacioNoEligeNada() {
        assertTrue(selector.elegir(List.of(),
                reglas(List.of(CriterioArma.de("glock")), PoliticaSinObjetivo.SEGUIR_ESPERANDO)).isEmpty());
    }

    private static ReglasSeleccion reglas(List<CriterioArma> objetivos, PoliticaSinObjetivo politica) {
        return new ReglasSeleccion(Modalidad.PORTE, objetivos, politica, List.of(), "", List.of(), 1);
    }

    private static List<ArmaInventario> inventario(String... descripciones) {
        List<ArmaInventario> filas = new ArrayList<>();
        for (int i = 0; i < descripciones.length; i++) {
            filas.add(new ArmaInventario(i, descripciones[i], ""));
        }
        return filas;
    }
}
