package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.EventoOperativo;

import java.util.List;

public interface RepositorioBitacora {

    void registrar(EventoOperativo evento);

    /** Eventos conservados, del más reciente al más antiguo. */
    List<EventoOperativo> eventos();
}
