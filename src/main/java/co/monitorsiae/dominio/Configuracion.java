package co.monitorsiae.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Toda la configuracion de la aplicacion. Es inmutable: cada cambio produce una copia.
 *
 * @param version                  version del formato, para migrar archivos antiguos
 * @param acceso                   contrasena de la aplicacion
 * @param navegador                navegador instalado que se controla
 * @param entornoPortal            simulador o portal real
 * @param intervaloConsultaSegundos pausa entre consultas de un mismo cliente
 * @param horario                  ventana de operacion
 * @param clientes                 clientes configurados
 */
public record Configuracion(
        int version,
        AccesoAplicacion acceso,
        NavegadorPreferido navegador,
        EntornoPortal entornoPortal,
        int intervaloConsultaSegundos,
        HorarioOperacion horario,
        List<Cliente> clientes) {

    public static final int VERSION_ACTUAL = 1;
    public static final int INTERVALO_MINIMO_SEGUNDOS = 2;
    public static final int INTERVALO_POR_DEFECTO_SEGUNDOS = 5;

    public Configuracion {
        version = version <= 0 ? VERSION_ACTUAL : version;
        acceso = Objects.requireNonNullElseGet(acceso, AccesoAplicacion::porDefecto);
        navegador = Objects.requireNonNullElse(navegador, NavegadorPreferido.CHROME);
        entornoPortal = Objects.requireNonNullElse(entornoPortal, EntornoPortal.SIMULADOR);
        intervaloConsultaSegundos = intervaloConsultaSegundos <= 0
                ? INTERVALO_POR_DEFECTO_SEGUNDOS
                : intervaloConsultaSegundos;
        horario = Objects.requireNonNullElseGet(horario, HorarioOperacion::porDefecto);
        clientes = Listas.inmutable(clientes);
    }

    public Configuracion(int version, AccesoAplicacion acceso, NavegadorPreferido navegador,
                         int intervaloConsultaSegundos, HorarioOperacion horario, List<Cliente> clientes) {
        this(version, acceso, navegador, EntornoPortal.SIMULADOR, intervaloConsultaSegundos, horario, clientes);
    }

    public static Configuracion porDefecto() {
        return new Configuracion(VERSION_ACTUAL, AccesoAplicacion.porDefecto(), NavegadorPreferido.CHROME,
                EntornoPortal.SIMULADOR, INTERVALO_POR_DEFECTO_SEGUNDOS, HorarioOperacion.porDefecto(), List.of());
    }

    public Configuracion conAcceso(AccesoAplicacion nuevoAcceso) {
        return new Configuracion(version, nuevoAcceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, clientes);
    }

    public Configuracion conClientes(List<Cliente> nuevosClientes) {
        return new Configuracion(version, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, nuevosClientes);
    }

    public Configuracion conEntornoPortal(EntornoPortal nuevoEntorno) {
        return new Configuracion(version, acceso, navegador, nuevoEntorno,
                intervaloConsultaSegundos, horario, clientes);
    }
}
