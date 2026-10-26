package co.monitorsiae.app;

import co.monitorsiae.dominio.Cliente;
import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.EntornoPortal;
import co.monitorsiae.dominio.NavegadorPreferido;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistentePrimeraEjecucionTest {

    @Test
    void esNecesarioCuandoNoHayClientesConfigurados() {
        assertTrue(AsistentePrimeraEjecucion.esNecesario(Configuracion.porDefecto()));
    }

    @Test
    void esNecesarioCuandoLaConfiguracionEsNula() {
        assertTrue(AsistentePrimeraEjecucion.esNecesario(null));
    }

    @Test
    void noEsNecesarioCuandoYaHayAlMenosUnCliente() {
        Configuracion conCliente = Configuracion.porDefecto().conClientes(List.of(Cliente.nuevo("Ana")));

        assertFalse(AsistentePrimeraEjecucion.esNecesario(conCliente));
    }

    @Test
    void unNombreVacioOEnBlancoNoEsValido() {
        assertNotNull(AsistentePrimeraEjecucion.validarNombreCliente(""));
        assertNotNull(AsistentePrimeraEjecucion.validarNombreCliente("   "));
        assertNotNull(AsistentePrimeraEjecucion.validarNombreCliente(null));
    }

    @Test
    void unNombreConContenidoEsValido() {
        assertNull(AsistentePrimeraEjecucion.validarNombreCliente("Ana"));
    }

    @Test
    void aplicarFijaNavegadorEntornoYPrimerCliente() {
        Configuracion base = Configuracion.porDefecto();

        Configuracion resultado = AsistentePrimeraEjecucion.aplicar(base, NavegadorPreferido.EDGE, "Ana",
                EntornoPortal.PORTAL_REAL);

        assertEquals(NavegadorPreferido.EDGE, resultado.navegador());
        assertEquals(EntornoPortal.PORTAL_REAL, resultado.entornoPortal());
        assertEquals(1, resultado.clientes().size());
        assertEquals("Ana", resultado.clientes().get(0).nombre());
    }

    @Test
    void aplicarNoCambiaElNavegadorNiElEntornoSiVienenNulos() {
        Configuracion base = Configuracion.porDefecto();

        Configuracion resultado = AsistentePrimeraEjecucion.aplicar(base, null, "Luis", null);

        assertEquals(base.navegador(), resultado.navegador());
        assertEquals(base.entornoPortal(), resultado.entornoPortal());
        assertEquals("Luis", resultado.clientes().get(0).nombre());
    }

    @Test
    void aplicarRechazaUnNombreInvalido() {
        Configuracion base = Configuracion.porDefecto();

        assertThrows(IllegalArgumentException.class,
                () -> AsistentePrimeraEjecucion.aplicar(base, NavegadorPreferido.CHROME, "", EntornoPortal.SIMULADOR));
    }

    @Test
    void aplicarNoAlteraHorarioNiPreferenciasDeAlertas() {
        Configuracion base = Configuracion.porDefecto();

        Configuracion resultado = AsistentePrimeraEjecucion.aplicar(base, NavegadorPreferido.CHROME, "Ana",
                EntornoPortal.SIMULADOR);

        assertEquals(base.horario(), resultado.horario());
        assertEquals(base.preferenciasAlertas(), resultado.preferenciasAlertas());
    }
}
