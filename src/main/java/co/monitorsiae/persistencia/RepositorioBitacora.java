package co.monitorsiae.persistencia;

import co.monitorsiae.monitoreo.EventoOperativo;

import java.util.List;

public interface RepositorioBitacora {

    void registrar(EventoOperativo evento);

    /** Eventos conservados, del más reciente al más antiguo. */
    List<EventoOperativo> eventos();

    /**
     * Borra todos los eventos guardados. No toca las capturas ni el historial de inventarios.
     *
     * @return cuántos eventos se borraron
     */
    default int vaciar() {
        throw new UnsupportedOperationException("Esta bitácora no se puede vaciar.");
    }
}
