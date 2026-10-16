package co.monitorsiae.dominio;

/**
 * Un error de configuracion que impide monitorear.
 *
 * @param ubicacion donde esta el error, por ejemplo "Cliente 'Ana' > Descripcion"
 * @param mensaje   explicacion para el usuario
 */
public record ProblemaConfiguracion(String ubicacion, String mensaje) {

    @Override
    public String toString() {
        return ubicacion + ": " + mensaje;
    }
}
