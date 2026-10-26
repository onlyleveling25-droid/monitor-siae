package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.EventoOperativo;

import java.util.ArrayList;
import java.util.List;

public final class RepositorioBitacoraMemoria implements RepositorioBitacora {

    private final List<EventoOperativo> eventos = new ArrayList<>();

    @Override
    public synchronized void registrar(EventoOperativo evento) {
        eventos.addFirst(evento);
        while (eventos.size() > RepositorioBitacoraJson.MAXIMO_EVENTOS) {
            eventos.removeLast();
        }
    }

    @Override
    public synchronized List<EventoOperativo> eventos() {
        return List.copyOf(eventos);
    }
}
