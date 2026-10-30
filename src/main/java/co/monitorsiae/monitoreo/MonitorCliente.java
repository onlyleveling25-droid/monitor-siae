package co.monitorsiae.monitoreo;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.SesionPortalCerradaException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private final Object bloqueoSolicitudes = new Object();
    private final List<CompletableFuture<Void>> solicitudesEnfoque = new ArrayList<>();

    private final SeguimientoSolicitud seguimiento;

    private volatile boolean detencionSolicitada;
    private boolean finalizado;

    public MonitorCliente(Cliente cliente, PortalDriver portal, CicloMonitoreo ciclo, VentanaHoraria ventana,
                          Duration intervalo, Pausa pausa, OyenteMonitoreo oyente) {
        this(cliente, portal, ciclo, ventana, intervalo, pausa, oyente, new SeguimientoSolicitud());
    }

    public MonitorCliente(Cliente cliente, PortalDriver portal, CicloMonitoreo ciclo, VentanaHoraria ventana,
                          Duration intervalo, Pausa pausa, OyenteMonitoreo oyente,
                          SeguimientoSolicitud seguimiento) {
        this.seguimiento = seguimiento;
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

    /**
     * Encola el enfoque para ejecutarlo en este mismo hilo, entre operaciones del portal.
     *
     * @return falso si el monitor ya terminó y la operación debe encolarse en su trabajador
     */
    public boolean solicitarEnfoque(CompletableFuture<Void> resultado) {
        synchronized (bloqueoSolicitudes) {
            if (finalizado) {
                return false;
            }
            solicitudesEnfoque.add(resultado);
        }
        pausa.despertar();
        return true;
    }

    @Override
    public void run() {
        oyente.alIniciar(cliente);
        MotivoDetencion motivo = MotivoDetencion.SOLICITADO_POR_USUARIO;
        try {
            motivo = vigilar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            try {
                motivo = portal.estaAbierto()
                        ? MotivoDetencion.ERROR_INESPERADO : MotivoDetencion.NAVEGADOR_CERRADO;
            } catch (RuntimeException errorEstado) {
                e.addSuppressed(errorEstado);
                motivo = MotivoDetencion.ERROR_INESPERADO;
            }
            throw e;
        } finally {
            procesarSolicitudesEnfoque(true);
            oyente.alDetenerse(cliente, motivo);
        }
    }

    private MotivoDetencion vigilar() throws InterruptedException {
        int fallosSeguidos = 0;
        boolean fueraDeHorarioNotificado = false;
        while (!detencionSolicitada) {
            if (!ventana.permiteAhora()) {
                oyente.fueraDeHorario(cliente);
                fueraDeHorarioNotificado = true;
                esperar(ESPERA_FUERA_DE_HORARIO);
                continue;
            }
            if (fueraDeHorarioNotificado) {
                oyente.dentroDeHorario(cliente);
                fueraDeHorarioNotificado = false;
            }

            ResultadoCiclo resultado = ciclo.ejecutar(cliente, portal, seguimiento);
            procesarSolicitudesEnfoque(false);
            oyente.alTerminarCiclo(cliente, resultado);

            if (resultado.detieneMonitoreo()) {
                return motivoDe(resultado);
            }

            if (resultado instanceof ResultadoCiclo.PortalNoResponde) {
                // El portal esta caido o lento: no es un fallo del programa. Se reintenta con la
                // espera normal y sin acercar el monitor a la detencion por fallos repetidos.
                esperar(intervalo);
                continue;
            }

            if (resultado instanceof ResultadoCiclo.Inesperado) {
                fallosSeguidos++;
                if (fallosSeguidos >= FALLOS_TOLERADOS) {
                    return MotivoDetencion.FALLOS_REPETIDOS;
                }
                // Espera creciente para no insistir sobre una plataforma que esta fallando.
                esperar(intervalo.multipliedBy(fallosSeguidos + 1L));
                continue;
            }

            fallosSeguidos = 0;
            esperar(intervalo);
        }
        return MotivoDetencion.SOLICITADO_POR_USUARIO;
    }

    private void esperar(Duration duracion) throws InterruptedException {
        long limite = System.nanoTime() + duracion.toNanos();
        boolean reanudarEspera = false;
        while (!detencionSolicitada) {
            long restante = limite - System.nanoTime();
            if (restante <= 0) {
                return;
            }
            pausa.esperar(reanudarEspera ? Duration.ofNanos(restante) : duracion);
            if (detencionSolicitada) {
                return;
            }
            if (!haySolicitudesEnfoque()) {
                return;
            }
            procesarSolicitudesEnfoque(false);
            reanudarEspera = true;
        }
    }

    private boolean haySolicitudesEnfoque() {
        synchronized (bloqueoSolicitudes) {
            return !solicitudesEnfoque.isEmpty();
        }
    }

    private void procesarSolicitudesEnfoque(boolean finalizar) {
        List<CompletableFuture<Void>> solicitudes;
        synchronized (bloqueoSolicitudes) {
            if (finalizar) {
                finalizado = true;
            }
            solicitudes = new ArrayList<>(solicitudesEnfoque);
            solicitudesEnfoque.clear();
        }
        for (CompletableFuture<Void> solicitud : solicitudes) {
            try {
                if (!portal.estaAbierto()) {
                    throw new SesionPortalCerradaException(
                            "La sesión del navegador ya se cerró. Abre una nueva sesión para este cliente.");
                }
                portal.enfocarVentana();
                solicitud.complete(null);
            } catch (RuntimeException e) {
                solicitud.completeExceptionally(e);
            }
        }
    }

    private static MotivoDetencion motivoDe(ResultadoCiclo resultado) {
        return switch (resultado) {
            case ResultadoCiclo.ArmaApartada a -> MotivoDetencion.ARMA_APARTADA;
            case ResultadoCiclo.ArmaEncontrada e -> MotivoDetencion.ARMA_ENCONTRADA;
            case ResultadoCiclo.SesionVencida s -> MotivoDetencion.SESION_VENCIDA;
            case ResultadoCiclo.SolicitudNoEncontrada n -> MotivoDetencion.SOLICITUD_NO_ENCONTRADA;
            case ResultadoCiclo.SinArmaDeInteres s -> MotivoDetencion.INVENTARIO_SIN_INTERES;
            case ResultadoCiclo.ApartadoFallido f -> MotivoDetencion.APARTADO_FALLIDO;
            default -> throw new IllegalStateException("Resultado sin motivo de detencion: " + resultado);
        };
    }
}
