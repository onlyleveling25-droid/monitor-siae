package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.portal.PortalDriver;

import java.time.Duration;

/**
 * Repite el ciclo de un cliente hasta que haya algo que la persona deba atender o hasta que se
 * pida detenerlo.
 *
 * <p>Debe ejecutarse en el mismo hilo que creo el {@link PortalDriver}. No cierra el portal al
 * terminar: si aparto un arma, la ventana debe quedar abierta para que la persona termine el
 * tramite.
 */
public final class MonitorCliente implements Runnable {

    /** Errores inesperados seguidos que se toleran antes de detenerse; la plataforma a veces falla un momento. */
    public static final int FALLOS_TOLERADOS = 3;

    static final Duration ESPERA_FUERA_DE_HORARIO = Duration.ofSeconds(30);

    private final Cliente cliente;
    private final PortalDriver portal;
    private final CicloMonitoreo ciclo;
    private final VentanaHoraria ventana;
    private final Duration intervalo;
    private final Pausa pausa;
    private final OyenteMonitoreo oyente;

    private volatile boolean detencionSolicitada;

    public MonitorCliente(Cliente cliente, PortalDriver portal, CicloMonitoreo ciclo, VentanaHoraria ventana,
                          Duration intervalo, Pausa pausa, OyenteMonitoreo oyente) {
        this.cliente = cliente;
        this.portal = portal;
        this.ciclo = ciclo;
        this.ventana = ventana;
        this.intervalo = intervalo;
        this.pausa = pausa;
        this.oyente = oyente;
    }

    /** Pide detener el monitor; termina al acabar el ciclo en curso o la espera actual. */
    public void detener() {
        detencionSolicitada = true;
        pausa.despertar();
    }

    @Override
    public void run() {
        oyente.alIniciar(cliente);
        MotivoDetencion motivo = MotivoDetencion.SOLICITADO_POR_USUARIO;
        try {
            motivo = vigilar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            oyente.alDetenerse(cliente, motivo);
        }
    }

    private MotivoDetencion vigilar() throws InterruptedException {
        int fallosSeguidos = 0;
        while (!detencionSolicitada) {
            if (!ventana.permiteAhora()) {
                oyente.fueraDeHorario(cliente);
                pausa.esperar(ESPERA_FUERA_DE_HORARIO);
                continue;
            }

            ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal);
            oyente.alTerminarCiclo(cliente, resultado);

            if (resultado.detieneMonitoreo()) {
                return motivoDe(resultado);
            }

            if (resultado instanceof ResultadoCiclo.Inesperado) {
                fallosSeguidos++;
                if (fallosSeguidos >= FALLOS_TOLERADOS) {
                    return MotivoDetencion.FALLOS_REPETIDOS;
                }
                // Espera creciente para no insistir sobre una plataforma que esta fallando.
                pausa.esperar(intervalo.multipliedBy(fallosSeguidos + 1L));
                continue;
            }

            fallosSeguidos = 0;
            pausa.esperar(intervalo);
        }
        return MotivoDetencion.SOLICITADO_POR_USUARIO;
    }

    private static MotivoDetencion motivoDe(ResultadoCiclo resultado) {
        return switch (resultado) {
            case ResultadoCiclo.ArmaApartada a -> MotivoDetencion.ARMA_APARTADA;
            case ResultadoCiclo.ArmaEncontrada e -> MotivoDetencion.ARMA_ENCONTRADA;
            case ResultadoCiclo.SesionVencida s -> MotivoDetencion.SESION_VENCIDA;
            default -> throw new IllegalStateException("Resultado sin motivo de detencion: " + resultado);
        };
    }
}
