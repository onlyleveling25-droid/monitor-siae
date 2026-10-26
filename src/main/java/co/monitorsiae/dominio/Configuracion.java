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
 * @param variacionEsperaPorcentaje variación aleatoria (jitter) de cada espera entre consultas, de 0 a 50 %;
 *                                 0 la desactiva y cada espera dura exactamente el intervalo
 */
public record Configuracion(
        int version,
        AccesoAplicacion acceso,
        NavegadorPreferido navegador,
        EntornoPortal entornoPortal,
        int intervaloConsultaSegundos,
        HorarioOperacion horario,
        List<Cliente> clientes,
        PreferenciasAlertas preferenciasAlertas,
        EscenarioSimulador escenarioSimulador,
        Integer variacionEsperaPorcentaje) {

    public static final int VERSION_ACTUAL = 3;
    public static final int INTERVALO_MINIMO_SEGUNDOS = 2;
    public static final int INTERVALO_POR_DEFECTO_SEGUNDOS = 5;
    public static final int VARIACION_POR_DEFECTO_PORCENTAJE = 20;
    public static final int VARIACION_MAXIMA_PORCENTAJE = 50;

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
        preferenciasAlertas = Objects.requireNonNullElseGet(preferenciasAlertas, PreferenciasAlertas::porDefecto);
        escenarioSimulador = Objects.requireNonNullElseGet(escenarioSimulador, EscenarioSimulador::porDefecto);
        // Integer y no int: un archivo anterior no trae el campo y debe tomar el valor predeterminado, no 0.
        variacionEsperaPorcentaje = variacionEsperaPorcentaje == null
                ? VARIACION_POR_DEFECTO_PORCENTAJE : variacionEsperaPorcentaje;
    }

    /** Configuración sin variación de espera: toma el valor predeterminado. */
    public Configuracion(int version, AccesoAplicacion acceso, NavegadorPreferido navegador,
                         EntornoPortal entornoPortal, int intervaloConsultaSegundos, HorarioOperacion horario,
                         List<Cliente> clientes, PreferenciasAlertas preferenciasAlertas,
                         EscenarioSimulador escenarioSimulador) {
        this(version, acceso, navegador, entornoPortal, intervaloConsultaSegundos, horario, clientes,
                preferenciasAlertas, escenarioSimulador, VARIACION_POR_DEFECTO_PORCENTAJE);
    }

    /** Configuración sin escenario del simulador: toma el predeterminado. */
    public Configuracion(int version, AccesoAplicacion acceso, NavegadorPreferido navegador,
                         EntornoPortal entornoPortal, int intervaloConsultaSegundos, HorarioOperacion horario,
                         List<Cliente> clientes, PreferenciasAlertas preferenciasAlertas) {
        this(version, acceso, navegador, entornoPortal, intervaloConsultaSegundos, horario, clientes,
                preferenciasAlertas, EscenarioSimulador.porDefecto());
    }

    public Configuracion(int version, AccesoAplicacion acceso, NavegadorPreferido navegador,
                         EntornoPortal entornoPortal, int intervaloConsultaSegundos, HorarioOperacion horario,
                         List<Cliente> clientes) {
        this(version, acceso, navegador, entornoPortal, intervaloConsultaSegundos, horario, clientes,
                PreferenciasAlertas.porDefecto());
    }

    public Configuracion(int version, AccesoAplicacion acceso, NavegadorPreferido navegador,
                         int intervaloConsultaSegundos, HorarioOperacion horario, List<Cliente> clientes) {
        this(version, acceso, navegador, EntornoPortal.SIMULADOR, intervaloConsultaSegundos, horario, clientes,
                PreferenciasAlertas.porDefecto());
    }

    public static Configuracion porDefecto() {
        return new Configuracion(VERSION_ACTUAL, AccesoAplicacion.porDefecto(), NavegadorPreferido.CHROME,
                EntornoPortal.SIMULADOR, INTERVALO_POR_DEFECTO_SEGUNDOS, HorarioOperacion.porDefecto(), List.of(),
                PreferenciasAlertas.porDefecto());
    }

    public Configuracion conAcceso(AccesoAplicacion nuevoAcceso) {
        return new Configuracion(version, nuevoAcceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, clientes, preferenciasAlertas, escenarioSimulador, variacionEsperaPorcentaje);
    }

    public Configuracion conClientes(List<Cliente> nuevosClientes) {
        return new Configuracion(version, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, nuevosClientes, preferenciasAlertas, escenarioSimulador, variacionEsperaPorcentaje);
    }

    public Configuracion conEntornoPortal(EntornoPortal nuevoEntorno) {
        return new Configuracion(version, acceso, navegador, nuevoEntorno,
                intervaloConsultaSegundos, horario, clientes, preferenciasAlertas, escenarioSimulador, variacionEsperaPorcentaje);
    }

    public Configuracion conPreferenciasAlertas(PreferenciasAlertas nuevasPreferencias) {
        return new Configuracion(version, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, clientes, nuevasPreferencias, escenarioSimulador, variacionEsperaPorcentaje);
    }

    public Configuracion conHorario(HorarioOperacion nuevoHorario) {
        return new Configuracion(version, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, nuevoHorario, clientes, preferenciasAlertas, escenarioSimulador, variacionEsperaPorcentaje);
    }

    public Configuracion conEscenarioSimulador(EscenarioSimulador nuevoEscenario) {
        return new Configuracion(version, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, clientes, preferenciasAlertas, nuevoEscenario, variacionEsperaPorcentaje);
    }

    public Configuracion conVariacionEsperaPorcentaje(int nuevaVariacion) {
        return new Configuracion(version, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, clientes, preferenciasAlertas, escenarioSimulador, nuevaVariacion);
    }

    public Configuracion conVersionActual() {
        return new Configuracion(VERSION_ACTUAL, acceso, navegador, entornoPortal,
                intervaloConsultaSegundos, horario, clientes, preferenciasAlertas, escenarioSimulador, variacionEsperaPorcentaje);
    }
}
