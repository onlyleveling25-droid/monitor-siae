package co.monitorsiae.portal.real;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumeroDeSolicitudTest {

    @Test
    void tomaElNumeroTalCualAparece() {
        assertEquals("1-01-45-202610-4336", PlaywrightPortalDriver.numeroDeSolicitud("1-01-45-202610-4336"));
    }

    @Test
    void ignoraLoQueSobraSiPeganLaLineaEntera() {
        assertEquals("1-01-45-202610-4336",
                PlaywrightPortalDriver.numeroDeSolicitud("  1-01-45-202610-4336 - EN PROGRESO "));
    }

    @Test
    void vacioOSinFormaDeNumeroSeDevuelveSinEspacios() {
        assertEquals("", PlaywrightPortalDriver.numeroDeSolicitud("   "));
        assertEquals("", PlaywrightPortalDriver.numeroDeSolicitud(null));
        assertEquals("abc", PlaywrightPortalDriver.numeroDeSolicitud(" abc "));
    }
}
