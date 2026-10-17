package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.ArmaInventario;

import java.util.List;

/**
 * Lo que ocurrio en una vuelta del monitor para un cliente.
 */
public sealed interface ResultadoCiclo {

    /** Indica si el monitor de ese cliente debe detenerse despues de este resultado. */
    default boolean detieneMonitoreo() {
        return false;
    }

    /** Descripcion breve para el registro y la pantalla. */
    String descripcion();

    record TramiteNoDisponible(String detalle) implements ResultadoCiclo {
        public String descripcion() {
            return "Opción del trámite no disponible: " + detalle;
        }
    }

    record SeccionalesCerradas() implements ResultadoCiclo {
        public String descripcion() {
            return "Seccionales cerradas.";
        }
    }

    record SinSeccionalObjetivo(List<String> abiertas) implements ResultadoCiclo {
        public SinSeccionalObjetivo {
            abiertas = List.copyOf(abiertas);
        }

        public String descripcion() {
            return "Abiertas pero no objetivo: " + String.join(" | ", abiertas);
        }
    }

    record SinInventario(String seccional) implements ResultadoCiclo {
        public String descripcion() {
            return "Sin inventario en " + seccional + ".";
        }
    }

    record SinArmaDeInteres(String seccional, List<ArmaInventario> inventario) implements ResultadoCiclo {
        public SinArmaDeInteres {
            inventario = List.copyOf(inventario);
        }

        public String descripcion() {
            return "Inventario en " + seccional + " (" + inventario.size() + " armas) sin armas de interés.";
        }
    }

    /** Modo alerta: hay un arma que cumple, pero no se selecciona. */
    record ArmaEncontrada(String seccional, EleccionArma eleccion) implements ResultadoCiclo {
        public boolean detieneMonitoreo() {
            return true;
        }

        public String descripcion() {
            return "Arma disponible en " + seccional + ": " + eleccion.arma().descripcion();
        }
    }

    record ArmaApartada(String seccional, EleccionArma eleccion, int intentos) implements ResultadoCiclo {
        public boolean detieneMonitoreo() {
            return true;
        }

        public String descripcion() {
            return "Arma apartada en " + seccional + ": " + eleccion.arma().descripcion();
        }
    }

    /** Se intento apartar y la plataforma no lo confirmo; probablemente otra persona la tomo. */
    record ApartadoFallido(String seccional, EleccionArma eleccion, int intentos) implements ResultadoCiclo {
        public String descripcion() {
            return "No se pudo apartar tras " + intentos + " intentos: " + eleccion.arma().descripcion();
        }
    }

    record SesionVencida(String detalle) implements ResultadoCiclo {
        public boolean detieneMonitoreo() {
            return true;
        }

        public String descripcion() {
            return "Sesión vencida: " + detalle;
        }
    }

    record Inesperado(String detalle) implements ResultadoCiclo {
        public String descripcion() {
            return "Respuesta inesperada de la plataforma: " + detalle;
        }
    }
}
