package co.monitorsiae.notificacion;

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

    private static Alerta alerta(String clienteId, String voz) {
        return new Alerta(clienteId, Gravedad.OPORTUNIDAD, voz, "", voz);
    }
}
