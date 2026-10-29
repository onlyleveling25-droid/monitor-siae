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

    /**
     * Se pidio vigilar la solicitud abierta y no hay ninguna que coincida. El monitor se detiene para
     * que la persona decida: revisar el portal o iniciar de nuevo desde el paso 1.
     */
    record SolicitudNoEncontrada(String detalle) implements ResultadoCiclo {
        public boolean detieneMonitoreo() {
            return true;
        }

        public String descripcion() {
            return detalle;
        }
    }

    /** La solicitud ya esta abierta en "Estado de tramite" y el portal aun no tiene inventario. */
    record SinInventarioEnSolicitud() implements ResultadoCiclo {
        public String descripcion() {
            return "Solicitud abierta en Estado de trámite: sin inventario por ahora.";
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
    record ArmaEncontrada(String seccional, EleccionArma eleccion, List<ArmaInventario> inventario)
            implements ResultadoCiclo {
        public ArmaEncontrada {
            inventario = List.copyOf(inventario);
        }

        public ArmaEncontrada(String seccional, EleccionArma eleccion) {
            this(seccional, eleccion, List.of(eleccion.arma()));
        }

        public boolean detieneMonitoreo() {
            return true;
        }

        public String descripcion() {
            return "Arma disponible en " + seccional + ": " + eleccion.arma().descripcion();
        }
    }

    /** Se intento apartar y la plataforma no lo confirmo; probablemente otra persona la tomo. */
    record ApartadoFallido(String seccional, EleccionArma eleccion, int intentos,
                           List<ArmaInventario> inventario) implements ResultadoCiclo {
        public ApartadoFallido {
            inventario = List.copyOf(inventario);
        }

        public ApartadoFallido(String seccional, EleccionArma eleccion, int intentos) {
            this(seccional, eleccion, intentos, List.of(eleccion.arma()));
        }

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

    /** El servidor del portal no contesto (timeout o error 5xx). No cuenta para detener el monitor. */
    record PortalNoResponde(String detalle) implements ResultadoCiclo {
        public String descripcion() {
            return "El portal no respondió: " + detalle;
        }
    }

    record Inesperado(String detalle) implements ResultadoCiclo {
        public String descripcion() {
            return "Respuesta inesperada de la plataforma: " + detalle;
        }
    }

    record ArmaApartada(String seccional, EleccionArma eleccion, int intentos,
                        List<ArmaInventario> inventario, boolean cierreSellado)
            implements ResultadoCiclo {

        public ArmaApartada {
            inventario = List.copyOf(inventario);
        }

        public ArmaApartada(String seccional, EleccionArma eleccion, int intentos,
                            List<ArmaInventario> inventario) {
            this(seccional, eleccion, intentos, inventario, true);
        }

        public ArmaApartada(String seccional, EleccionArma eleccion, int intentos) {
            this(seccional, eleccion, intentos, List.of(eleccion.arma()), true);
        }

        public boolean detieneMonitoreo() {
            return true;
        }

        public String descripcion() {
            return "Arma apartada en " + seccional + ": " + eleccion.arma().descripcion()
                    + (cierreSellado ? "" : " · la reserva no quedó sellada, pulsa FINALIZAR");
        }
    }
}
