package co.monitorsiae.acceso;

import co.monitorsiae.dominio.AccesoAplicacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioAccesoTest {

    private final ServicioAcceso servicio = new ServicioAcceso(new CifradorPbkdf2(1_000));

    @Test
    void primerUsoPideCrearContrasena() {
        AccesoAplicacion inicial = AccesoAplicacion.porDefecto();

        assertTrue(servicio.debeCrearContrasena(inicial));
        assertFalse(servicio.pideContrasena(inicial));
    }

    @Test
    void despuesDeCrearlaLaPideYLaVerifica() {
        AccesoAplicacion acceso = servicio.crear("clave123".toCharArray(), "clave123".toCharArray());

        assertTrue(servicio.pideContrasena(acceso));
        assertTrue(servicio.verificar(acceso, "clave123".toCharArray()));
        assertFalse(servicio.verificar(acceso, "otra1234".toCharArray()));
    }

    @Test
    void rechazaContrasenasCortasOQueNoCoinciden() {
        assertThrows(ContrasenaRechazadaException.class,
                () -> servicio.crear("123".toCharArray(), "123".toCharArray()));
        assertThrows(ContrasenaRechazadaException.class,
                () -> servicio.crear("clave123".toCharArray(), "clave124".toCharArray()));
    }

    @Test
    void cambiarExigeLaContrasenaActual() {
        AccesoAplicacion acceso = servicio.crear("clave123".toCharArray(), "clave123".toCharArray());

        assertThrows(ContrasenaRechazadaException.class, () -> servicio.cambiar(acceso,
                "incorrecta".toCharArray(), "nueva1234".toCharArray(), "nueva1234".toCharArray()));

        AccesoAplicacion cambiado = servicio.cambiar(acceso,
                "clave123".toCharArray(), "nueva1234".toCharArray(), "nueva1234".toCharArray());
        assertTrue(servicio.verificar(cambiado, "nueva1234".toCharArray()));
        assertFalse(servicio.verificar(cambiado, "clave123".toCharArray()));
    }

    @Test
    void desactivarExigeLaContrasenaActualYQuitaLaProteccion() {
        AccesoAplicacion acceso = servicio.crear("clave123".toCharArray(), "clave123".toCharArray());

        assertThrows(ContrasenaRechazadaException.class,
                () -> servicio.desactivar(acceso, "incorrecta".toCharArray()));

        AccesoAplicacion desactivado = servicio.desactivar(acceso, "clave123".toCharArray());
        assertFalse(servicio.pideContrasena(desactivado));
        assertFalse(servicio.debeCrearContrasena(desactivado));
    }

    @Test
    void borraLaContrasenaDeMemoriaAlTerminar() {
        char[] nueva = "clave123".toCharArray();
        char[] confirmacion = "clave123".toCharArray();

        servicio.crear(nueva, confirmacion);

        assertArrayEquals(new char[8], nueva);
        assertArrayEquals(new char[8], confirmacion);
    }
}
