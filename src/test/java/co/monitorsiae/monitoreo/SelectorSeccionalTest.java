package co.monitorsiae.monitoreo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorSeccionalTest {

    private final SelectorSeccional selector = new SelectorSeccional();

    @Test
    void aceptaElNombreCompletoOUnaParte() {
        List<String> abiertas = List.of("16 - Usaquén, Bogotá D.C.");

        assertEquals(Optional.of("16 - Usaquén, Bogotá D.C."), selector.elegir(abiertas, List.of("bogota")));
        assertEquals(Optional.of("16 - Usaquén, Bogotá D.C."),
                selector.elegir(abiertas, List.of("16 - Usaquén, Bogotá D.C.")));
    }

    @Test
    void respetaElOrdenDePreferenciaDelCliente() {
        List<String> abiertas = List.of("16 - Usaquén, Bogotá D.C.", "05 - Medellín, Antioquia");

        assertEquals(Optional.of("05 - Medellín, Antioquia"),
                selector.elegir(abiertas, List.of("Medellín", "Bogotá")));
    }

    @Test
    void ignoraSeccionalesQueNoSonObjetivo() {
        assertTrue(selector.elegir(List.of("73 - Cali, Valle del Cauca"), List.of("Medellín", "Bogotá")).isEmpty());
    }
}
