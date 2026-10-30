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
        Optional<Alerta> alerta = switch (resultado) {
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
                            + (f.causa().isEmpty()
                            ? ". La plataforma no confirmó el apartado tras " + f.intentos()
                            + " intento(s). El monitoreo se detuvo y dejó la tabla abierta: pulse «Ir al "
                            + "navegador» y apártela a mano."
                            : ". Un paso del portal falló al apartarla (" + f.causa() + "). El programa no "
                            + "insistió para no confirmar nada a ciegas y se detuvo: pulse «Ir al navegador» y "
                            + "apártela usted."),
                    "Atención. Había un arma disponible para " + cliente.nombre()
                            + " pero no se pudo apartar. Revise de inmediato."));
            // Hubo inventario aunque nada cumple: se avisa para que la persona lo revise. Quien escucha
            // se encarga de que suene una sola vez por inventario, no en cada ciclo.
            case ResultadoCiclo.SinArmaDeInteres s -> Optional.of(new Alerta(cliente.id(), Gravedad.OPORTUNIDAD,
                    "Inventario sin arma de interés: " + cliente.nombre(),
                    "Apareció inventario en " + s.seccional() + " (" + s.inventario().size() + " armas), pero "
                            + "ninguna cumple los objetivos ni las alternativas; no se seleccionó nada. La lista "
                            + "completa queda en la bitácora y en el historial. El monitoreo se detuvo y dejó la tabla abierta: "
                            + "pulse «Ir al navegador» si quiere elegir a mano. Si no toma ninguna, reanúdelo desde el "
                            + "Panel (⋯ → «Reanudar monitoreo»).",
                    "Atención. Hay inventario para " + cliente.nombre()
                            + ", pero ninguna arma es de interés. Revise la lista."));
            // El monitor se detuvo solo: la alerta lleva los pasos para que la persona sepa qué decidir.
            case ResultadoCiclo.SolicitudNoEncontrada n -> Optional.of(new Alerta(cliente.id(), Gravedad.PROBLEMA,
                    "Revise la solicitud: " + cliente.nombre(),
                    n.textoCompleto(),
                    "El monitoreo de " + cliente.nombre() + " se detuvo. Revise la solicitud en Estado de trámite."));
            case ResultadoCiclo.SesionVencida s -> Optional.of(new Alerta(cliente.id(), Gravedad.PROBLEMA,
                    "Sesión cerrada: " + cliente.nombre(),
                    "La plataforma cerró la sesión. Inicie sesión de nuevo en la ventana de este cliente y "
                            + "reanude el monitoreo.",
                    "La sesión de " + cliente.nombre() + " se cerró. Inicie sesión de nuevo."));
            default -> Optional.empty();
        };
        return alerta;
    }

    /** Alertas que solo se conocen al detenerse el monitor y no vienen de un resultado puntual. */
    public Optional<Alerta> deDetencion(Cliente cliente, MotivoDetencion motivo) {
        // SOLICITUD_NO_ENCONTRADA ya sonó con el resultado del ciclo, que trae los pasos a seguir.
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
