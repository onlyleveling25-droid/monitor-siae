# Plan de rediseño de la interfaz de Monitor SIAE

## Alcance y reglas de trabajo

Todo el trabajo de rediseño se realiza en la rama `feature/redisenio-ui`. Las pruebas, correcciones y validaciones manuales permanecen en esta rama; **no se integra ningún cambio a `main` hasta cerrar todas las fases, revisar el resultado completo y recibir aprobación explícita**.

El objetivo es mejorar claridad, eficiencia y consistencia visual sin alterar reglas de negocio ni romper automatizaciones existentes.

### Invariantes

- No cambiar validaciones, reglas de selección, horarios ni decisiones de monitoreo.
- No modificar Playwright salvo que una función aprobada lo requiera, de forma acotada y probada (por ejemplo, enfocar la ventana de un cliente).
- Mantener apertura, inicio, detención, reanudación y detección del cierre de sesión del navegador.
- Mantener compatibilidad con la configuración JSON existente, sus respaldos y escritura segura.
- Las preferencias nuevas deben tener valores predeterminados compatibles y migración cubierta por pruebas.
- Cada fase debe compilar y pasar las pruebas automatizadas. Cuando corresponda, el usuario valida el comportamiento visual localmente en esta rama antes de continuar.
- No hacer merge ni sincronizar los cambios de trabajo con `main` durante las pruebas.

## Dirección visual aprobada

- Base clara y neutra azulada; superficies blancas, bordes discretos y sombra mínima.
- Tipografía nativa (`Segoe UI Variable`, con `Segoe UI` como respaldo), jerarquía y espaciado coherentes.
- Colores semánticos: azul para trabajo, teal para sesión abierta, ámbar para espera, naranja para oportunidad/alerta, verde para éxito, rojo para problema y gris para neutral.
- Iconografía SVG propia, consistente, con nombres accesibles; evitar glifos improvisados y dependencias de fuentes de iconos.
- Transiciones cortas y sutiles para cambios de estado; evitar parpadeo repetido o animaciones que distraigan.
- Acciones principales claras y acciones secundarias agrupadas en `⋯` cuando corresponda.

## Fases

### 1. Fundamentos visuales e iconografía — completada

- Definir tokens visuales en CSS JavaFX.
- Incorporar iconos SVG locales y cargarlos como trazados JavaFX sin dependencia externa.
- Preparar estilos base para panel, controles, estados, alertas y formularios.

**Validación:** compilación, carga de todos los SVG mediante prueba y suite automatizada.

### 2. Panel operativo — implementado, validación de contraste pendiente

- Presentar una fila compacta por cliente con identidad, modo, estado, actividad reciente y antigüedad relativa.
- Mostrar una acción contextual principal; llevar detener, historial y edición al menú `⋯`.
- Añadir resumen con métricas fiables (clientes, monitoreo activo, sesiones abiertas y alertas activas); no presentar como real un contador no disponible, como “consultas hoy”.
- Dar tratamiento visual diferenciado a cliente activo/inactivo y a cada estado operativo.
- Resaltar brevemente una actualización, enfatizar oportunidades en naranja y facilitar estados vacíos.
- Mantener todos los handlers actuales de apertura, inicio, detención, historial y reconocimiento.

**Validación manual del panel:** el usuario confirmó que la primera versión se ve mejor y que las acciones probadas funcionan. Se aplicaron ajustes adicionales de contraste a activo/inactivo, actividad, tiempo relativo y menú `⋯`; queda revisar visualmente esos últimos ajustes junto con la nueva página de Configuración.

### 3. Configuración como página — implementada, validación de guardado pendiente

- Sustituir el diálogo modal por una página dentro del marco de navegación. El contenido general/clientes conserva sus controles y validaciones.
- Añadir secciones: General, Clientes, Horario, Acceso y Alarmas.
- Mantener el editor maestro-detalle de clientes, agrupando los campos por identidad, trámite y selección.
- Conservar el guardado explícito, cancelación/descartar cambios, autorización por contraseña y validación existente.
- Asegurar que errores al guardar mantengan los cambios visibles para corregirlos y que navegar fuera no descarte cambios sin aviso.

**Validación:** guardar, cancelar, editar múltiples clientes, probar contraseña, producir errores de validación y recargar una configuración previa.

**Validación manual parcial:** el usuario confirmó que la navegación entre Panel y Ajustes funciona en ambos sentidos. Falta recorrer los flujos de guardado, descarte, autorización y validación.

**Comportamiento acordado:** Guardar mantiene la página de Ajustes abierta y confirma el resultado allí. “Editar cliente” desde una fila abre directamente la pestaña Clientes con el perfil correspondiente seleccionado.

### 4. Preferencias de alarmas — implementadas, validación manual pendiente

- Añadir canales configurables (sonido y voz como mínimo) y tonos seleccionables con vista previa.
- Permitir habilitar o silenciar sonido por cliente independientemente de que la oportunidad visual continúe disponible.
- Permitir sobrescrituras por cliente y valores predeterminados generales.
- Añadir reconocimiento individual de alarmas, sin silenciar clientes distintos.
- Versionar el JSON y migrar campos ausentes con valores predeterminados seguros; conservar `.bak`.
- El sonido y la voz se mantienen habilitados por defecto para preservar el comportamiento anterior.
- El sonido por cliente puede heredar el valor general o sobrescribirlo. Las preferencias de audio no ocultan alertas visuales.
- El formato JSON se actualizó a la versión 2; las configuraciones anteriores se migran en memoria con preferencias seguras y se guardan en el formato nuevo al guardar cambios.
- La página distingue visualmente sonido y voz, explica los tres modos por cliente y previsualiza tres ciclos del patrón para facilitar la comparación.

**Validación:** lectura de JSON anterior, guardar/cargar configuración nueva, canales independientes, preferencias por cliente, previsualización y reconocimiento sin detener monitoreo.

### 5. Ir a la ventana del cliente — implementado, validación manual pendiente

- Mostrar “Ir al navegador” desde la oportunidad y las acciones de cliente.
- Enrutar la solicitud a través del coordinador y del hilo propietario de la sesión; la UI nunca accede directamente a objetos Playwright.
- Permitir solicitar el enfoque mientras el monitor está activo, ejecutándolo en su hilo entre operaciones de Playwright y reanudando la espera restante para conservar el intervalo de consulta.
- Enfocar/traer al frente la página compatible con las capacidades disponibles del navegador y Windows.
- Informar claramente si la sesión no existe, se cerró o no se pudo enfocar.
- Si el usuario solicita ir a un navegador que ya se confirmó cerrado, reconocer solamente la alerta de ese cliente para detener su sonido/voz; los errores de enfoque no relacionados con cierre no silencian alertas.
- La tarjeta de una oportunidad muestra “Ir al navegador” como acción principal; también está disponible en el menú del cliente y en la ventana de alertas.
- El driver Playwright usa `Page.bringToFront()` desde el trabajador dedicado del cliente. La aplicación informa que solicitó el enfoque; Windows puede mantener el navegador minimizado o limitar su activación y mostrar el icono parpadeante en la barra de tareas. No se usan APIs nativas que fuercen el foco de Windows.

**Validación automatizada:** prueba que enfocar durante un monitoreo activo no espera al ciclo siguiente y conserva el hilo propietario.

**Validación manual pendiente:** probar con dos clientes abiertos, confirmar que se enfoca el correcto mientras monitorea, restaurar una ventana minimizada, verificar la alerta de sesión cerrada y asegurar que solo esa alerta se silencia al intentar ir a un navegador inexistente.

### 6. Bitácora

- Mostrar eventos operativos útiles por cliente, tipo y periodo.
- Incluir marcas de tiempo y detalle accionable; evitar registrar cada ciclo como ruido.
- Definir límite/retención para que los datos locales no crezcan indefinidamente.

**Validación:** orden temporal, filtros, límites, cierre/reinicio de aplicación y protección de datos sensibles.

### 7. Diagnóstico

- Mostrar veredicto general y comprobaciones independientes de configuración, navegador, almacenamiento, audio/voz y horario.
- Ofrecer acciones seguras y concretas cuando aplique.
- No abrir ni automatizar sesiones del portal como efecto secundario de un diagnóstico.

**Validación:** fallas simuladas de cada comprobación, textos de remedio, funcionamiento sin permisos y ausencia de efectos en Playwright.

### 8. Asistente y herramientas adicionales

- Asistente de primera ejecución para navegador, primer cliente, apertura de sesión e inicio.
- Editor visual semanal que represente el horario existente sin cambiar sus reglas.
- Evaluar modo ensayo con una especificación explícita de qué puede preparar y qué nunca debe enviar. Esta función requiere pruebas aisladas y no debe incluirse hasta que sea seguro validar el flujo sin alterar el portal.
- Evaluar perfiles y escalamiento de alarmas de forma opcional, explícita y controlable.

**Validación:** instalación/configuración inicial, usuarios existentes, equivalencia horaria con reglas actuales y garantías de no enviar cambios involuntarios en modo ensayo.

### 9. Cierre de la rama y aprobación

- Suite completa, compilación y revisión de regresiones.
- Prueba guiada del usuario en local sobre todos los flujos y páginas.
- Corregir observaciones y repetir pruebas permaneciendo en `feature/redisenio-ui`.
- Revisar compatibilidad de JSON, usabilidad, iconografía, contraste, accesibilidad y limpieza de recursos.
- Solo con todo el alcance cerrado y aprobación explícita se conversa sobre integrar en `main`; esa integración no forma parte de las pruebas intermedias.

## Estado actual

### Correcciones de estabilidad — 2026-10-19

- La contraseña preparada en Ajustes se aplica únicamente con Guardar cambios, junto con el resto
  del formulario y en una sola escritura. Cancelar o descartar conserva el acceso anterior; un error
  de escritura mantiene el borrador para reintentar.
- Cerrar la ventana principal solicita confirmación si hay cambios pendientes, usando la misma
  comprobación que volver al Panel.
- El cierre de la aplicación y del coordinador tolera llamadas repetidas sin enviar tareas a un
  trabajador que ya se cerró.
- Un historial ilegible muestra un aviso en el panel sin impedir el arranque ni modificar el archivo.
- Panel y ventana de alertas comparten el reconocimiento individual ante una sesión confirmada como
  cerrada. Otros errores de enfoque conservan las alertas.
- Verificación automatizada: compilación limpia y 111 pruebas aprobadas con JDK 21. Nueve pruebas
  nuevas cubren persistencia conjunta, descarte de acceso, fallo y reintento de guardado, cierre
  repetido, historial ilegible y reconocimiento individual.
- El usuario confirmó que puede abrir, navegar, guardar y actualizar. Los escenarios excepcionales
  anteriores siguen pendientes de validación visual local.

### Avance de las fases

- Fundamentos visuales e iconografía: completados.
- Panel operativo: implementado y probado por el usuario; revisar en local los últimos ajustes de contraste.
- Configuración como página: navegación revisada por el usuario; siguen pendientes los demás escenarios manuales de guardado y descarte.
- Preferencias de alarmas: implementadas con pruebas de migración, persistencia, selección de tono y filtrado por cliente; pendiente validación de audio en el equipo del usuario.
- Ir al navegador del cliente: implementado con pruebas del coordinador para hilo propietario y ausencia de sesión; pendiente prueba manual con varias ventanas del navegador.
- Siguiente fase: bitácora de eventos operativos con filtrado y retención.
- Rama activa: `feature/redisenio-ui`.
- Integración a `main`: aplazada hasta la aprobación final.
