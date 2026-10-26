package co.monitorsiae.dominio;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilEscalamientoTest {

    private final NivelEscalamiento nivelInicial =
            new NivelEscalamiento(Duration.ofMinutes(1), false, false, TonoAlarma.SIRENA);
    private final NivelEscalamiento nivelIntermedio =
            new NivelEscalamiento(Duration.ofMinutes(3), true, false, TonoAlarma.DOBLE);
    private final NivelEscalamiento nivelFinal =
            new NivelEscalamiento(Duration.ofMinutes(5), true, true, TonoAlarma.SUAVE);

    @Test
    void deshabilitadoNoAplicaNingunNivel() {
        PerfilEscalamiento perfil = PerfilEscalamiento.deshabilitado();

        assertTrue(perfil.nivelVigente(Duration.ofHours(1)).isEmpty());
        assertTrue(perfil.niveles().isEmpty());
    }

    @Test
    void ordenaLosNivelesPorEsperaSinImportarElOrdenDeEntrada() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(nivelFinal, nivelInicial, nivelIntermedio));

        assertEquals(List.of(nivelInicial, nivelIntermedio, nivelFinal), perfil.niveles());
    }

    @Test
    void antesDelPrimerNivelNoHayNivelVigente() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true, List.of(nivelInicial, nivelIntermedio));

        assertTrue(perfil.nivelVigente(Duration.ofSeconds(30)).isEmpty());
    }

    @Test
    void enLaFronteraExactaDeLaEsperaElNivelYaEstaVigente() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true, List.of(nivelInicial));

        assertEquals(Optional.of(nivelInicial), perfil.nivelVigente(Duration.ofMinutes(1)));
    }

    @Test
    void devuelveElNivelMasAltoQueYaCumplioSuEspera() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(nivelInicial, nivelIntermedio, nivelFinal));

        assertEquals(Optional.of(nivelIntermedio), perfil.nivelVigente(Duration.ofMinutes(4)));
    }

    @Test
    void unPerfilHabilitadoPeroSinNivelesNuncaAplicaNada() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true, List.of());

        assertTrue(perfil.nivelVigente(Duration.ofHours(10)).isEmpty());
    }

    @Test
    void transcurridoNuloNoAplicaNingunNivel() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true, List.of(nivelInicial));

        assertTrue(perfil.nivelVigente(null).isEmpty());
    }

    @Test
    void unPerfilDeshabilitadoConNivelesConfiguradosNoLosAplica() {
        PerfilEscalamiento perfil = new PerfilEscalamiento(false, List.of(nivelInicial, nivelFinal));

        assertTrue(perfil.nivelVigente(Duration.ofHours(1)).isEmpty());
    }

    @Test
    void nivelesNulosSeDescartanYNoRompenElOrden() {
        List<NivelEscalamiento> conNulo = new ArrayList<>();
        conNulo.add(nivelFinal);
        conNulo.add(null);
        conNulo.add(nivelInicial);
        PerfilEscalamiento perfil = new PerfilEscalamiento(true, conNulo);

        assertEquals(List.of(nivelInicial, nivelFinal), perfil.niveles());
    }
}
