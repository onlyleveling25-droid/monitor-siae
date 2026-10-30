package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.persistencia.RepositorioBitacora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda en la bitácora los hechos operativos del monitor. Los ciclos sin novedad no se registran
 * y un mismo hallazgo repetido de forma consecutiva se anota una sola vez, para que la bitácora
 * no se llene de ruido. Un fallo al escribir nunca interrumpe el monitoreo.
 */
public final class OyenteBitacora implements OyenteMonitoreo {

    private static final Logger REGISTRO = LoggerFactory.getLogger(OyenteBitacora.class);

    private final RepositorioBitacora bitacora;
    private final Clock reloj;
    private final Map<String, String> ultimoPorCliente = new ConcurrentHashMap<>();
    private final Map<String, Instant> inicioPorCliente = new ConcurrentHashMap<>();
    private final InventarioNuevo inventarioNuevo = new InventarioNuevo();

    public OyenteBitacora(RepositorioBitacora bitacora, Clock reloj) {
        this.bitacora = Objects.requireNonNull(bitacora);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void alIniciar(Cliente cliente) {
        inventarioNuevo.olvidar(cliente.id());
        ultimoPorCliente.remove(cliente.id());
        inicioPorCliente.put(cliente.id(), reloj.instant());
        String seccionales = String.join(", ", cliente.tramite().seccionales());
        anotar(cliente, TipoEvento.MONITOREO_INICIADO,
                seccionales.isEmpty() ? "Se inició el monitoreo."
                        : "Se inició el monitoreo. Seccionales que busca: " + seccionales + ".",
                seccionales, "", null, "");
    }

    @Override
    public void alTerminarCiclo(Cliente cliente, ResultadoCiclo resultado) {
        if (resultado instanceof ResultadoCiclo.SinArmaDeInteres sinInteres) {
            // Una vez por inventario: el mismo listado en los ciclos siguientes no se repite.
            if (inventarioNuevo.registrar(cliente.id(), resultado)) {
                anotar(cliente, TipoEvento.INVENTARIO_SIN_INTERES, detalleSinInteres(sinInteres),
                        sinInteres.seccional(), "", null, "");
            }
            return;
        }
        inventarioNuevo.registrar(cliente.id(), resultado);
        TipoEvento tipo = switch (resultado) {
            case ResultadoCiclo.ArmaEncontrada e -> TipoEvento.OPORTUNIDAD;
            case ResultadoCiclo.ArmaApartada a -> TipoEvento.ARMA_APARTADA;
            case ResultadoCiclo.ApartadoFallido f -> TipoEvento.APARTADO_FALLIDO;
            case ResultadoCiclo.SesionVencida s -> TipoEvento.SESION_VENCIDA;
            case ResultadoCiclo.Inesperado i -> TipoEvento.RESPUESTA_INESPERADA;
            case ResultadoCiclo.PortalNoResponde p -> TipoEvento.PORTAL_SIN_RESPUESTA;
            default -> null;
        };
        if (tipo == null) {
            return;
        }
        String clave = tipo + "|" + resultado.descripcion();
        if (clave.equals(ultimoPorCliente.put(cliente.id(), clave))) {
            return;
        }
        String seccional = switch (resultado) {
            case ResultadoCiclo.ArmaEncontrada e -> e.seccional();
            case ResultadoCiclo.ArmaApartada a -> a.seccional();
            case ResultadoCiclo.ApartadoFallido f -> f.seccional();
            default -> "";
        };
        String arma = switch (resultado) {
            case ResultadoCiclo.ArmaEncontrada e -> e.eleccion().arma().descripcion();
            case ResultadoCiclo.ArmaApartada a -> a.eleccion().arma().descripcion();
            case ResultadoCiclo.ApartadoFallido f -> f.eleccion().arma().descripcion();
            default -> "";
        };
        anotar(cliente, tipo, resultado.descripcion(), seccional, arma, null,
                EventoOperativo.capturaEn(resultado.descripcion()));
    }

    @Override
    public void alDetenerse(Cliente cliente, MotivoDetencion motivo) {
        ultimoPorCliente.remove(cliente.id());
        Instant inicio = inicioPorCliente.remove(cliente.id());
        Duration duracion = inicio == null ? null : Duration.between(inicio, reloj.instant());
        EventoOperativo conDuracion = new EventoOperativo(reloj.instant(), cliente.id(), cliente.nombre(),
                TipoEvento.MONITOREO_DETENIDO, "", "", "", duracion);
        String duro = duracion == null ? "" : " Duró " + conDuracion.duracionTexto() + ".";
        anotar(cliente, TipoEvento.MONITOREO_DETENIDO,
                "Monitoreo detenido: " + texto(motivo) + "." + duro, "", "", duracion, "");
    }

    private void anotar(Cliente cliente, TipoEvento tipo, String detalle, String seccional, String arma,
                        Duration duracion, String captura) {
        try {
            bitacora.registrar(new EventoOperativo(reloj.instant(), cliente.id(), cliente.nombre(), tipo, detalle,
                    seccional, arma, duracion, captura));
        } catch (RuntimeException e) {
            REGISTRO.warn("No se pudo escribir en la bitácora; el monitoreo continúa.", e);
        }
    }

    /** La lista completa queda en el detalle del evento, para revisarla aunque el historial ya no la tenga. */
    static String detalleSinInteres(ResultadoCiclo.SinArmaDeInteres sinInteres) {
        StringBuilder detalle = new StringBuilder(sinInteres.descripcion())
                .append(" Ninguna cumplía los objetivos ni las alternativas; no se seleccionó nada. Inventario:");
        sinInteres.inventario().forEach(arma -> detalle.append("\n• ").append(arma.descripcion()));
        return detalle.toString();
    }

    private static String texto(MotivoDetencion motivo) {
        return switch (motivo) {
            case SOLICITADO_POR_USUARIO -> "por solicitud del usuario";
            case ARMA_APARTADA -> "se apartó un arma";
            case ARMA_ENCONTRADA -> "se encontró un arma";
            case SESION_VENCIDA -> "venció la sesión del portal";
            case FALLOS_REPETIDOS -> "fallos repetidos del portal";
            case NAVEGADOR_CERRADO -> "se cerró el navegador";
            case ERROR_INESPERADO -> "error inesperado";
            case SOLICITUD_NO_ENCONTRADA -> "no se encontró la solicitud en Estado de trámite";
            case INVENTARIO_SIN_INTERES -> "hubo inventario sin armas de interés; la tabla quedó abierta para elegir a mano";
            case APARTADO_FALLIDO -> "no se pudo apartar el arma; la tabla quedó abierta para elegir a mano";
        };
    }
}
