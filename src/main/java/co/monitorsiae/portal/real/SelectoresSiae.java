package co.monitorsiae.portal.real;

/** Todos los selectores del portal SIAE en un solo lugar. Si el portal cambia, se toca aqui. */
final class SelectoresSiae {

    private SelectoresSiae() {
    }

    // --- Paso 1 ---------------------------------------------------------------
    // Los tres desplegables comparten id="asynchronous-demo": se distinguen por su placeholder.
    static final String PLACEHOLDER_PERMISO = "Seleccione como desea obtener el permiso";
    static final String PLACEHOLDER_TIPO_TRAMITE =
            "Ingrese el parametro Tipo de Tramite / Clase de Tramite";
    static final String PLACEHOLDER_SECCIONAL =
            "Indique la seccional donde va a tramitar el permiso para porte o tenencia";

    static final String OPCIONES = "[role='option']";
    static final String CAMPO_DESCRIPCION = "textarea[name='descripcion']";
    static final String CONTADOR_DESCRIPCION = "text=/restantes\\s*\\d+/";
    static final String BOTON_GUARDAR = "button:has-text('GUARDAR')";
    static final String AVISO_NO_DISPONIBLE = "div[role='alert']:has-text('no esta disponible')";
    static final String LOGIN_PASSWORD = "input[type='password']";

    // --- Paso 2 (CONFIRMAR con el DOM real) -----------------------------------
    static final String PASO_2 = "text=FG-1";
    static final String SIN_INVENTARIO = "text=/[Nn]o hay inventario/";
    static final String FILAS_INVENTARIO = "table tbody tr";
    static final String ASIGNACION_EXITOSA = "text=/Asignaci[oó]n exitosa/";
}