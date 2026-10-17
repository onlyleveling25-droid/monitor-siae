package co.monitorsiae.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextoTest {

    @Test
    void normalizaTildesMayusculasYSeparadores() {
        assertEquals("medellin", Texto.normalizar("  Medellín "));
        assertEquals("g 43", Texto.normalizar("G-43"));
        assertEquals("g 43", Texto.normalizar("G43"));
        assertEquals("cal 9 mm", Texto.normalizar("CAL 9mm"));
        assertEquals("s w", Texto.normalizar("S&W"));
        assertEquals("", Texto.normalizar(null));
    }

    @Test
    void comparaOpcionesDeLaPlataformaSinImportarTildes() {
        assertTrue(Texto.equivalentes("Armas personas naturales: Adquisición de arma",
                "armas personas naturales adquisicion de arma"));
        assertTrue(Texto.equivalentes("16 - Usaquén, Bogotá D.C.", "16 usaquen bogota d c"));
    }

    @Test
    void buscaPalabrasCompletasYNoFragmentos() {
        String glock = "Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM";

        assertTrue(Texto.contieneTermino(glock, "g43"));
        assertTrue(Texto.contieneTermino(glock, "mod g-43"));
        assertFalse(Texto.contieneTermino(glock, "glock g-43"));
        assertFalse(Texto.contieneTermino(glock, "g4"));
        assertFalse(Texto.contieneTermino("Revolver S&W CAL .357", "35"));
        assertFalse(Texto.contieneTermino(glock, "  "));
    }

    @Test
    void contieneTodosExigeCadaTerminoYAlMenosUno() {
        String beretta = "Pistola Beretta 92 FS COMPACT CAL 9MM BERETTA 9MM";

        assertTrue(Texto.contieneTodos(beretta, List.of("beretta", "92", "compact")));
        assertFalse(Texto.contieneTodos(beretta, List.of("beretta", "92x")));
        assertFalse(Texto.contieneTodos(beretta, List.of()));
    }
}
