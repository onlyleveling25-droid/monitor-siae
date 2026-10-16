package co.monitorsiae.dominio;

import java.util.List;

/**
 * Valores del paso 1 del tramite, escritos tal como aparecen en la plataforma.
 *
 * @param obtenerPermiso opcion de "Seleccione como desea obtener el permiso"
 * @param tipoTramite    opcion de "Tipo de Tramite / Clase de Tramite"
 * @param seccionales    seccionales aceptables, en orden de preferencia
 * @param descripcion    justificacion de la solicitud (entre 150 y 500 caracteres)
 */
public record ParametrosTramite(
        String obtenerPermiso,
        String tipoTramite,
        List<String> seccionales,
        String descripcion) {

    public static final int DESCRIPCION_MINIMA = 150;
    public static final int DESCRIPCION_MAXIMA = 500;

    public ParametrosTramite {
        obtenerPermiso = Listas.texto(obtenerPermiso);
        tipoTramite = Listas.texto(tipoTramite);
        seccionales = Listas.textos(seccionales);
        // La descripcion no se recorta: la plataforma cuenta los caracteres tal como se pegan.
        descripcion = descripcion == null ? "" : descripcion;
    }

    public static ParametrosTramite porDefecto() {
        return new ParametrosTramite("Defensa Personal",
                "Armas personas naturales: Adquisición de arma", List.of(), "");
    }
}
