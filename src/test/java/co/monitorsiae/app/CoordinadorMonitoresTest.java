package co.monitorsiae.app;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.ClientesDePrueba;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.notificacion.CentralAlertas;
import co.monitorsiae.portal.simulado.Escena;
import co.monitorsiae.portal.simulado.PortalSimulado;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinadorMonitoresTest {

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
        long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
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
            if (!coordinador.ultimoResultado(clienteId).isBlank()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("El monitor no publicó el primer resultado dentro del plazo.");
    }

    private record Actualizacion(String clienteId, CoordinadorMonitores.Estado estado) {
    }
}
