package co.monitorsiae.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusquedaClientesTest {

    @Test
    void sinConsultaMuestraTodos() {
        assertTrue(BusquedaClientes.coincide("Ana Pérez", ""));
        assertTrue(BusquedaClientes.coincide("Ana Pérez", "   "));
        assertTrue(BusquedaClientes.coincide("Ana Pérez", null));
    }

    @Test
    void ignoraMayusculasYTildes() {
        assertTrue(BusquedaClientes.coincide("María Gómez", "maria"));
        assertTrue(BusquedaClientes.coincide("maria gomez", "GÓMEZ"));
    }

    @Test
    void aceptaPalabrasIncompletasYEnCualquierOrden() {
        assertTrue(BusquedaClientes.coincide("Ana Pérez", "ana per"));
        assertTrue(BusquedaClientes.coincide("Ana Pérez", "per ana"));
        assertTrue(BusquedaClientes.coincide("Cliente de prueba", "prue"));
    }

    @Test
    void todasLasPalabrasDebenAparecer() {
        assertFalse(BusquedaClientes.coincide("Ana Pérez", "ana lopez"));
        assertFalse(BusquedaClientes.coincide("Ana Pérez", "carlos"));
    }
}
