package co.monitorsiae.dominio;

import java.util.List;

/**
 * Datos validos de partida para las pruebas; cada prueba cambia solo lo que le interesa.
 */
public final class ClientesDePrueba {

    public static final String DESCRIPCION_VALIDA = "Solicito el arma para defensa personal. ".repeat(4);

    private ClientesDePrueba() {
    }

    public static Cliente valido(String nombre) {
        return conReglas(nombre, new ReglasSeleccion(Modalidad.PORTE,
                List.of(CriterioArma.de("glock", "g-43"), CriterioArma.de("glock", "g-26")),
                PoliticaSinObjetivo.USAR_ALTERNATIVAS,
                List.of(CriterioArma.de("cordova", "compacta")),
                "Pistola", List.of("22 lr"), 2));
    }

    public static Cliente conReglas(String nombre, ReglasSeleccion reglas) {
        return new Cliente(null, nombre, true, tramiteValido(DESCRIPCION_VALIDA), ModoReaccion.APARTAR, reglas);
    }

    public static Cliente conDescripcion(String nombre, String descripcion) {
        return new Cliente(null, nombre, true, tramiteValido(descripcion), ModoReaccion.APARTAR,
                valido(nombre).seleccion());
    }

    private static ParametrosTramite tramiteValido(String descripcion) {
        return new ParametrosTramite("Defensa Personal", "Armas personas naturales: Adquisición de arma",
                List.of("Medellín", "Bogotá"), descripcion);
    }
}
