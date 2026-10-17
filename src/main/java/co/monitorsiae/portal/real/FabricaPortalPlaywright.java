package co.monitorsiae.portal.real;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.NavegadorPreferido;
import co.monitorsiae.persistencia.UbicacionDatos;
import co.monitorsiae.portal.PortalDriver;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricaPortalPlaywright implements Function<Cliente, PortalDriver> {

    private final UbicacionDatos ubicacion;
    private final Supplier<Configuracion> configuracion;

    public FabricaPortalPlaywright(UbicacionDatos ubicacion, Supplier<Configuracion> configuracion) {
        this.ubicacion = Objects.requireNonNull(ubicacion);
        this.configuracion = Objects.requireNonNull(configuracion);
    }

    @Override
    public PortalDriver apply(Cliente cliente) {
        Path raizPerfiles = ubicacion.perfilesNavegador().toAbsolutePath().normalize();
        Path perfil = raizPerfiles.resolve(cliente.id()).normalize();
        if (!perfil.startsWith(raizPerfiles)) {
            throw new IllegalArgumentException("El identificador del cliente no es válido para su perfil de navegador.");
        }
        NavegadorPreferido navegador = configuracion.get().navegador();
        return new PlaywrightPortalDriver(perfil, navegador);
    }
}
