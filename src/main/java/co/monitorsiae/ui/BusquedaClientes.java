package co.monitorsiae.ui;

import co.monitorsiae.dominio.Texto;

import java.util.Arrays;

/**
 * Filtro del buscador del Panel. Cada palabra escrita debe aparecer en el nombre del cliente, en
 * cualquier orden y aunque esté incompleta: "ana per" encuentra a "Ana Pérez". No distingue
 * mayúsculas ni tildes.
 */
final class BusquedaClientes {

    private BusquedaClientes() {
    }

    static boolean coincide(String nombreCliente, String consulta) {
        String buscado = Texto.normalizar(consulta);
        if (buscado.isEmpty()) {
            return true;
        }
        String nombre = Texto.normalizar(nombreCliente);
        return Arrays.stream(buscado.split(" ")).allMatch(nombre::contains);
    }
}
