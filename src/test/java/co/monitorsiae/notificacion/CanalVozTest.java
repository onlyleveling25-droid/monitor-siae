package co.monitorsiae.notificacion;

import co.monitorsiae.dominio.Configuracion;
import co.monitorsiae.dominio.NivelEscalamiento;
import co.monitorsiae.dominio.PerfilEscalamiento;
import co.monitorsiae.dominio.PreferenciasAlertas;
import co.monitorsiae.dominio.TonoAlarma;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanalVozTest {

    private final List<String> dichos = new CopyOnWriteArrayList<>();

    @Test
    void repiteCadaAlertaHastaQueSeReconozcan() throws InterruptedException {
        CanalVoz[] canal = new CanalVoz[1];
        int[] rondas = {0};
        canal[0] = new CanalVoz(dichos::add, duracion -> {
            if (++rondas[0] == 3) {
                canal[0].mostrar(List.of());
            }
        });

        canal[0].mostrar(List.of(alerta("ana", "Arma para Ana"), alerta("luis", "Arma para Luis")));
        canal[0].esperarSilencio(Duration.ofSeconds(5));

        assertEquals(List.of("Arma para Ana", "Arma para Luis", "Arma para Ana", "Arma para Luis",
                "Arma para Ana", "Arma para Luis"), dichos);
    }

    @Test
    void callaEnCuantoSeReconoceSinTerminarLaRonda() throws InterruptedException {
        CanalVoz[] canal = new CanalVoz[1];
        canal[0] = new CanalVoz(texto -> {
            dichos.add(texto);
            canal[0].mostrar(List.of());
        }, duracion -> { });

        canal[0].mostrar(List.of(alerta("ana", "Arma para Ana"), alerta("luis", "Arma para Luis")));
        canal[0].esperarSilencio(Duration.ofSeconds(5));

        assertEquals(List.of("Arma para Ana"), dichos);
    }

    @Test
    void sinAlertasNoHabla() throws InterruptedException {
        CanalVoz canal = new CanalVoz(dichos::add, duracion -> { });

        canal.mostrar(List.of());
        canal.esperarSilencio(Duration.ofSeconds(1));

        assertTrue(dichos.isEmpty());
    }

    @Test
    void respetaLaPreferenciaDeVozDeshabilitada() throws InterruptedException {
        Configuracion configuracion = Configuracion.porDefecto()
                .conPreferenciasAlertas(new PreferenciasAlertas(true, false, TonoAlarma.SIRENA));
        CanalVoz canal = new CanalVoz(dichos::add, duracion -> { }, () -> configuracion);

        canal.mostrar(List.of(alerta("ana", "Alerta para Ana")));
        canal.esperarSilencio(Duration.ofSeconds(1));

        assertTrue(dichos.isEmpty());
    }

    @Test
    void unNivelDeEscalamientoForzarVozHaceHablarAunqueLaVozEsteDeshabilitada() throws InterruptedException {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(new NivelEscalamiento(Duration.ofMinutes(5), false, true, TonoAlarma.SIRENA)));
        Configuracion configuracion = Configuracion.porDefecto()
                .conPreferenciasAlertas(new PreferenciasAlertas(true, false, TonoAlarma.SIRENA, perfil));
        CanalVoz[] canal = new CanalVoz[1];
        canal[0] = new CanalVoz(texto -> {
            dichos.add(texto);
            canal[0].mostrar(List.of());
        }, duracion -> { }, () -> configuracion, id -> Duration.ofMinutes(10));

        canal[0].mostrar(List.of(alerta("ana", "Alerta para Ana")));
        canal[0].esperarSilencio(Duration.ofSeconds(1));

        assertEquals(List.of("Alerta para Ana"), dichos);
    }

    @Test
    void sinNivelVigenteLaVozSigueDeshabilitada() throws InterruptedException {
        PerfilEscalamiento perfil = new PerfilEscalamiento(true,
                List.of(new NivelEscalamiento(Duration.ofMinutes(5), false, true, TonoAlarma.SIRENA)));
        Configuracion configuracion = Configuracion.porDefecto()
                .conPreferenciasAlertas(new PreferenciasAlertas(true, false, TonoAlarma.SIRENA, perfil));
        CanalVoz canal = new CanalVoz(dichos::add, duracion -> { }, () -> configuracion, id -> Duration.ofMinutes(1));

        canal.mostrar(List.of(alerta("ana", "Alerta para Ana")));
        canal.esperarSilencio(Duration.ofSeconds(1));

        assertTrue(dichos.isEmpty());
    }

    private static Alerta alerta(String clienteId, String voz) {
        return new Alerta(clienteId, Gravedad.OPORTUNIDAD, voz, "", voz);
    }
}
