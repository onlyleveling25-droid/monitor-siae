package co.monitorsiae.dominio;

/**
 * Qué le pasa al monitor cuando el entorno es el simulador. Cada vez que se inicia un monitoreo el
 * escenario arranca desde su primera escena, así que se puede repetir tantas veces como haga falta.
 */
public enum EscenarioSimulador {
    HALLAZGO_INMEDIATO("Hay inventario desde la primera consulta",
            "Encuentra armas de inmediato: sirve para probar la selección y la alarma sin esperar."),
    SIN_INVENTARIO_Y_LUEGO_HAY("Sin inventario y luego aparece",
            "Primero las seccionales están cerradas o sin inventario, y después de unas consultas aparece "
                    + "inventario: se ve el monitoreo esperando y luego la selección según el cliente."),
    INVENTARIO_SIN_OBJETIVOS("Hay inventario, pero sin los objetivos del cliente",
            "Aparece inventario que no contiene ninguno de los objetivos del cliente: sirve para ver cómo "
                    + "actúan las alternativas y el comodín."),
    APARTADO_FALLIDO("El arma aparece pero otra persona la toma",
            "Aparece un arma que cumple, pero apartarla no se confirma: prueba los reintentos y el aviso de "
                    + "apartado fallido."),
    TRES_FALLOS_SEGUIDOS("La plataforma falla tres veces seguidas",
            "Cada consulta responde con error: el monitor se detiene por fallos repetidos y suena la alarma."),
    SESION_VENCIDA("La sesión vence a mitad del monitoreo",
            "Tras un par de consultas sin inventario, la plataforma pide iniciar sesión de nuevo y el monitor "
                    + "se detiene con alarma."),
    SECCIONALES_CERRADAS("Las seccionales siempre están cerradas",
            "Todas las consultas encuentran las seccionales cerradas: sirve para dejar el monitoreo corriendo "
                    + "y probar el panel, el horario y la bitácora.");

    private final String etiqueta;
    private final String descripcion;

    EscenarioSimulador(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String descripcion() {
        return descripcion;
    }

    public static EscenarioSimulador porDefecto() {
        return SIN_INVENTARIO_Y_LUEGO_HAY;
    }
}
