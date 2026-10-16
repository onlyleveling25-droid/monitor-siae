package co.monitorsiae.dominio;

/**
 * Proteccion opcional de la configuracion con una contrasena de la aplicacion.
 *
 * @param requerirContrasena si la edicion de la configuracion pide contrasena
 * @param hash               contrasena derivada con PBKDF2; vacio si aun no se ha creado
 */
public record AccesoAplicacion(boolean requerirContrasena, String hash) {

    public AccesoAplicacion {
        hash = Listas.texto(hash);
    }

    public static AccesoAplicacion porDefecto() {
        return new AccesoAplicacion(true, "");
    }

    public boolean tieneContrasena() {
        return !hash.isEmpty();
    }
}
