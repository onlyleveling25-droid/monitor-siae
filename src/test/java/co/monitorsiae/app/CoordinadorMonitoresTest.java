package co.monitorsiae.app;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.ArmaInventario;
import co.monitorsiae.dominio.ModoReaccion;
import co.monitorsiae.dominio.Modalidad;
import co.monitorsiae.dominio.ParametrosTramite;
import co.monitorsiae.dominio.PoliticaSinObjetivo;
import co.monitorsiae.dominio.ReglasSeleccion;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.notificacion.Alerta;
import co.monitorsiae.portal.PortalDriver;
import co.monitorsiae.portal.ResultadoGuardar;
import co.monitorsiae.portal.SesionPortalCerradaException;
import co.monitorsiae.portal.simulado.Escena;
import co.monitorsiae.portal.simulado.FabricaPortalSimulada;
import co.monitorsiae.portal.simulado.PortalSimulado;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinadorMonitoresTest {

    @Test
    void cerrarDosVecesLiberaLaSesionSinReenviarTareasAlTrabajadorCerrado() throws Exception {
        Cliente cliente = Cliente.nuevo("Prueba de cierre");
        var portal = new PortalDePrueba();
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        var coordinador = new CoordinadorMonitores(
                Configuracion.porDefecto().conEntornoPortal(EntornoPortal.PORTAL_REAL),
                new CentralAlertas(List.of()), perfil -> portal,
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);
            coordinador.close();
            coordinador.close();
            assertTrue(portal.liberado.await(2, TimeUnit.SECONDS));
            assertEquals(1, portal.cierres.get());
        } finally {
            coordinador.close();
        }
    }

    @Test
    void elModoRealExigeAbrirSesionAntesDelMonitoreo() throws InterruptedException {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto()
                .conEntornoPortal(EntornoPortal.PORTAL_REAL)
                .conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> PortalSimulado.con(Escena.abiertasSinInventario(
                        perfil.tramite().seccionales().getFirst())),
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            assertThrows(IllegalStateException.class, () -> coordinador.iniciar(cliente));
            coordinador.abrirSesion(cliente);

            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);

            assertEquals("Navegador abierto. Inicia sesión y resuelve el captcha manualmente.",
                    coordinador.ultimoResultado(cliente.id()));
        } finally {
            coordinador.close();
        }
    }

    @Test
    void enfocaLaVentanaEnElHiloPropietarioDeLaSesion() throws Exception {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto()
                .conEntornoPortal(EntornoPortal.PORTAL_REAL)
                .conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        AtomicReference<PortalDePrueba> portal = new AtomicReference<>();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> {
                    PortalDePrueba nuevo = new PortalDePrueba();
                    portal.set(nuevo);
                    return nuevo;
                },
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);

            coordinador.enfocarVentana(cliente.id()).get(2, TimeUnit.SECONDS);

            assertEquals(1, portal.get().enfoques.get());
            assertEquals("monitor-cliente-" + cliente.id(), portal.get().hiloEnfoque.get());
        } finally {
            coordinador.close();
        }
    }

    @Test
    void enfocaLaVentanaMientrasMonitoreaSinEsperarElSiguienteCiclo() throws Exception {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto()
                .conEntornoPortal(EntornoPortal.PORTAL_REAL)
                .conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        AtomicReference<PortalEnfoqueContinuo> portal = new AtomicReference<>();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> {
                    PortalEnfoqueContinuo nuevo = new PortalEnfoqueContinuo();
                    portal.set(nuevo);
                    return nuevo;
                },
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);
            coordinador.iniciar(cliente);

            assertTrue(portal.get().consultaIniciada.await(2, TimeUnit.SECONDS));
            coordinador.enfocarVentana(cliente.id()).get(2, TimeUnit.SECONDS);

            assertEquals(1, portal.get().enfoques.get());
            assertEquals("monitor-cliente-" + cliente.id(), portal.get().hiloEnfoque.get());
        } finally {
            coordinador.close();
        }
    }

    @Test
    void enfocarSinSesionInformaQueDebeAbrirseElNavegador() {
        CoordinadorMonitores coordinador = new CoordinadorMonitores(Configuracion.porDefecto(),
                new CentralAlertas(List.of()), cliente -> new PortalDePrueba(), (id, estado) -> { });

        try {
            var error = org.junit.jupiter.api.Assertions.assertThrows(
                    java.util.concurrent.CompletionException.class,
                    () -> coordinador.enfocarVentana("sin-sesion").join());

            assertTrue(error.getCause().getMessage().contains("no tiene una sesión de navegador abierta"));
        } finally {
            coordinador.close();
        }
    }

    @Test
    void elSimuladorConectaInventarioConLaAlertaDeOportunidad() throws InterruptedException {
        Cliente base = ClientesDePrueba.valido("Ana");
        Cliente cliente = new Cliente(base.id(), base.nombre(), true, base.tramite(), ModoReaccion.ALERTA,
                new ReglasSeleccion(base.seleccion().modalidad(), base.seleccion().objetivos(),
                        PoliticaSinObjetivo.USAR_ALTERNATIVAS_LUEGO_TIPO,
                        base.seleccion().alternativas(), "Pistola", base.seleccion().excluir(),
                        base.seleccion().reintentos()));
        AtomicReference<Alerta> alertaRecibida = new AtomicReference<>();
        CentralAlertas alertas = new CentralAlertas(List.of(activas -> {
            if (!activas.isEmpty()) {
                alertaRecibida.set(activas.getFirst());
            }
        }));
        CoordinadorMonitores coordinador = new CoordinadorMonitores(
                Configuracion.porDefecto().conClientes(List.of(cliente)),
                alertas,
                new FabricaPortalSimulada(),
                (id, estado) -> {
                });
        try {
            coordinador.iniciar(cliente);
            long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            while (alertaRecibida.get() == null && System.nanoTime() < limite) {
                Thread.sleep(10);
            }

            assertNotNull(alertaRecibida.get(), "El simulador debe producir una alerta de arma disponible.");
            assertEquals("Arma disponible: Ana", alertaRecibida.get().titulo());
            esperarEstado(coordinador, cliente.id(), CoordinadorMonitores.Estado.ALERTA_ENCONTRADA);
            var snapshot = coordinador.ultimoSnapshot(cliente.id()).orElseThrow();
            assertEquals(2, snapshot.inventario().size());
            assertEquals(0, snapshot.eleccion().arma().posicion());
        } finally {
            coordinador.close();
        }
    }

    @Test
    void detieneYReanudaElMonitorEnElTrabajadorDelCliente() throws InterruptedException {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto().conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> PortalSimulado.con(Escena.abiertasSinInventario(
                        perfil.tramite().seccionales().getFirst())),
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.iniciar(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.MONITOREANDO);
            esperarResultado(coordinador, cliente.id());
            coordinador.detener(cliente.id());
            esperar(cambios, CoordinadorMonitores.Estado.DETENIDO);

            coordinador.iniciar(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.MONITOREANDO);
            coordinador.detener(cliente.id());
            esperar(cambios, CoordinadorMonitores.Estado.DETENIDO);

            assertEquals("Sin inventario en Medellín.", coordinador.ultimoResultado(cliente.id()));
        } finally {
            coordinador.close();
        }
    }

    @Test
    void detenerConservaLaSesionAbiertaYReanudarReutilizaElMismoPortal() throws InterruptedException {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto()
                .conEntornoPortal(EntornoPortal.PORTAL_REAL)
                .conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        AtomicInteger portalesCreados = new AtomicInteger();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> {
                    portalesCreados.incrementAndGet();
                    return PortalSimulado.con(Escena.abiertasSinInventario(
                            perfil.tramite().seccionales().getFirst()));
                },
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);
            assertTrue(coordinador.sesionAbierta(cliente.id()));

            coordinador.detener(cliente.id());
            esperar(cambios, CoordinadorMonitores.Estado.DETENIDO);
            assertTrue(coordinador.sesionAbierta(cliente.id()),
                    "Detener el monitoreo no debe cerrar la sesión del portal.");

            coordinador.iniciar(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.MONITOREANDO);
            coordinador.detener(cliente.id());
            esperar(cambios, CoordinadorMonitores.Estado.DETENIDO);

            coordinador.iniciar(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.MONITOREANDO);
            assertEquals(1, portalesCreados.get(),
                    "Reanudar debe continuar con el mismo PortalDriver.");
            coordinador.detener(cliente.id());
            esperar(cambios, CoordinadorMonitores.Estado.DETENIDO);
        } finally {
            coordinador.close();
        }
    }

    @Test
    void reconoceCuandoSeCierraElNavegadorYPermiteAbrirUnaNuevaSesion() throws InterruptedException {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto()
                .conEntornoPortal(EntornoPortal.PORTAL_REAL)
                .conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        AtomicInteger portalesCreados = new AtomicInteger();
        AtomicReference<PortalDePrueba> portalActual = new AtomicReference<>();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> {
                    portalesCreados.incrementAndGet();
                    PortalDePrueba portal = new PortalDePrueba();
                    portalActual.set(portal);
                    return portal;
                },
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);
            portalActual.get().cerrarManualmente();

            esperar(cambios, CoordinadorMonitores.Estado.SESION_CERRADA);
            assertTrue(!coordinador.sesionAbierta(cliente.id()),
                    "La referencia a una sesión cerrada debe liberarse.");

            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);
            assertEquals(2, portalesCreados.get(),
                    "Abrir sesión debe crear un nuevo portal después del cierre manual.");
        } finally {
            coordinador.close();
        }
    }

    @Test
    void cerrarElNavegadorDuranteElMonitoreoTerminaSinErrorGenerico() throws InterruptedException {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        Configuracion configuracion = Configuracion.porDefecto()
                .conEntornoPortal(EntornoPortal.PORTAL_REAL)
                .conClientes(List.of(cliente));
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        AtomicReference<PortalDePrueba> portalActual = new AtomicReference<>();
        CoordinadorMonitores coordinador = new CoordinadorMonitores(configuracion,
                new CentralAlertas(List.of()),
                perfil -> {
                    PortalDePrueba portal = new PortalDePrueba();
                    portalActual.set(portal);
                    return portal;
                },
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.abrirSesion(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.SESION_ABIERTA);
            coordinador.iniciar(cliente);
            esperar(cambios, CoordinadorMonitores.Estado.MONITOREANDO);
            portalActual.get().cerrarManualmente();

            esperar(cambios, CoordinadorMonitores.Estado.SESION_CERRADA);
            assertEquals("El navegador se cerró. Abre una nueva sesión para volver a conectar este cliente.",
                    coordinador.ultimoResultado(cliente.id()));
            assertTrue(!coordinador.sesionAbierta(cliente.id()));
        } finally {
            coordinador.close();
        }
    }

    @Test
    void unaDetencionDuranteElArranqueNoPierdeLaSolicitud() throws InterruptedException {
        Cliente cliente = ClientesDePrueba.valido("Ana");
        BlockingQueue<Actualizacion> cambios = new LinkedBlockingQueue<>();
        CountDownLatch fabricaIniciada = new CountDownLatch(1);
        CountDownLatch continuarFabrica = new CountDownLatch(1);
        CoordinadorMonitores coordinador = new CoordinadorMonitores(
                Configuracion.porDefecto().conClientes(List.of(cliente)),
                new CentralAlertas(List.of()),
                perfil -> {
                    fabricaIniciada.countDown();
                    try {
                        continuarFabrica.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Se interrumpió la creación del portal.", e);
                    }
                    return PortalSimulado.con(Escena.abiertasSinInventario(
                            perfil.tramite().seccionales().getFirst()));
                },
                (id, estado) -> cambios.add(new Actualizacion(id, estado)));
        try {
            coordinador.iniciar(cliente);
            assertTrue(fabricaIniciada.await(1, TimeUnit.SECONDS));
            coordinador.detener(cliente.id());
            continuarFabrica.countDown();
            esperar(cambios, CoordinadorMonitores.Estado.DETENIDO);
        } finally {
            continuarFabrica.countDown();
            coordinador.close();
        }
    }

    private static void esperar(BlockingQueue<Actualizacion> cambios, CoordinadorMonitores.Estado estado)
            throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < limite) {
            Actualizacion actualizacion = cambios.poll(100, TimeUnit.MILLISECONDS);
            if (actualizacion != null && actualizacion.estado() == estado) {
                assertNotNull(actualizacion.clienteId());
                return;
            }
        }
        throw new AssertionError("No se recibió el estado " + estado + " dentro del plazo.");
    }

    private static void esperarResultado(CoordinadorMonitores coordinador, String clienteId)
            throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < limite) {
            String resultado = coordinador.ultimoResultado(clienteId);
            if (!resultado.isBlank() && !resultado.startsWith("Iniciando")
                    && !resultado.equals("Consultando el inventario.")) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("El monitor no publicó el primer resultado dentro del plazo.");
    }

    private static void esperarEstado(CoordinadorMonitores coordinador, String clienteId,
                                      CoordinadorMonitores.Estado estado) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < limite) {
            if (coordinador.estado(clienteId) == estado) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("El monitor no llegó al estado " + estado + ".");
    }

    private record Actualizacion(String clienteId, CoordinadorMonitores.Estado estado) {
    }

    private static final class PortalDePrueba implements PortalDriver {

        private final CountDownLatch liberado = new CountDownLatch(1);
        private final AtomicInteger cierres = new AtomicInteger();
        private final AtomicBoolean abierto = new AtomicBoolean(true);
        private final AtomicInteger enfoques = new AtomicInteger();
        private final AtomicReference<String> hiloEnfoque = new AtomicReference<>();
        private volatile Runnable accionAlCerrar = () -> { };

        @Override
        public List<String> abrirFormulario(ParametrosTramite tramite) {
            while (estaAbierto()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Se interrumpió la consulta de prueba.", e);
                }
            }
            throw new SesionPortalCerradaException("El navegador se cerró.");
        }

        @Override
        public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
            return ResultadoGuardar.SIN_INVENTARIO;
        }

        @Override
        public List<ArmaInventario> leerInventario() {
            return List.of();
        }

        @Override
        public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
            return false;
        }

        @Override
        public boolean estaAbierto() {
            return abierto.get();
        }

        @Override
        public void enfocarVentana() {
            enfoques.incrementAndGet();
            hiloEnfoque.set(Thread.currentThread().getName());
        }

        @Override
        public void alCerrar(Runnable accion) {
            accionAlCerrar = accion;
            if (!abierto.get()) {
                accion.run();
            }
        }

        private void cerrarManualmente() {
            if (abierto.compareAndSet(true, false)) {
                accionAlCerrar.run();
            }
        }

        @Override
        public void close() {
            cierres.incrementAndGet();
            cerrarManualmente();
            liberado.countDown();
        }
    }

    private static final class PortalEnfoqueContinuo implements PortalDriver {

        private final AtomicBoolean abierto = new AtomicBoolean(true);
        private final AtomicInteger enfoques = new AtomicInteger();
        private final AtomicReference<String> hiloEnfoque = new AtomicReference<>();
        private final CountDownLatch consultaIniciada = new CountDownLatch(1);

        @Override
        public List<String> abrirFormulario(ParametrosTramite tramite) {
            consultaIniciada.countDown();
            return List.of();
        }

        @Override
        public ResultadoGuardar enviarPaso1(String seccional, String descripcion) {
            return ResultadoGuardar.SIN_INVENTARIO;
        }

        @Override
        public List<ArmaInventario> leerInventario() {
            return List.of();
        }

        @Override
        public boolean apartar(ArmaInventario arma, Modalidad modalidad) {
            return false;
        }

        @Override
        public boolean estaAbierto() {
            return abierto.get();
        }

        @Override
        public void enfocarVentana() {
            enfoques.incrementAndGet();
            hiloEnfoque.set(Thread.currentThread().getName());
        }

        @Override
        public void close() {
            abierto.set(false);
        }
    }
}
