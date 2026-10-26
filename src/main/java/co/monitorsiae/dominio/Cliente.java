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
 * @param alcancePaso2 hasta donde debe llegar la automatizacion del paso 2 (pre-asignar o sellar el tramite)
 */
public record Cliente(
        String id,
        String nombre,
        boolean activo,
        ParametrosTramite tramite,
        ModoReaccion modo,
        ReglasSeleccion seleccion,
        PreferenciaSonidoCliente preferenciaSonido,
        AlcancePaso2 alcancePaso2) {

    public Cliente {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.strip();
        nombre = Listas.texto(nombre);
        tramite = Objects.requireNonNullElseGet(tramite, ParametrosTramite::porDefecto);
        modo = Objects.requireNonNullElse(modo, ModoReaccion.APARTAR);
        seleccion = Objects.requireNonNullElseGet(seleccion, ReglasSeleccion::porDefecto);
        preferenciaSonido = Objects.requireNonNullElse(preferenciaSonido, PreferenciaSonidoCliente.HEREDAR);
        alcancePaso2 = Objects.requireNonNullElse(alcancePaso2, AlcancePaso2.PRE_ASIGNAR_ARMA);
    }

    public Cliente(String id, String nombre, boolean activo, ParametrosTramite tramite,
                   ModoReaccion modo, ReglasSeleccion seleccion, PreferenciaSonidoCliente preferenciaSonido) {
        this(id, nombre, activo, tramite, modo, seleccion, preferenciaSonido, AlcancePaso2.PRE_ASIGNAR_ARMA);
    }

    public Cliente(String id, String nombre, boolean activo, ParametrosTramite tramite,
                   ModoReaccion modo, ReglasSeleccion seleccion) {
        this(id, nombre, activo, tramite, modo, seleccion, PreferenciaSonidoCliente.HEREDAR,
                AlcancePaso2.PRE_ASIGNAR_ARMA);
    }

    public static Cliente nuevo(String nombre) {
        return new Cliente(null, nombre, true, ParametrosTramite.porDefecto(),
                ModoReaccion.APARTAR, ReglasSeleccion.porDefecto(), PreferenciaSonidoCliente.HEREDAR,
                AlcancePaso2.PRE_ASIGNAR_ARMA);
    }
}
