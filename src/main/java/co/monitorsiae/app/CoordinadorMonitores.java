package co.monitorsiae.app;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.monitoreo.CicloMonitoreo;
import co.monitorsiae.monitoreo.MonitorCliente;
import co.monitorsiae.monitoreo.OyenteMonitoreo;
import co.monitorsiae.monitoreo.OyenteRegistro;
import co.monitorsiae.monitoreo.Pausa;
import co.monitorsiae.monitoreo.ResultadoCiclo;
import co.monitorsiae.monitoreo.SelectorArma;
import co.monitorsiae.monitoreo.SelectorSeccional;
import co.monitorsiae.monitoreo.SnapshotInventario;
import co.monitorsiae.monitoreo.VentanaHoraria;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.OyenteAlertas;
import co.monitorsiae.notificacion.TraductorAlertas;
import co.monitorsiae.persistencia.RepositorioSnapshots;
import co.monitorsiae.persistencia.RepositorioSnapshotsMemoria;
import co.monitorsiae.portal.PortalDriver;

/**
 * Mantiene un hilo propietario por cliente y ejecuta en el todos los accesos a
 * su PortalDriver.
 */
public final class CoordinadorMonitores implements AutoCloseable {

    public enum Estado {
        LISTO,
        ABRIENDO_SESION,
        SESION_ABIERTA,
        SESION_CERRADA,
        INICIANDO,
        MONITOREANDO,
        FUERA_DE_HORARIO,
        ALERTA_ENCONTRADA,
        ARMA_APARTADA,
        DETENIENDO,
        DETENIDO,
        ERROR
    }

    private final Map<String, SesionCliente> sesiones = new ConcurrentHashMap<>();
    private final BiConsumer<String, Estado> alCambiarEstado;
    private final CentralAlertas alertas;
    private final Function<Cliente, PortalDriver> fabricaPortal;
    private final RepositorioSnapshots snapshots;
    private volatile Configuracion configuracion;
    private volatile boolean cerrado;

    public CoordinadorMonitores(Configuracion configuracion, CentralAlertas alertas,
            Function<Cliente, PortalDriver> fabricaPortal,
            BiConsumer<String, Estado> alCambiarEstado) {
        this(configuracion, alertas, fabricaPortal, alCambiarEstado, new RepositorioSnapshotsMemoria());
    }

    public CoordinadorMonitores(Configuracion configuracion, CentralAlertas alertas,
            Function<Cliente, PortalDriver> fabricaPortal,
            BiConsumer<String, Estado> alCambiarEstado,
            RepositorioSnapshots snapshots) {
        this.configuracion = Objects.requireNonNull(configuracion);
        this.alertas = Objects.requireNonNull(alertas);
        this.fabricaPortal = Objects.requireNonNull(fabricaPortal);
        this.alCambiarEstado = Objects.requireNonNull(alCambiarEstado);
        this.snapshots = Objects.requireNonNull(snapshots);
    }

    public synchronized void actualizarConfiguracion(Configuracion nueva) {
        this.configuracion = nueva;
    }

    public Estado estado(String clienteId) {
        SesionCliente sesion = sesiones.get(clienteId);
        return sesion == null ? Estado.LISTO : sesion.estado;
    }

    public String ultimoResultado(String clienteId) {
        SesionCliente sesion = sesiones.get(clienteId);
        return sesion == null ? "" : sesion.ultimoResultado;
    }

    public boolean sesionAbierta(String clienteId) {
        SesionCliente sesion = sesiones.get(clienteId);
        return sesion != null && sesion.portal != null;
    }

    public Optional<SnapshotInventario> ultimoSnapshot(String clienteId) {
        return snapshots.ultimoDe(clienteId);
    }

    public List<SnapshotInventario> historialSnapshots(String clienteId) {
        return snapshots.historialDe(clienteId);
    }

    public void iniciar(Cliente cliente) {
        if (cerrado) {
            throw new IllegalStateException("La aplicación se está cerrando.");
        }

        if (configuracion.entornoPortal() == EntornoPortal.PORTAL_REAL && !sesionAbierta(cliente.id())) {
            throw new IllegalStateException("Abre la sesión de " + cliente.nombre()
                    + " e inicia sesión en la plataforma antes de monitorear.");
        }

        SesionCliente sesion = sesionCliente(cliente.id());
        synchronized (sesion) {
            if (sesion.estado == Estado.MONITOREANDO || sesion.estado == Estado.INICIANDO
                    || sesion.estado == Estado.DETENIENDO) {
                return;
            }
            sesion.estado = Estado.INICIANDO;
            sesion.ultimoResultado = "Iniciando y preparando una nueva consulta.";
            notificar(sesion.id, Estado.INICIANDO);
            sesion.trabajador.submit(() -> ejecutar(cliente, sesion));
        }
    }

    public void abrirSesion(Cliente cliente) {
        if (cerrado) {
            throw new IllegalStateException("La aplicación se está cerrando.");
        }
        if (configuracion.entornoPortal() != EntornoPortal.PORTAL_REAL) {
            throw new IllegalStateException("Selecciona Portal real en Configuración para abrir la sesión.");
        }
        SesionCliente sesion = sesionCliente(cliente.id());
        synchronized (sesion) {
            if (sesion.portal != null) {
                sesion.ultimoResultado = "La sesión de este cliente ya está abierta.";
                notificar(sesion.id, sesion.estado);
                return;
            }
            if (sesion.estado == Estado.ABRIENDO_SESION || sesion.estado == Estado.INICIANDO
                    || sesion.estado == Estado.MONITOREANDO || sesion.estado == Estado.DETENIENDO) {
                return;
            }
            sesion.estado = Estado.ABRIENDO_SESION;
            sesion.ultimoResultado = "Abriendo el navegador del cliente.";
            notificar(sesion.id, Estado.ABRIENDO_SESION);
            sesion.trabajador.submit(() -> {
                try {
                    obtenerPortal(cliente, sesion);
                    if (!sesion.portal.estaAbierto()) {
                        throw new IllegalStateException("El navegador se cerró durante la apertura.");
                    }
                    sesion.estado = Estado.SESION_ABIERTA;
                    sesion.ultimoResultado = "Navegador abierto. Inicia sesión y resuelve el captcha manualmente.";
                    notificar(sesion.id, Estado.SESION_ABIERTA);
                } catch (RuntimeException e) {
                    boolean navegadorCerrado = sesion.portal != null && !portalSigueAbierto(sesion.portal);
                    if (navegadorCerrado) {
                        LOG.info("El navegador del cliente {} se cerró durante la apertura.", cliente.nombre());
                        cerrarPortal(sesion.portal, sesion.id);
                        sesion.portal = null;
                        sesion.entornoPortal = null;
                        sesion.estado = Estado.SESION_CERRADA;
                        sesion.ultimoResultado = mensajeSesionCerrada();
                    } else {
                        LOG.error("No se pudo abrir la sesión del cliente {}", cliente.nombre(), e);
                        sesion.estado = Estado.ERROR;
                        sesion.ultimoResultado = mensajeError(e, "No se pudo abrir el navegador.");
                    }
                    notificar(sesion.id, sesion.estado);
                }
            });
        }
    }

    public void detener(String clienteId) {
        SesionCliente sesion = sesiones.get(clienteId);
        if (sesion == null) {
            return;
        }
        synchronized (sesion) {
            if (sesion.estado == Estado.INICIANDO) {
                sesion.detencionSolicitada = true;
                sesion.estado = Estado.DETENIENDO;
                notificar(sesion.id, Estado.DETENIENDO);
                if (sesion.monitor != null) {
                    sesion.monitor.detener();
                }
            } else if (sesion.monitor != null && (sesion.estado == Estado.MONITOREANDO
                    || sesion.estado == Estado.FUERA_DE_HORARIO)) {
                sesion.estado = Estado.DETENIENDO;
                notificar(sesion.id, Estado.DETENIENDO);
                sesion.monitor.detener();
            } else if (sesion.monitor == null && sesion.portal != null) {
                sesion.estado = Estado.DETENIDO;
                sesion.ultimoResultado = "Monitoreo detenido. La sesión del navegador permanece abierta.";
                notificar(sesion.id, Estado.DETENIDO);
            }
        }
    }

    private void ejecutar(Cliente cliente, SesionCliente sesion) {
        try {
            obtenerPortal(cliente, sesion);
            if (sesion.detencionSolicitada) {
                synchronized (sesion) {
                    sesion.detencionSolicitada = false;
                    sesion.estado = Estado.DETENIDO;
                    sesion.ultimoResultado =
                            "Monitoreo detenido. La sesión del navegador permanece abierta.";
                }
                notificar(sesion.id, Estado.DETENIDO);
                return;
            }
            Configuracion actual = configuracion;
            VentanaHoraria ventana = new VentanaHoraria(actual.horario(), Clock.systemUTC());
            OyenteMonitoreo oyente = new OyenteMonitoreo() {
                private final OyenteRegistro registro = new OyenteRegistro();
                private final OyenteAlertas oyenteAlertas = new OyenteAlertas(new TraductorAlertas(), alertas);

                @Override
                public void alIniciar(Cliente iniciado) {
                    synchronized (sesion) {
                        sesion.estado = sesion.detencionSolicitada ? Estado.DETENIENDO : Estado.MONITOREANDO;
                        sesion.ultimoResultado = "Consultando el inventario.";
                    }
                    notificar(sesion.id, sesion.estado);
                    registro.alIniciar(iniciado);
                    oyenteAlertas.alIniciar(iniciado);
                }

                @Override
                public void fueraDeHorario(Cliente iniciado) {
                    sesion.estado = Estado.FUERA_DE_HORARIO;
                    sesion.ultimoResultado = ventana.motivoFueraDeHorario();
                    notificar(sesion.id, Estado.FUERA_DE_HORARIO);
                    registro.fueraDeHorario(iniciado);
                    oyenteAlertas.fueraDeHorario(iniciado);
                }

                @Override
                public void dentroDeHorario(Cliente iniciado) {
                    sesion.estado = Estado.MONITOREANDO;
                    sesion.ultimoResultado = "Horario habilitado; consultando el inventario.";
                    notificar(sesion.id, Estado.MONITOREANDO);
                }

                @Override
                public void alTerminarCiclo(Cliente iniciado, ResultadoCiclo resultado) {
                    sesion.ultimoResultado = resultado.descripcion();
                    if (resultado instanceof ResultadoCiclo.ArmaEncontrada encontrada) {
                        snapshots.guardar(new SnapshotInventario(Instant.now(), iniciado.id(), iniciado.nombre(),
                                encontrada.seccional(), encontrada.inventario(), encontrada.eleccion()));
                        sesion.estado = Estado.ALERTA_ENCONTRADA;
                    } else if (resultado instanceof ResultadoCiclo.ArmaApartada apartada) {
                        snapshots.guardar(new SnapshotInventario(Instant.now(), iniciado.id(), iniciado.nombre(),
                                apartada.seccional(), apartada.inventario(), apartada.eleccion()));
                        sesion.estado = Estado.ARMA_APARTADA;
                    } else if (resultado instanceof ResultadoCiclo.ApartadoFallido fallo) {
                        snapshots.guardar(new SnapshotInventario(Instant.now(), iniciado.id(), iniciado.nombre(),
                                fallo.seccional(), fallo.inventario(), fallo.eleccion()));
                    }
                    notificar(sesion.id, sesion.estado);
                    registro.alTerminarCiclo(iniciado, resultado);
                    oyenteAlertas.alTerminarCiclo(iniciado, resultado);
                }

                @Override
                public void alDetenerse(Cliente iniciado, co.monitorsiae.monitoreo.MotivoDetencion motivo) {
                    registro.alDetenerse(iniciado, motivo);
                    oyenteAlertas.alDetenerse(iniciado, motivo);
                }
            };
            sesion.monitor = new MonitorCliente(cliente, sesion.portal,
                    new CicloMonitoreo(new SelectorSeccional(), new SelectorArma()),
                    ventana,
                    Duration.ofSeconds(actual.intervaloConsultaSegundos()), Pausa.real(), oyente);
            if (sesion.detencionSolicitada) {
                sesion.monitor.detener();
            }
            sesion.monitor.run();
        } catch (RuntimeException e) {
            boolean navegadorCerrado = sesion.portal == null || !portalSigueAbierto(sesion.portal);
            if (navegadorCerrado) {
                LOG.info("La sesión del navegador del cliente {} se cerró.", cliente.nombre());
                if (sesion.portal != null) {
                    cerrarPortal(sesion.portal, sesion.id);
                }
            } else {
                LOG.error("No se pudo ejecutar el monitor del cliente {}", cliente.nombre(), e);
            }
            synchronized (sesion) {
                sesion.monitor = null;
                sesion.detencionSolicitada = false;
                if (navegadorCerrado) {
                    sesion.portal = null;
                    sesion.entornoPortal = null;
                    sesion.estado = Estado.SESION_CERRADA;
                    sesion.ultimoResultado = mensajeSesionCerrada();
                } else {
                    sesion.estado = Estado.ERROR;
                    sesion.ultimoResultado = mensajeError(e, "Error durante el monitoreo.");
                }
            }
            notificar(sesion.id, sesion.estado);
            return;
        }
        synchronized (sesion) {
            sesion.monitor = null;
            boolean detenidoPorUsuario = sesion.detencionSolicitada;
            sesion.detencionSolicitada = false;
            if (sesion.estado != Estado.ERROR && sesion.estado != Estado.ALERTA_ENCONTRADA
                    && sesion.estado != Estado.ARMA_APARTADA) {
                sesion.estado = Estado.DETENIDO;
                if (detenidoPorUsuario) {
                    sesion.ultimoResultado =
                            "Monitoreo detenido. La sesión del navegador permanece abierta.";
                }
            }
        }
        if (sesion.estado != Estado.ERROR) {
            notificar(sesion.id, sesion.estado);
        }
    }

    private PortalDriver obtenerPortal(Cliente cliente, SesionCliente sesion) {
        EntornoPortal solicitado = configuracion.entornoPortal();
        if (sesion.portal != null && sesion.entornoPortal != solicitado) {
            sesion.portal.close();
            sesion.portal = null;
        }
        if (sesion.portal == null) {
            sesion.portal = Objects.requireNonNull(fabricaPortal.apply(cliente),
                    "La fábrica no puede devolver un PortalDriver nulo.");
            sesion.entornoPortal = solicitado;
        }
        return sesion.portal;
    }

    private SesionCliente sesionCliente(String id) {
        SesionCliente sesion = sesiones.computeIfAbsent(id, SesionCliente::new);
        synchronized (sesion) {
            if (sesion.vigilancia == null && !cerrado) {
                sesion.vigilancia = sesion.trabajador.scheduleWithFixedDelay(
                        () -> revisarSesion(sesion), 1, 1, TimeUnit.SECONDS);
            }
        }
        return sesion;
    }

    private void revisarSesion(SesionCliente sesion) {
        PortalDriver portal = sesion.portal;
        if (cerrado || portal == null || sesion.monitor != null) {
            return;
        }
        try {
            if (portal.estaAbierto()) {
                return;
            }
        } catch (RuntimeException e) {
            LOG.error("No se pudo comprobar la sesión del cliente {}", sesion.id, e);
            cerrarPortal(portal, sesion.id);
            synchronized (sesion) {
                sesion.portal = null;
                sesion.entornoPortal = null;
                sesion.estado = Estado.ERROR;
                sesion.ultimoResultado = mensajeError(e, "No se pudo comprobar el estado del navegador.");
            }
            notificar(sesion.id, Estado.ERROR);
            return;
        }
        cerrarPortal(portal, sesion.id);
        synchronized (sesion) {
            if (sesion.portal != portal || sesion.monitor != null) {
                return;
            }
            sesion.portal = null;
            sesion.entornoPortal = null;
            sesion.estado = Estado.SESION_CERRADA;
            sesion.ultimoResultado = mensajeSesionCerrada();
        }
        LOG.info("El navegador del cliente {} se cerró; se puede abrir una nueva sesión.", sesion.id);
        notificar(sesion.id, Estado.SESION_CERRADA);
    }

    private static boolean portalSigueAbierto(PortalDriver portal) {
        try {
            return portal.estaAbierto();
        } catch (RuntimeException e) {
            LOG.error("No se pudo determinar si el navegador sigue abierto.", e);
            return true;
        }
    }

    private static String mensajeSesionCerrada() {
        return "El navegador se cerró. Abre una nueva sesión para volver a conectar este cliente.";
    }

    private static String mensajeError(RuntimeException error, String respaldo) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? respaldo : respaldo + " " + error.getMessage();
    }

    private static void cerrarPortal(PortalDriver portal, String clienteId) {
        try {
            portal.close();
        } catch (RuntimeException e) {
            LOG.warn("No se pudo liberar el navegador cerrado del cliente {}", clienteId, e);
        }
    }

    private void notificar(String id, Estado estado) {
        alCambiarEstado.accept(id, estado);
    }

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(CoordinadorMonitores.class);

    @Override
    public void close() {
        cerrado = true;
        sesiones.values().forEach(sesion -> detener(sesion.id));
        sesiones.values().forEach(sesion -> {
            sesion.trabajador.submit(() -> {
                if (sesion.portal != null) {
                    try {
                        sesion.portal.close();
                        sesion.portal = null;
                    } catch (RuntimeException e) {
                        org.slf4j.LoggerFactory.getLogger(CoordinadorMonitores.class)
                                .error("No se pudo cerrar el navegador del cliente {}", sesion.id, e);
                    }
                }
            });
            sesion.trabajador.shutdown();
        });
    }

    private static final class SesionCliente {

        private final String id;
        private final ScheduledExecutorService trabajador;
        private java.util.concurrent.ScheduledFuture<?> vigilancia;
        private volatile Estado estado = Estado.LISTO;
        private volatile PortalDriver portal;
        private volatile EntornoPortal entornoPortal;
        private volatile MonitorCliente monitor;
        private volatile boolean detencionSolicitada;
        private volatile String ultimoResultado = "";

        private SesionCliente(String id) {
            this.id = id;
            this.trabajador = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread hilo = new Thread(runnable, "monitor-cliente-" + id);
                hilo.setDaemon(true);
                return hilo;
            });
        }
    }
}
