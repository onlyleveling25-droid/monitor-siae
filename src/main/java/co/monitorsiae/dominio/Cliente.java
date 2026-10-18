package co.monitorsiae.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Persona en cuyo nombre se hace el tramite y la configuracion con la que se vigila.
 *
 * @param id      identificador estable; nombra tambien la carpeta del perfil de navegador
 * @param nombre  nombre para mostrar en pantalla y en las alertas
 * @param activo  si participa en el monitoreo
 * @param tramite valores del paso 1
 * @param modo    reaccion ante un arma que cumple las reglas
 * @param seleccion reglas para elegir el arma
 */
public record Cliente(
        String id,
        String nombre,
        boolean activo,
        ParametrosTramite tramite,
        ModoReaccion modo,
        ReglasSeleccion seleccion,
        PreferenciaSonidoCliente preferenciaSonido) {

    public Cliente {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.strip();
        nombre = Listas.texto(nombre);
        tramite = Objects.requireNonNullElseGet(tramite, ParametrosTramite::porDefecto);
        modo = Objects.requireNonNullElse(modo, ModoReaccion.APARTAR);
        seleccion = Objects.requireNonNullElseGet(seleccion, ReglasSeleccion::porDefecto);
        preferenciaSonido = Objects.requireNonNullElse(preferenciaSonido, PreferenciaSonidoCliente.HEREDAR);
    }

    public Cliente(String id, String nombre, boolean activo, ParametrosTramite tramite,
                   ModoReaccion modo, ReglasSeleccion seleccion) {
        this(id, nombre, activo, tramite, modo, seleccion, PreferenciaSonidoCliente.HEREDAR);
    }

    public static Cliente nuevo(String nombre) {
        return new Cliente(null, nombre, true, ParametrosTramite.porDefecto(),
                ModoReaccion.APARTAR, ReglasSeleccion.porDefecto(), PreferenciaSonidoCliente.HEREDAR);
    }
}
