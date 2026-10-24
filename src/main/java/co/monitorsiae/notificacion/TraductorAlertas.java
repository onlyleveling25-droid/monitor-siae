package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.monitoreo.EleccionArma;
import co.monitorsiae.monitoreo.MotivoDetencion;
import co.monitorsiae.monitoreo.ResultadoCiclo;

import java.util.Optional;

/**
 * Decide que resultados del monitor merecen una alerta y redacta su texto. Solo alerta lo que
 * exige que la persona actue; el resto queda en el registro.
 */
public final class TraductorAlertas {

    public Optional<Alerta> deResultado(Cliente cliente, ResultadoCiclo resultado) {
        return switch (resultado) {
            case ResultadoCiclo.ArmaApartada a -> Optional.of(new Alerta(cliente.id(), Gravedad.OPORTUNIDAD,
                    "Arma apartada: " + cliente.nombre(),
                    describir(a.eleccion()) + " en " + a.seccional()
                            + ". Termine el trámite en la ventana del navegador de este cliente.",
                    "Atención. Arma apartada para " + cliente.nombre() + ". Termine el trámite."));
            case ResultadoCiclo.ArmaEncontrada e -> Optional.of(new Alerta(cliente.id(), Gravedad.OPORTUNIDAD,
                    "Arma disponible: " + cliente.nombre(),
                    describir(e.eleccion()) + " en " + e.seccional()
                            + ". Selecciónela ya en la ventana del navegador de este cliente.",
                    "Atención. Hay un arma disponible para " + cliente.nombre() + ". Selecciónela ya."));
            // Aunque no se logró sellar la reserva, hubo inventario real: se avisa igual que una
            // oportunidad para que la persona intervenga de inmediato, sin esperar la ventana horaria.
            case ResultadoCiclo.ApartadoFallido f -> Optional.of(new Alerta(cliente.id(), Gravedad.OPORTUNIDAD,
                    "Arma disponible (no se pudo apartar): " + cliente.nombre(),
                    describir(f.eleccion()) + " en " + f.seccional()
                            + ". La plataforma no confirmó el apartado tras " + f.intentos()
                            + " intento(s); revise la ventana del navegador de este cliente.",
                    "Atención. Había un arma disponible para " + cliente.nombre()
                            + " pero no se pudo apartar. Revise de inmediato."));
            case ResultadoCiclo.SesionVencida s -> Optional.of(new Alerta(cliente.id(), Gravedad.PROBLEMA,
                    "Sesión cerrada: " + cliente.nombre(),
                    "La plataforma cerró la sesión. Inicie sesión de nuevo en la ventana de este cliente y "
                            + "reanude el monitoreo.",
                    "La sesión de " + cliente.nombre() + " se cerró. Inicie sesión de nuevo."));
            default -> Optional.empty();
        };
    }

    /** Alertas que solo se conocen al detenerse el monitor y no vienen de un resultado puntual. */
    public Optional<Alerta> deDetencion(Cliente cliente, MotivoDetencion motivo) {
        if (motivo != MotivoDetencion.FALLOS_REPETIDOS) {
            return Optional.empty();
        }
        return Optional.of(new Alerta(cliente.id(), Gravedad.PROBLEMA,
                "Monitoreo detenido: " + cliente.nombre(),
                "La plataforma respondió con errores varias veces seguidas. Revise la ventana de este "
                        + "cliente y reanude el monitoreo.",
                "El monitoreo de " + cliente.nombre() + " se detuvo por errores de la plataforma."));
    }

    private static String describir(EleccionArma eleccion) {
        String arma = eleccion.arma().descripcion();
        return switch (eleccion.motivo()) {
            case OBJETIVO -> arma;
            case ALTERNATIVA -> arma + " (alternativa, el objetivo no estaba)";
            case CUALQUIERA_DEL_TIPO -> arma + " (reemplazo por tipo, el objetivo no estaba)";
        };
    }
}
