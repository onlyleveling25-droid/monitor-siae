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
    record SolicitudNoEncontrada(String detalle, String queHacer) implements ResultadoCiclo {
        public SolicitudNoEncontrada {
            queHacer = queHacer == null ? "" : queHacer;
        }

        public SolicitudNoEncontrada(String detalle) {
            this(detalle, "");
        }

        public boolean detieneMonitoreo() {
            return true;
        }

        /** El resumen y, debajo, los pasos que debe seguir la persona. */
        public String textoCompleto() {
            return queHacer.isBlank() ? detalle : detalle + "\n\n" + queHacer;
        }

        public String descripcion() {
            return detalle;
        }
    }

    /**
     * La solicitud ya esta abierta en "Estado de tramite" y el portal aun no tiene inventario.
     *
     * @param numero numero de la solicitud vigilada; vacio si no se conoce
     */
    record SinInventarioEnSolicitud(String numero) implements ResultadoCiclo {
        public SinInventarioEnSolicitud {
            numero = numero == null ? "" : numero.strip();
        }

        public SinInventarioEnSolicitud() {
            this("");
        }

        public String descripcion() {
            return (numero.isEmpty() ? "Solicitud abierta" : "Solicitud " + numero)
                    + " en Estado de trámite: sin inventario por ahora.";
        }
    }

    /** Hubo inventario, pero ninguna arma cumple los objetivos, alternativas ni el tipo comodin. */
    /**
     * @param yaVisto todas sus armas ya se le habían mostrado a la persona (por ejemplo, reanudó sin
     *                tomar ninguna): se sigue vigilando en silencio en vez de detenerse otra vez
     */
    record SinArmaDeInteres(String seccional, List<ArmaInventario> inventario, boolean yaVisto)
            implements ResultadoCiclo {
        public SinArmaDeInteres {
            inventario = List.copyOf(inventario);
        }

        public SinArmaDeInteres(String seccional, List<ArmaInventario> inventario) {
            this(seccional, inventario, false);
        }

        /** Con armas nuevas se detiene y deja la tabla abierta para que la persona elija a mano si quiere. */
        public boolean detieneMonitoreo() {
            return !yaVisto;
        }

        public String descripcion() {
            return "Inventario en " + seccional + " (" + inventario.size() + " armas) sin armas de interés.";
        }

        /** Identifica este inventario: dos lecturas con la misma firma son el mismo listado. */
        public String firma() {
            StringBuilder firma = new StringBuilder(seccional);
            inventario.forEach(arma -> firma.append('\n').append(arma.descripcion()));
            return firma.toString();
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

    /**
     * Se intento apartar y no se logro: la plataforma no lo confirmo (probablemente otra persona la
     * tomo) o un paso del portal fallo a mitad del camino.
     *
     * @param causa que paso del portal fallo; vacio si simplemente no se confirmo
     */
    record ApartadoFallido(String seccional, EleccionArma eleccion, int intentos,
                           List<ArmaInventario> inventario, String causa) implements ResultadoCiclo {
        public ApartadoFallido {
            inventario = List.copyOf(inventario);
            causa = causa == null ? "" : causa.strip();
        }

        /** Se detiene con la tabla abierta para que la persona aparte el arma a mano. */
        public boolean detieneMonitoreo() {
            return true;
        }

        public ApartadoFallido(String seccional, EleccionArma eleccion, int intentos,
                               List<ArmaInventario> inventario) {
            this(seccional, eleccion, intentos, inventario, "");
        }

        public ApartadoFallido(String seccional, EleccionArma eleccion, int intentos) {
            this(seccional, eleccion, intentos, List.of(eleccion.arma()));
        }

        public String descripcion() {
            return causa.isEmpty()
                    ? "No se pudo apartar tras " + intentos + " intentos: " + eleccion.arma().descripcion()
                    : "No se pudo apartar " + eleccion.arma().descripcion() + ": " + causa
                    + " Apártela a mano en el navegador.";
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
