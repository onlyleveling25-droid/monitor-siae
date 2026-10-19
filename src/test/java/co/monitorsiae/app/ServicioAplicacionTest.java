package co.monitorsiae.app;

import co.monitorsiae.acceso.CifradorPbkdf2;
import co.monitorsiae.acceso.ContrasenaRechazadaException;
import co.monitorsiae.acceso.ServicioAcceso;
import co.monitorsiae.dominio.AccesoAplicacion;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import co.monitorsiae.dominio.ValidadorConfiguracion;
import co.monitorsiae.persistencia.RepositorioConfiguracion;
import co.monitorsiae.persistencia.RepositorioConfiguracionJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioAplicacionTest {

    @TempDir
    Path carpeta;

    private final CifradorPbkdf2 cifrador = new CifradorPbkdf2(1_000);

    @Test
    void preparaLaProteccionSinPersistirYGuardaTodoConUnSoloRespaldo() throws IOException {
        Path archivo = carpeta.resolve("config.json");
        var repositorio = new RepositorioConfiguracionJson(archivo);
        Configuracion original = Configuracion.porDefecto().conAcceso(new AccesoAplicacion(false, ""));
        repositorio.guardar(original);
        var servicio = servicio(repositorio);
        String contenidoOriginal = Files.readString(archivo);

        AccesoAplicacion preparado = servicio.prepararCreacionContrasena(
                "clave123".toCharArray(), "clave123".toCharArray());

        assertEquals(original, servicio.configuracion());
        assertEquals(contenidoOriginal, Files.readString(archivo));
        assertFalse(repositorio.existeRespaldo());
        Configuracion propuesta = original.conAcceso(preparado)
                .conPreferenciasAlertas(new PreferenciasAlertas(false, false, TonoAlarma.DOBLE));
        servicio.guardar(propuesta);

        assertEquals(propuesta, servicio.configuracion());
        assertEquals(propuesta, repositorio.cargar());
        assertEquals(original, repositorio.restaurarRespaldo());
    }

    @Test
    void descartarUnCambioDeContrasenaConservaLaAnterior() {
        var repositorio = repositorioProtegido();
        var servicio = servicio(repositorio);

        AccesoAplicacion preparado = servicio.prepararCambioContrasena(
                "anterior123".toCharArray(), "nueva123".toCharArray(), "nueva123".toCharArray());

        assertTrue(cifrador.coincide("nueva123".toCharArray(), preparado.hash()));
        assertTrue(servicio.verificarContrasena("anterior123".toCharArray()));
        assertFalse(servicio.verificarContrasena("nueva123".toCharArray()));
        assertEquals(repositorio.cargar(), servicio.configuracion());
    }

    @Test
    void prepararDesactivacionExigeLaContrasenaYNoLaAplica() {
        var repositorio = repositorioProtegido();
        var servicio = servicio(repositorio);

        assertThrows(ContrasenaRechazadaException.class,
                () -> servicio.prepararDesactivacionProteccion("incorrecta".toCharArray()));
        AccesoAplicacion preparado = servicio.prepararDesactivacionProteccion("anterior123".toCharArray());

        assertFalse(preparado.requerirContrasena());
        assertTrue(servicio.pideContrasena());
        assertTrue(repositorio.cargar().acceso().requerirContrasena());
    }

    @Test
    void falloDeEscrituraNoAplicaLaContrasenaNiLosDemasCambiosYPermiteReintentar() {
        var repositorio = repositorioProtegido();
        Configuracion original = repositorio.cargar();
        var servicio = servicio(repositorio);
        AccesoAplicacion preparado = servicio.prepararCambioContrasena(
                "anterior123".toCharArray(), "nueva123".toCharArray(), "nueva123".toCharArray());
        Configuracion propuesta = original.conAcceso(preparado)
                .conPreferenciasAlertas(new PreferenciasAlertas(false, false, TonoAlarma.DOBLE));
        repositorio.fallar = true;

        assertThrows(UncheckedIOException.class, () -> servicio.guardar(propuesta));
        assertEquals(original, servicio.configuracion());
        assertEquals(original, repositorio.cargar());
        assertTrue(servicio.verificarContrasena("anterior123".toCharArray()));

        repositorio.fallar = false;
        servicio.guardar(propuesta);
        assertEquals(propuesta, repositorio.cargar());
        assertTrue(servicio.verificarContrasena("nueva123".toCharArray()));
    }

    private ServicioAplicacion servicio(RepositorioConfiguracion repositorio) {
        return new ServicioAplicacion(repositorio, new ServicioAcceso(cifrador), new ValidadorConfiguracion());
    }

    private RepositorioDePrueba repositorioProtegido() {
        return new RepositorioDePrueba(Configuracion.porDefecto().conAcceso(
                new AccesoAplicacion(true, cifrador.derivar("anterior123".toCharArray()))));
    }

    private static final class RepositorioDePrueba implements RepositorioConfiguracion {
        private Configuracion guardada;
        private boolean fallar;

        private RepositorioDePrueba(Configuracion inicial) {
            guardada = inicial;
        }

        @Override
        public Configuracion cargar() {
            return guardada;
        }

        @Override
        public void guardar(Configuracion nueva) {
            if (fallar) {
                throw new UncheckedIOException(new IOException("Disco no disponible"));
            }
            guardada = nueva;
        }

        @Override
        public boolean existeRespaldo() {
            return false;
        }

        @Override
        public Configuracion restaurarRespaldo() {
            throw new UnsupportedOperationException();
        }
    }
}
