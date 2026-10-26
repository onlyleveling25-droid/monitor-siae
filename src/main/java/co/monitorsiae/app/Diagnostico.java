package co.monitorsiae.app;

import java.util.Comparator;
import java.util.List;

/** Conjunto de comprobaciones independientes y el veredicto general. */
public record Diagnostico(List<Comprobacion> comprobaciones) {

    public Diagnostico {
        comprobaciones = List.copyOf(comprobaciones);
    }

    /** El veredicto es el peor nivel encontrado. */
    public Comprobacion.Nivel veredicto() {
        return comprobaciones.stream().map(Comprobacion::nivel)
                .max(Comparator.naturalOrder()).orElse(Comprobacion.Nivel.CORRECTO);
    }

    public String resumen() {
        return switch (veredicto()) {
            case CORRECTO -> "Todo en orden: no se encontraron problemas.";
            case AVISO -> "Funciona, pero hay puntos que conviene revisar.";
            case PROBLEMA -> "Hay problemas que impiden o ponen en riesgo el monitoreo.";
        };
    }
}
