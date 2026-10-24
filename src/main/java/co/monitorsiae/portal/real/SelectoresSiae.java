package co.monitorsiae.portal.real;

/**
 * Todos los selectores del portal SIAE en un solo lugar. Si el portal cambia, se toca aqui.
 *
 * <p>El paso 1 es React + MUI. El paso 2 es otra aplicacion del portal (Angular + Material) y
 * puede abrirse en una pestana aparte: los selectores de esa parte son los que se revisan primero
 * si algo falla.
 */
final class SelectoresSiae {

    private SelectoresSiae() {
    }

    // --- Paso 1: los tres combos comparten id, se distinguen por el placeholder ---
    static final String PLACEHOLDER_PERMISO = "Seleccione como desea obtener el permiso";
    static final String PLACEHOLDER_TIPO_TRAMITE =
            "Ingrese el parametro Tipo de Tramite / Clase de Tramite";
    static final String PLACEHOLDER_SECCIONAL =
            "Indique la seccional donde va a tramitar el permiso para porte o tenencia";

    static final String CAMPO_DESCRIPCION = "textarea[name='descripcion']";
    static final String CONTADOR_DESCRIPCION = "text=/restantes\\s*\\d+/";
    static final String BOTON_GUARDAR = "button:has-text('GUARDAR')";
    static final String AVISO_NO_DISPONIBLE = "div[role='alert']:has-text('no esta disponible')";
    static final String LOGIN_PASSWORD = "input[type='password']";
    static final String INGRESAR_PORTAL = "text=INGRESAR PORTAL";

    /** Opciones de cualquier lista abierta: MUI (React) y Material (Angular). */
    static final String OPCIONES = "[role='option'], mat-option, li[role='option']";

    // --- Paso 2 -----------------------------------------------------------------
    // El aviso real va en MAYUSCULAS: la bandera i no es opcional.
    static final String SIN_INVENTARIO = "text=/NO HAY INVENTARIO DISPONIBLE/i";
    static final String GUARDANDO = "text=/GUARDANDO/i";

    /** Pantalla intermedia: hay que pulsar este enlace para que se abra el inventario. */
    static final String ENLACE_INVENTARIO = "text=/solicitud\\s+adquisici[oó]n\\s+de\\s+armas/i";

    /**
     * Senal de FG-1 ABIERTA. NO sirve "text=FG-1": ese texto tambien esta en la tarjeta de la
     * pantalla intermedia y en la cabecera de cada modal. El boton FINALIZAR es exclusivo de la
     * ventana del inventario.
     */
    static final String PASO_2 = "button:has-text('FINALIZAR')";

    /** Tras sellar la reserva, la tarjeta pasa a "Formulario antecedentes Fiscalia". */
    static final String PASO_3 = "text=/Formulario antecedentes Fiscal[ií]a/i"; // CONFIRMAR con 18-tras-confirmar.txt

    /** Filas: mat-table de Angular Material o DataGrid de MUI. */
    static final String FILAS_INVENTARIO = "table tbody tr, [role='row']:has([role='gridcell'])";
    static final String CELDAS_INVENTARIO = "td, [role='gridcell']";
    static final String FILA_SELECCIONADA = "text=/\\d+\\s*fila[s]?\\s+seleccionada/i";

    /** Modales: Angular Material (mat-dialog) y MUI. */
    static final String MODALES =
            "[role='dialog'], mat-dialog-container, .cdk-overlay-pane, .MuiDialog-container";
    static final String CAMPOS_MODAL =
            "input, textarea, [role='combobox'], mat-select, [role='spinbutton']";
    static final String BOTON_CONFIRMAR = "button:has-text('CONFIRMAR')";
    static final String BOTON_VOLVER = "button:has-text('VOLVER')";

    static final String BOTON_PORTE = "button:has-text('PORTE')";
    static final String BOTON_TENENCIA = "button:has-text('TENENCIA')";
    static final String BOTON_FINALIZAR = "button:has-text('FINALIZAR')";

    static final String ERROR_CUPO = "text=/NO CUENTA CON CUPO DISPONIBLE/i";
    static final String ASIGNACION_EXITOSA ="text=/asignaci[oó]n\\s+exitosa|se\\s+asign[oó]|registrad[oa]\\s+correctamente/i";
}