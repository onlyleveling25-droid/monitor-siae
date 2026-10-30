package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe la actividad del monitor en el registro de la aplicacion.
 */
public final class OyenteRegistro implements OyenteMonitoreo {

    private static final Logger REGISTRO = LoggerFactory.getLogger("monitor");

    @Override
    public void alIniciar(Cliente cliente) {
        REGISTRO.info("[{}] Monitoreo iniciado", cliente.nombre());
    }

    @Override
    public void fueraDeHorario(Cliente cliente) {
        REGISTRO.debug("[{}] Fuera del horario de operación", cliente.nombre());
    }

    @Override
    public void alTerminarCiclo(Cliente cliente, ResultadoCiclo resultado) {
        switch (resultado) {
            case ResultadoCiclo.ArmaApartada a -> REGISTRO.warn("[{}] {}", cliente.nombre(), a.descripcion());
            case ResultadoCiclo.ArmaEncontrada e -> REGISTRO.warn("[{}] {}", cliente.nombre(), e.descripcion());
            case ResultadoCiclo.SesionVencida s -> REGISTRO.error("[{}] {}", cliente.nombre(), s.descripcion());
            case ResultadoCiclo.Inesperado i -> REGISTRO.error("[{}] {}", cliente.nombre(), i.descripcion());
            case ResultadoCiclo.PortalNoResponde p -> REGISTRO.warn("[{}] {}", cliente.nombre(), p.descripcion());
            case ResultadoCiclo.ApartadoFallido f -> REGISTRO.warn("[{}] {}", cliente.nombre(), f.descripcion());
            default -> REGISTRO.info("[{}] {}", cliente.nombre(), resultado.descripcion());
        }
    }

    @Override
    public void alDetenerse(Cliente cliente, MotivoDetencion motivo) {
        String detalle = switch (motivo) {
            case SOLICITADO_POR_USUARIO -> "por solicitud del usuario";
            case ARMA_APARTADA -> "porque se apartó un arma";
            case ARMA_ENCONTRADA -> "porque se encontró un arma";
            case SESION_VENCIDA -> "porque venció la sesión del portal";
            case FALLOS_REPETIDOS -> "por fallos repetidos del portal";
            case NAVEGADOR_CERRADO -> "porque se cerró el navegador";
            case ERROR_INESPERADO -> "por un error inesperado";
            case SOLICITUD_NO_ENCONTRADA -> "porque no se encontró la solicitud en Estado de trámite";
            case INVENTARIO_SIN_INTERES -> "porque hubo inventario sin armas de interés (tabla abierta para elegir a mano)";
            case APARTADO_FALLIDO -> "porque no se pudo apartar el arma (tabla abierta para elegir a mano)";
        };
        REGISTRO.info("[{}] Monitoreo detenido: {}", cliente.nombre(), detalle);
    }
}
