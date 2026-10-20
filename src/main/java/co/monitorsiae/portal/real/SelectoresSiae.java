package co.monitorsiae.portal.real;

/**
 * Todos los selectores del portal SIAE en un solo lugar. Si el portal cambia,
 * se toca aqui.
 */
final class SelectoresSiae {

        private SelectoresSiae() {
        }

        // --- Paso 1 ---------------------------------------------------------------
        // Los tres desplegables comparten id="asynchronous-demo": se distinguen por su
        // placeholder.
        static final String PLACEHOLDER_PERMISO = "Seleccione como desea obtener el permiso";
        static final String PLACEHOLDER_TIPO_TRAMITE = "Ingrese el parametro Tipo de Tramite / Clase de Tramite";
        static final String PLACEHOLDER_SECCIONAL = "Indique la seccional donde va a tramitar el permiso para porte o tenencia";

        static final String OPCIONES = "[role='option']";
        static final String CAMPO_DESCRIPCION = "textarea[name='descripcion']";
        static final String CONTADOR_DESCRIPCION = "text=/restantes\\s*\\d+/";
        static final String BOTON_GUARDAR = "button:has-text('GUARDAR')";
        static final String AVISO_NO_DISPONIBLE = "div[role='alert']:has-text('no esta disponible')";
        static final String LOGIN_PASSWORD = "input[type='password']";
        static final String INGRESAR_PORTAL = "text=INGRESAR PORTAL";

     
        // --- Paso 2 (Estructura real del manual DCCAE) ---------------------------
        static final String PASO_2 = "text=FG-1";
        static final String SIN_INVENTARIO = "text=/[Nn]o hay inventario/";
        static final String FILAS_INVENTARIO = "table tbody tr";

        // Nuevos selectores para apartar:
        static final String BOTON_PORTE = "button:has-text('PORTE'), div:has-text('PORTE'), [role='button']:has-text('PORTE')";
        static final String BOTON_TENENCIA = "button:has-text('TENENCIA'), div:has-text('TENENCIA'), [role='button']:has-text('TENENCIA')";
        static final String BOTON_FINALIZAR = "button:has-text('FINALIZAR'), button:has-text('Finalizar')";
        static final String BOTON_CONFIRMAR_MODAL = "button:has-text('CONFIRMAR'), button:has-text('Confirmar')";
        static final String ASIGNACION_EXITOSA = "text=/Asignaci[oó]n exitosa|exitoso|registrad[ao]/i";
}