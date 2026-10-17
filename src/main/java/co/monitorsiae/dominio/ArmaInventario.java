package co.monitorsiae.dominio;

/**
 * Una fila del listado de armas del paso 2.
 *
 * @param posicion    posicion de la fila en el listado, empezando en 0; sirve para seleccionarla
 * @param descripcion texto completo de la fila, tal como lo muestra la plataforma
 * @param tipo        columna "Tipo de Arma" si se pudo leer; vacio si no
 */
public record ArmaInventario(int posicion, String descripcion, String tipo) {

    public ArmaInventario {
        descripcion = descripcion == null ? "" : descripcion.strip();
        tipo = tipo == null ? "" : tipo.strip();
    }

    /**
     * Tipo de arma conocido o deducido. Si la columna no se pudo leer, se toma la primera palabra
     * de la descripcion, que en la plataforma siempre es el tipo ("Pistola Glock ...").
     */
    public String tipoEfectivo() {
        if (!tipo.isEmpty()) {
            return tipo;
        }
        int espacio = descripcion.indexOf(' ');
        return espacio < 0 ? descripcion : descripcion.substring(0, espacio);
    }
}
