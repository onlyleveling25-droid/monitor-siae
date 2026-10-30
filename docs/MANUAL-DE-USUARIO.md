# Manual de usuario de Monitor SIAE

Este manual explica, pantalla por pantalla y opción por opción, cómo usar Monitor SIAE. Está escrito para
quien opera el programa día a día (el tramitador), no para quien lo desarrolla.

## Contenido

1. [Qué es Monitor SIAE y qué no hace](#1-qué-es-monitor-siae-y-qué-no-hace)
2. [Antes de empezar](#2-antes-de-empezar)
3. [Conceptos que conviene conocer](#3-conceptos-que-conviene-conocer)
4. [Primer arranque: el asistente](#4-primer-arranque-el-asistente)
5. [Recorrido por la ventana](#5-recorrido-por-la-ventana)
6. [El Panel](#6-el-panel)
7. [Cómo trabajar paso a paso](#7-cómo-trabajar-paso-a-paso)
8. [Ajustes](#8-ajustes)
9. [Cómo elige el arma el sistema](#9-cómo-elige-el-arma-el-sistema)
10. [Las alertas](#10-las-alertas)
11. [Bitácora](#11-bitácora)
12. [Diagnóstico](#12-diagnóstico)
13. [Historial de inventario](#13-historial-de-inventario)
14. [Dónde se guardan tus datos](#14-dónde-se-guardan-tus-datos)
15. [Solución de problemas](#15-solución-de-problemas)
16. [Glosario](#16-glosario)

---

## 1. Qué es Monitor SIAE y qué no hace

Monitor SIAE es una aplicación de escritorio para Windows que agiliza la solicitud de adquisición de arma en
la plataforma SIAE 2.0 del DCCAE.

**Cómo ayuda.** Tú inicias sesión en la plataforma con cada cliente. A partir de ahí el programa:

1. Llena el **paso 1** del trámite (cómo obtener el permiso, tipo de trámite, seccional y descripción).
2. Pulsa **GUARDAR** una y otra vez, con una espera entre intentos, mientras no haya inventario.
3. Cuando el listado del **paso 2** trae armas, elige la que mejor cumple las reglas de ese cliente.
4. Según lo que hayas configurado, **aparta** el arma (o solo te avisa).
5. Lanza una **alarma** (ventana, sonido y voz) para que tú termines el proceso.

**Lo que hace**
- Prueba, en cada vuelta, **todas** las seccionales del cliente, en el orden que tú definas.
- Puede limitarse a un **horario** con horas distintas para cada día.
- Te avisa de inmediato cuando hay inventario real, aunque sea fuera del horario configurado.
- Detecta que la sesión venció o que la plataforma respondió algo inesperado, y te avisa en lugar de repetir
  a ciegas.
- Guarda una **bitácora** de lo importante y una captura de pantalla cuando algo falla.

**Lo que no hace**
- **No inicia sesión ni resuelve el captcha.** Eso lo haces siempre tú, en el navegador.
- No guarda las contraseñas de la plataforma.
- No carga documentos ni envía la solicitud (el paso 3 es tuyo).
- **No termina el trámite por ti.** Cuando aparta un arma, tú debes entrar al navegador y completarlo.

> **Importante.** El programa solo te ayuda a llegar primero al inventario. La alarma significa "ve ya al
> navegador de ese cliente". Si no la atiendes, la reserva puede perderse.

---

## 2. Antes de empezar

### Requisitos
- Windows 10 u 11.
- **Google Chrome o Microsoft Edge** instalado (el que elijas en Ajustes).
- Para que la alarma **hable en español**: una voz en español de Windows (ver más abajo). Sin ella igual suenan
  la sirena y la ventana, pero la voz hablará con acento inglés.

### Instalar una voz en español (si no la tienes)
1. Abre **Configuración de Windows → Hora e idioma → Voz → Agregar voces** y elige **Español**.
2. Si esa pantalla se cierra o muestra 0 MB, abre **PowerShell como administrador** y ejecuta:

   ```powershell
   Add-WindowsCapability -Online -Name "Language.TextToSpeech~~~es-MX~0.0.1.0"
   ```

   (usa `es-ES` para la voz de España). No hace falta reiniciar el equipo: cierra y vuelve a abrir Monitor SIAE.

### Mientras lo uses
- Mantén el equipo **encendido y sin suspenderse**: si el equipo duerme, el monitoreo se pausa y no consulta.
- Revisa que el **volumen** del equipo esté activo. En Diagnóstico (sección 12) puedes comprobar el audio.
- No cierres Monitor SIAE mientras monitorea: al cerrarlo se detienen todos los monitoreos y se cierran los
  navegadores que el programa abrió.

---

## 3. Conceptos que conviene conocer

| Concepto | Qué significa |
|---|---|
| **Cliente** | La persona para la que haces el trámite. Cada cliente tiene su propia configuración, su propia sesión de navegador y su propio monitoreo. |
| **Monitoreo** | El ciclo automático de un cliente: llenar el paso 1, guardar, mirar el inventario, esperar y repetir. |
| **Consulta (o ciclo)** | Una vuelta completa de ese ciclo. |
| **Sesión** | El navegador abierto de un cliente, con la sesión iniciada en la plataforma. |
| **Seccional** | La oficina donde se tramita (por ejemplo "16 - Usaquén, Bogotá D.C."). |
| **Inventario** | La lista de armas disponibles que muestra el paso 2. |
| **Objetivo** | Un arma que el cliente quiere, descrita con palabras clave (por ejemplo `glock, g-43`). |
| **Alternativa** | Un arma de reemplazo que se acepta si no aparece ningún objetivo. |
| **Comodín** | Un *tipo* de arma (por ejemplo `Pistola`) que se acepta como último recurso. |
| **Alerta** | El aviso que exige tu atención: ventana, sonido y voz. |
| **Reconocer** | Decirle al programa "ya lo vi": detiene el sonido y cierra la ventana de esa alerta. |
| **Simulador** | Un portal de práctica incluido en el programa: no se conecta a la plataforma real. |
| **Portal real** | El modo que usa la plataforma SIAE de verdad, con tu navegador. |

---

## 4. Primer arranque: el asistente

La primera vez que abres Monitor SIAE (o si no hay ningún cliente configurado) aparece el **asistente de
bienvenida**. No vuelve a aparecer cuando ya tienes clientes. Se recorre con **Siguiente** y **Atrás**.

| Paso | Qué pide | Detalle |
|---|---|---|
| **Bienvenido** | Nada | Explica para qué sirve el asistente. |
| **Elige el navegador** | Chrome o Edge | El navegador instalado que usará el programa. |
| **Crea el primer cliente** | El nombre del cliente | Obligatorio. Ejemplo: *Ana López*. Los demás datos los completas después en Ajustes. |
| **Elige el entorno del portal** | Simulador o Portal real | Si estás aprendiendo, elige **Simulador**. Cuando estés listo, cambia a **Portal real** en Ajustes. |
| **Todo listo** | Resumen | El botón final es **Empezar a usar Monitor SIAE** y guarda esa configuración inicial. |

Puedes **Cancelar** en cualquier momento y el asistente no guardará nada. Después de terminarlo, el cliente
queda creado pero **incompleto** (solo tiene nombre): hay que completar sus datos en **Ajustes → Clientes**
antes de poder monitorear (sección 8.3).

---

## 5. Recorrido por la ventana

A la izquierda hay una barra con cuatro botones:

| Botón | Para qué sirve |
|---|---|
| **Panel** | La pantalla principal: estado de cada cliente y los botones para iniciar, detener y atender alertas. |
| **Bitácora** | El registro de lo importante que ha pasado (oportunidades, apartados, fallos…). |
| **Diagnóstico** | Revisa que el equipo y la configuración estén bien. |
| **Ajustes** | Toda la configuración: general, clientes y alarmas. |

Si estás en **Ajustes** con cambios sin guardar y pulsas otro botón, el programa te pregunta si quieres
descartarlos (ver sección 8.1).

---

## 6. El Panel

El Panel se titula **Centro de monitoreo** ("Estado y actividad de cada cliente, en tiempo real").

### 6.1 Botones de la parte superior

| Botón | Qué hace |
|---|---|
| **Reconocer todas** | Reconoce **todas** las alertas activas: cierra la ventana de alertas y detiene el sonido y la voz. **No detiene ningún monitoreo.** Muestra un mensaje como *"Se reconocieron 2 alertas. Se cerraron las notificaciones y se detuvieron las repeticiones; el monitoreo no se detuvo."* Si no había alertas dice *"No había alertas activas."* |
| **Detener todos** | Detiene el monitoreo de **todos** los clientes que lo tienen en curso. Antes pide confirmación (ver abajo). Está deshabilitado (gris) si no hay ningún monitoreo en curso. |
| **Probar alarma** | Lanza una alerta de prueba ("Alarma de prueba: si escuchas la sirena o la voz, los canales locales están activos"). Sirve para comprobar el volumen. Reconócela para silenciarla. |

**La ventana de confirmación de "Detener todos"** dice, por ejemplo: *"Se detendrá el monitoreo de 2
clientes: Ana, Luis. Las sesiones de los navegadores seguirán abiertas y podrás reanudar cada monitoreo con
Iniciar."* Tiene dos botones: **Detener todos** y **Cancelar**. **Cancelar es el botón por defecto**, así que
pulsar Enter sin pensar no detiene nada. Detener no cierra los navegadores ni las sesiones.

### 6.2 La franja de modo

Justo debajo del título hay una línea con un punto azul que indica el entorno:

- **"Simulador · pruebas sin conexión ni acciones en SIAE."** Nada de lo que hagas afecta a la plataforma real.
- **"Portal real · inicia sesión en el navegador de cada cliente antes de monitorear."** Estás trabajando con la
  plataforma de verdad.

### 6.3 Las cuatro tarjetas de resumen

| Tarjeta | Qué cuenta |
|---|---|
| **Clientes** | Cuántos clientes hay configurados. |
| **Monitoreando** | Cuántos están en este momento consultando el inventario. |
| **Sesiones abiertas** | Cuántos tienen su navegador abierto (en portal real). |
| **Alertas activas** | Cuántas alertas están sin reconocer. |

Se actualizan solas cada segundo.

### 6.4 La lista de clientes

Cada cliente es una fila. De izquierda a derecha:

1. **Barra de color.** Cambia según el estado: azul (trabajando), verde azulado (sesión abierta), ámbar
   (esperando horario), naranja (oportunidad), verde (éxito), rojo (problema), gris (neutral). Parpadea
   brevemente cuando algo cambia.
2. **Identidad.** Un círculo con la inicial, el nombre y una línea:
   - **"Activo · Apartar automáticamente"** o **"Activo · Solo alertar"**: indica el modo del cliente.
   - **"Inactivo · se puede editar en Ajustes"**: el cliente está desmarcado en Ajustes y no se puede iniciar.
3. **Actividad.** Dos líneas: arriba, lo que está haciendo; abajo, el **último resultado** y, a la derecha,
   hace cuánto ocurrió ("ahora", "hace 5 min"…).
4. **Etiqueta de estado** (la píldora de color). Ver la tabla de abajo.
5. **Botón principal.** Cambia según la situación (ver 6.6).
6. **Botón ⋯ (más acciones).** Ver 6.7.

Si el cliente tiene una oportunidad pendiente, **toda la fila se resalta**.

Cuando no hay clientes se ve: *"Aún no hay clientes. Agrega el primero desde Ajustes para empezar."*

### 6.5 Los estados de un cliente

Cada estado muestra una etiqueta y una frase de actividad:

| Etiqueta | Actividad | Qué significa | Qué hacer |
|---|---|---|---|
| **Listo** (gris) | Cliente preparado | Todavía no se ha hecho nada con este cliente. | Inicia el monitoreo (en portal real, antes abre la sesión). |
| **Abriendo navegador** | Abriendo sesión del portal | El programa está abriendo el navegador de este cliente. | Espera unos segundos. |
| **Sesión abierta** (verde azulado) | Sesión del portal abierta | El navegador está abierto. | **Inicia sesión y resuelve el captcha tú mismo**; luego pulsa **Iniciar / reanudar**. |
| **Sesión cerrada** (gris) | Sesión del portal cerrada | El navegador se cerró (lo cerraste tú o se cayó). | Pulsa **Abrir sesión** de nuevo. |
| **Iniciando** (azul) | Iniciando monitoreo | Está arrancando el monitoreo. | Espera un instante. |
| **Monitoreando** (azul) | Consultando inventario | El monitoreo está en marcha. | Nada: espera la alerta. |
| **Fuera del horario** (ámbar) | En espera del horario configurado | Estás dentro de un horario limitado y ahora está fuera de franja. El monitoreo sigue vivo pero **no consulta**. | Nada: retoma solo (revisa cada ~30 segundos). El mensaje dice cuándo vuelve. |
| **Alerta encontrada** (naranja) | Inventario disponible; alerta generada | Hay un arma que cumple y el cliente está en modo **Solo alertar** (o no se pudo apartar). | **Ve ya al navegador** y selecciona el arma tú. |
| **Arma apartada** (verde) | Arma apartada; monitoreo finalizado | El programa apartó el arma. | **Ve ya al navegador y termina el trámite.** |
| **Deteniendo** (azul) | Deteniendo monitoreo | Se pidió detener y está terminando. | Espera un instante. |
| **Detenido** (gris) | Monitoreo detenido | El monitoreo no está corriendo. | Reanuda con **Iniciar / reanudar** si quieres. |
| **Error** (rojo) | Se produjo un error | Algo falló al abrir el navegador o durante el monitoreo. | Lee el mensaje de la fila y de la bitácora; ver la sección 15. |

### 6.6 El botón principal de cada cliente

Su texto depende de la situación:

| Texto del botón | Cuándo aparece | Qué hace |
|---|---|---|
| **Abrir sesión** | Portal real y el cliente no tiene el navegador abierto. | Abre el navegador de ese cliente en la página del trámite. |
| **Iniciar / reanudar** | El cliente puede empezar o retomar. | Arranca el monitoreo. |
| **Iniciando…** / **Deteniendo…** (deshabilitado) | Mientras se arranca o se detiene. | — |
| **Monitoreando** (deshabilitado) | El monitoreo está en marcha (o esperando horario). | Para detenerlo usa el menú ⋯. |
| **Ir al navegador** | Portal real, con una oportunidad pendiente y la sesión abierta. | Trae al frente el navegador de ese cliente. |

### Dónde empieza el monitoreo: "Nueva solicitud" o "Solicitud existente"

Junto al botón **Iniciar / reanudar** hay un selector con dos opciones:

- **Nueva solicitud (paso 1)**: busca inventario desde el paso 1, como siempre.
- **Solicitud existente**: va al módulo **Estado de trámite**, abre la solicitud que ya quedó en progreso y
  busca el inventario desde ahí, sin volver a llenar seccional, tipo de trámite ni descripción. **Si no la
  encuentra, no hace el paso 1 por su cuenta**: se detiene y muestra *"No se encontró la solicitud…"* en la fila
  y en un aviso. Tú decides: revisas el portal o cambias el selector a *Nueva solicitud (paso 1)* y vuelves a iniciar.

Al elegir **Solicitud existente** aparece el campo **N.º de solicitud (opcional)**. Escribe el número tal como
sale en la tarjeta de Estado de trámite, por ejemplo `1-01-45-202610-4336` (si pegas la línea completa,
*"1-01-45-202610-4336 - EN PROGRESO"*, se toma solo el número). Sirve cuando hay más de una solicitud abierta,
por ejemplo de otra seccional. Si lo dejas vacío, se toma la primera solicitud *EN PROGRESO* con el mismo permiso
y tipo de trámite del cliente, y **desde ese momento se vigila siempre esa misma** (su número aparece en la fila del
cliente: *"Solicitud 1-01-45-… en Estado de trámite: sin inventario por ahora."*). Así, si después aparece otra
solicitud del mismo tipo, el programa no salta a ella.

**Por qué existe.** Cuando el paso 1 se guarda y el portal sí tiene inventario pero **ninguna arma
cumple tus objetivos**, la solicitud ya quedó abierta en Estado de trámite. Repetir el paso 1 no tiene
sentido: lo que corresponde es volver a abrir esa solicitud y mirar si apareció inventario. El programa lo
hace solo: tras ese caso pasa a vigilar la solicitud (el selector cambia a *Solicitud existente*). Usa el
selector a mano cuando reinicias el programa o quieres retomar una solicitud que dejaste abierta.

**Si la solicitud tarda en aparecer.** El portal puede tardar unos segundos en mostrar en Estado de trámite la
solicitud que acaba de crear el paso 1. El programa espera hasta unos 20 segundos, **recarga la página** y espera
otros 20. Si aun así no aparece, **se detiene** (no crea otra solicitud por su cuenta), suena una alerta de problema
y el aviso del Panel explica qué hacer:
1. Abre «Estado de trámite» en el navegador del cliente y busca una tarjeta EN PROGRESO de su trámite.
2. Si la ves: elige «Solicitud existente», escribe su número y pulsa «Iniciar / reanudar».
3. Si no está: deja «Nueva solicitud (paso 1)» y pulsa «Iniciar / reanudar»; el programa vuelve a buscar desde
   el paso 1.

**Cómo trabaja desde Estado de trámite.** La primera vez abre el módulo, elige la solicitud *EN PROGRESO* y pulsa
la línea "FG-1…" (el portal busca inventario). En las vueltas siguientes **no recarga nada**: mientras esa línea
siga en pantalla, solo vuelve a pulsarla. Si la página se recarga o sales de ahí, repite el recorrido completo.
Tras pulsar la línea espera:
- si aparece el listado, aplica tus objetivos, alternativas y comodín igual que en el paso 1;
- si dice *"No hay inventario disponible por parte de Indumil."*, cierra el cuadro haciendo clic **fuera de
  él** y espera al siguiente ciclo.

**Cuando hay inventario en Estado de trámite.** Elige el arma según tus reglas, pulsa PORTE o TENENCIA y atiende
lo que muestre el portal:
- Si sale *"¿Está seguro de adquirir el siguiente arma?"*, lo confirma y se comporta como en el paso 1: con
  *pre-asignar* se detiene ahí; con *sellar* sigue con FINALIZAR y el cuadro *"¿Desea confirmar y continuar…?"*.
- Si **no** sale ese cuadro (o no hay botones PORTE/TENENCIA), pulsa **FINALIZAR del listado** y confirma el cuadro
  *"Ver armas"*. En este camino lo hace **igual con *pre-asignar* que con *sellar***.
- Si sale directamente *"Ver armas"*, lo confirma ahí.
- Si aparece un cuadro que no reconoce, **no confirma nada**: informa del error con una captura y deja la
  solicitud como está. FINALIZAR solo se pulsa con una fila seleccionada y sin el aviso de "sin inventario".
- Antes de pulsar FINALIZAR comprueba que **lo seleccionado sea el arma elegida**: el portal lo muestra con la fila
  marcada, con el contador "1 fila seleccionada" o con el resumen del arma. Si la fila marcada es otra, no
  confirma nada.

La alarma suena en Estado de trámite con las mismas reglas que en el paso 1: cuando el programa **encuentra un arma
que cumple** (la aparta, o avisa en modo alerta), cuando el apartado falla y **una vez** cuando aparece inventario
sin ningún arma de interés (sección 10).

> **Importante.** El programa **nunca pulsa FINALIZAR** en ese cuadro sin inventario, porque en ese momento
> **cancela la solicitud**. Tampoco toca el botón *Cancelar trámite*. Si no logra cerrar el cuadro, se detiene
> con un error y una captura antes que arriesgar la solicitud. Haz lo mismo cuando navegues a mano: cierra
> ese cuadro con un clic fuera.

El botón **Iniciar / reanudar** aparece **gris (deshabilitado)** si:
- el cliente está **inactivo** (se activa en Ajustes → Clientes → "Cliente incluido en el monitoreo");
- hay **errores de configuración** (sale un aviso: *"Completa la configuración antes de iniciar el monitoreo."*);
- en portal real, **no has abierto la sesión** de ese cliente.

Pasa el mouse sobre el botón para ver el motivo exacto.

### 6.7 El menú ⋯ (más acciones)

| Opción | Qué hace | Cuándo está disponible |
|---|---|---|
| **Detener monitoreo** | Detiene el monitoreo de ese cliente. La sesión del navegador **queda abierta**. | Solo si está iniciando, monitoreando o esperando horario. |
| **Ver historial de inventario** | Abre el historial de consultas con inventario (sección 13). | Solo si ya hay historial. |
| **Ir al navegador** | Trae al frente el navegador del cliente. | Solo en portal real y con la sesión abierta. |
| **Editar cliente** | Abre Ajustes directamente en la pestaña Clientes, con ese cliente seleccionado. | Siempre. |

### 6.8 Avisos que aparecen en el Panel

Debajo de las tarjetas pueden aparecer franjas de aviso. Las de color tienen una **X** para cerrarlas.

| Color | Ejemplo de mensaje | Cuándo |
|---|---|---|
| Azul | *"Ana: abriendo el navegador del perfil."* | Al abrir una sesión. |
| Verde | *"Ana: navegador abierto. Completa el inicio de sesión manualmente."* | Sesión abierta. |
| Azul | *"Ana: navegador cerrado. Puedes abrir una nueva sesión."* | Se cerró el navegador. |
| Azul | *"Ana: monitoreo detenido; la sesión del navegador permanece abierta."* | Detuviste el monitoreo. |
| Azul | *"Ana: Fuera del horario configurado (hoy lunes: 07:00 a 10:00). Retomará la consulta mañana a las 07:00."* | Una sola vez, cuando el cliente entra en "Fuera del horario". |
| Verde | *"Ana: solicitud de enfoque enviada. Si Windows no la trae al frente, selecciona su icono en la barra de tareas."* | Al pedir **Ir al navegador**. |
| Rojo | *"Ana: …"* con el detalle del problema | Error al abrir el navegador o durante el monitoreo. |
| Amarillo claro (banner) | *"Completa la configuración antes de iniciar el monitoreo."* | Hay datos faltantes o inválidos. |
| Verde claro (banner) | Confirmaciones de **Reconocer todas** y **Detener todos**. | Tras usar esos botones. |

> **Sobre "Ir al navegador".** Windows puede negarse a traer una ventana al frente. Si no aparece, el icono del
> navegador parpadea en la barra de tareas: haz clic en él.

### 6.9 Qué significan los mensajes de la segunda línea (último resultado)

Esta línea cuenta qué pasó en la última consulta. Los más comunes:

| Mensaje | Significado | Qué hacer |
|---|---|---|
| *Iniciando y preparando una nueva consulta.* | Está arrancando. | Nada. |
| *Consultando el inventario.* | Está en una consulta. | Nada. |
| *Horario habilitado; consultando el inventario.* | Volvió a estar dentro del horario. | Nada. |
| *Fuera del horario configurado (hoy lunes: 07:00 a 10:00). Retomará la consulta mañana a las 07:00.* | No consulta ahora. Dice el horario de hoy y cuándo vuelve. | Nada, o amplía el horario en Ajustes. |
| *Seccionales cerradas.* | La plataforma no ofrece ninguna seccional abierta ahora. | Nada: sigue intentando. |
| *Abiertas pero no objetivo: …* | Hay seccionales abiertas, pero **ninguna** es de las que pediste. | Revisa que la seccional del cliente esté bien escrita (sección 8.3). |
| *Sin inventario en Medellín.* | La seccional está abierta pero no hay armas. Si probó varias, las lista separadas por " \| ". | Nada: sigue intentando. |
| *Inventario en Medellín (14 armas) sin armas de interés.* | Hay armas, pero ninguna cumple tus reglas. | Revisa objetivos, alternativas y comodín (sección 9). |
| *Arma disponible en Medellín: …* | Modo **Solo alertar**: hay un arma que cumple. Se detiene el monitoreo de ese cliente. | Ve al navegador y selecciónala. |
| *Arma apartada en Medellín: …* | Modo **Apartar**: se apartó el arma. Se detiene el monitoreo de ese cliente. | Ve al navegador y termina el trámite. |
| *… · la reserva no quedó sellada, pulsa FINALIZAR* | Se apartó, pero el cierre final **no** se hizo (por la configuración del paso 2 o porque no se confirmó). | En el navegador, pulsa **FINALIZAR** y confirma. |
| *No se pudo apartar tras 2 intentos: …* | Había un arma, pero la plataforma no confirmó el apartado (probablemente otra persona la tomó). | Revisa el navegador de inmediato. El monitoreo **no** se detiene solo. |
| *Sesión vencida: …* | La plataforma cerró la sesión. Se detiene el monitoreo. | Inicia sesión de nuevo en el navegador y reanuda. |
| *Opción del trámite no disponible: …* | Alguno de los textos de "cómo obtener el permiso" o "tipo de trámite" no aparece en la plataforma. | Corrige esos textos en Ajustes → Clientes (deben ser iguales a los del portal). |
| *El portal no respondió: GUARDAR no tuvo respuesta…* | El servidor del portal no contestó (por ejemplo un error 504). No es un fallo del programa. | No hagas nada: el monitoreo vuelve a intentarlo con la espera normal y **no se detiene** por esto. |
| *Respuesta inesperada de la plataforma: … (captura: fallo-….png)* | La página no se comportó como se esperaba. Se guardó una captura. | Mira la captura en la Bitácora (sección 11). Tras **tres seguidas**, el monitoreo se detiene. |
| *Monitoreo detenido. La sesión del navegador permanece abierta.* | Detuviste el monitoreo. | — |
| *Abriendo el navegador del cliente.* | Se está abriendo la sesión. | Espera. |
| *Navegador abierto. Inicia sesión y resuelve el captcha manualmente.* | Es tu turno. | Inicia sesión en el navegador. |
| *El navegador se cerró. Abre una nueva sesión para volver a conectar este cliente.* | El navegador ya no está. | Pulsa **Abrir sesión**. |
| *No se pudo abrir el navegador. …* | Falló la apertura. | Revisa que Chrome/Edge esté instalado (Diagnóstico). |
| *Error durante el monitoreo. …* | Un fallo inesperado detuvo el monitoreo. | Mira la bitácora y el Diagnóstico. |

---

## 7. Cómo trabajar paso a paso

### 7.1 Practicar con el simulador (sin riesgo)

1. En **Ajustes → General**, deja **Entorno del portal = Simulador (pruebas)**.
2. Elige un **escenario** (sección 8.2). El predeterminado, *"Sin inventario y luego aparece"*, es el mejor
   para aprender.
3. En **Ajustes → Clientes**, completa los datos del cliente (sección 8.3).
4. Vuelve al **Panel** y pulsa **Iniciar / reanudar**. Verás cómo cambia el mensaje de la fila hasta que
   "aparece" inventario y suena la alarma.
5. Pulsa **Entendido** en la ventana de alertas (o **Reconocer todas**) para silenciarla.

No necesitas abrir sesión: el simulador no usa navegador.

### 7.2 Trabajar con el portal real

1. En **Ajustes → General**, cambia **Entorno del portal** a **Portal real (sesión manual)** y guarda. Si había
   monitoreos en marcha, deténlos antes de cambiar de entorno.
2. En el **Panel**, pulsa **Abrir sesión** en el cliente. Se abre Chrome o Edge con un perfil propio de ese
   cliente y se carga la página del trámite.
3. **En ese navegador, inicia sesión en la plataforma y resuelve el captcha.** El programa no lo hace por ti
   y no guarda tus contraseñas.
4. Cuando termines, vuelve al Panel y pulsa **Iniciar / reanudar**.
5. Deja el navegador abierto (puedes minimizarlo). **Evita navegar dentro de esa ventana** mientras el programa
   monitorea: el programa la está usando.
6. Cuando suene la alarma, pulsa **Ir al navegador** (en la ventana de alertas o en el Panel), y completa
   el trámite a mano.

> Cada cliente tiene su propio navegador y su propio perfil. Con varios clientes verás varias ventanas.

### 7.3 Cuando suena la alarma

1. **Para la alarma** pulsando **Entendido** en la ventana de alertas (o **Reconocer todas** en el Panel).
2. **Ve al navegador** del cliente (botón **Ir al navegador**).
3. **Termina el trámite** a mano. Si el mensaje decía que la reserva no quedó sellada, pulsa **FINALIZAR** y
   confirma.
4. Cuando termines, si quieres seguir monitoreando a ese cliente, pulsa **Iniciar / reanudar**.

---

## 8. Ajustes

Abre **Ajustes** desde la barra lateral. Tiene tres pestañas: **General**, **Clientes** y **Alarmas**.

### 8.1 Cómo funciona la pantalla de Ajustes

- **Contraseña.** Si activaste la protección (8.2, Seguridad), al abrir Ajustes se pide la contraseña
  (*"Ingresa la contraseña de la aplicación para continuar."*). Si es incorrecta, aparece *"Contraseña
  incorrecta"*. La primera vez que activas la protección debes crear una contraseña.
- **Guardar cambios.** Los cambios **no se aplican hasta que pulses Guardar cambios** (abajo a la derecha).
  Si todo está bien, aparece *"Cambios guardados correctamente. Puedes seguir en Ajustes o volver al Panel."*
  y te quedas en Ajustes.
- **Si hay errores,** aparece una ventana *"No se pudo guardar la configuración"* con la lista de lo que
  falta. Tus cambios siguen en pantalla para que los corrijas.
- **Cancelar.** Vuelve al Panel. Si hay cambios sin guardar, pregunta *"¿Salir de Configuración? Los
  cambios que no guardaste se perderán."* con los botones **Descartar cambios** y **Cancelar**.
- **Al cambiar de pantalla o cerrar la aplicación** con cambios sin guardar, aparece la misma pregunta.
- Se hace una **copia de respaldo** automática (`config.json.bak`) cada vez que guardas.
- Algunos cambios rigen **desde el siguiente inicio** de monitoreo (intervalo, variación, escenario). Los
  monitoreos que ya están corriendo conservan los valores con los que arrancaron.

### 8.2 Pestaña General

#### Portal y navegador

| Opción | Qué hace |
|---|---|
| **Navegador preferido** | **Chrome** o **Edge**. Es el navegador que el programa abre para cada cliente. Debe estar instalado. |
| **Entorno del portal** | **Simulador (pruebas)**: portal de práctica, sin conexión a SIAE. **Portal real (sesión manual)**: la plataforma verdadera; tú inicias sesión. Con el portal real, abre e inicia sesión manualmente en el navegador de cada cliente antes de monitorear. |
| **Escenario del simulador** | Solo aparece con el simulador. Elige qué le pasa al monitor (tabla de abajo). Cada vez que inicias un monitoreo, el escenario arranca desde el principio. |

**Escenarios del simulador**

| Escenario | Qué ocurre | Sirve para probar… |
|---|---|---|
| **Hay inventario desde la primera consulta** | Encuentra armas de inmediato. | La selección y la alarma, sin esperar. |
| **Sin inventario y luego aparece** *(predeterminado)* | Seccionales cerradas, luego abiertas sin inventario y, a la quinta consulta, inventario. | Ver la espera y luego la selección. |
| **Hay inventario, pero sin los objetivos del cliente** | Aparece inventario sin ninguno de tus objetivos. | Cómo actúan las alternativas y el comodín. |
| **El arma aparece pero otra persona la toma** | Apartar no se confirma. | Los reintentos y el aviso de apartado fallido. |
| **La plataforma falla tres veces seguidas** | Una consulta cerrada, una sin inventario y tres errores. | La detención por fallos repetidos y su alarma. |
| **La sesión vence a mitad del monitoreo** | Tras dos consultas, la plataforma pide iniciar sesión. | La alarma de sesión vencida. |
| **Las seccionales siempre están cerradas** | Todas las consultas encuentran todo cerrado. | Dejarlo corriendo para probar panel, horario y bitácora. |

El inventario simulado tiene 15 armas con nombres al estilo de la plataforma (Glock G-43, G-26 y G-19, CZ P10,
Cordova, Jericho, Taurus, Beretta, Sig Sauer, Smith & Wesson, Canik, Walther, un Ruger de .22 LR y un
revólver), para que puedas probar objetivos, alternativas, comodín y exclusiones.

#### Frecuencia y disponibilidad

| Opción | Qué hace |
|---|---|
| **Intervalo entre consultas** | Cuántos **segundos** espera el programa entre una consulta y la siguiente, de 2 a 3600. Junto al número ves su equivalencia en minutos (por ejemplo, 180 = 3 min). Un intervalo corto consulta más seguido; uno largo es más discreto. |
| **Variación aleatoria de cada espera** | Un porcentaje de 0 a 50 % (20 % por defecto). Cada espera dura un valor al azar dentro de ese margen alrededor del intervalo. Explicación abajo. |

**Variación aleatoria (jitter).** Con intervalo 180 s y variación 20 %, cada espera dura al azar entre **144 s y
216 s** (el 20 % de 180 es 36, que se **resta o suma** al azar). El promedio sigue siendo 180 s. La línea de
ayuda de la pantalla hace esa cuenta por ti. Sirve para que las consultas no sigan un ritmo fijo, fácil de
distinguir del de una persona. Con **0 %** cada espera dura exactamente el intervalo.
El tiempo real entre consultas es la espera **más** lo que tarda cada consulta (cargar el formulario, probar
cada seccional).

También se aplica a la **espera creciente tras un fallo**: la primera falla espera ≈ 2 × intervalo, la segunda
≈ 3 × intervalo, y a la tercera el monitoreo se detiene.

**Horario de consulta**

| Opción | Qué hace |
|---|---|
| **Limitar el monitoreo a un horario** (casilla) | Es el **interruptor general**. **Desactivado (por defecto)**: el programa consulta a cualquier hora y la grilla de abajo **no se aplica** (se ve atenuada). **Activado**: solo consulta dentro de las horas de cada día marcado. |
| **Días y horas** | Una columna por día (Lun a Dom). Marca la casilla de cada día que quieras monitorear y escribe sus horas en **Desde** y **Hasta** (se muestran en 12 horas, como `08:00 a. m.` y `04:00 p. m.`; puedes escribirlas así, como `4:00 pm` o en 24 horas, como `16:00`). La franja azul muestra gráficamente las horas. Un día sin marcar **no se consulta**. |
| **Copiar Lunes a todos los días** | Aplica las horas de Lunes a toda la semana y marca todos los días. |

- La **hora de inicio debe ser anterior** a la de fin; no se admiten horarios que crucen la medianoche.
- Las horas son **hora local de Colombia** (America/Bogota).
- Dentro de la franja, el inicio es inclusivo y el final exclusivo (`08:00` a `16:00` consulta desde las 8:00:00
  hasta las 15:59:59).
- Cada día puede tener un rango distinto (por ejemplo, lunes a viernes de 7:00 a 19:00 y sábado de 9:00 a 12:00).

**Qué no hace el horario.** El horario solo decide **cuándo se consulta**. No impide **abrir sesión**, no bloquea
el botón de iniciar y **nunca silencia un hallazgo**: si una consulta trae inventario, la alerta suena siempre. Si
inicias un monitoreo fuera de horario, queda en el estado **Fuera del horario**, esperando, y arranca solo cuando
llega la franja.

#### Seguridad

| Opción | Qué hace |
|---|---|
| **Solicitar contraseña para editar la configuración** | Si la activas, hay que ingresar una contraseña para abrir Ajustes. Al activarla por primera vez, el programa pide **crear** una (mínimo 6 caracteres, escrita dos veces). La contraseña queda **preparada** y solo se aplica al pulsar **Guardar cambios**. |
| **Cambiar contraseña** | Pide la contraseña actual, la nueva y su confirmación. Tampoco se aplica hasta guardar. |

Para **desactivar** la protección hay que conocer la contraseña actual. Si **cancelas** o descartas los cambios, la
contraseña anterior se conserva. Esta contraseña protege **Ajustes** de la aplicación; no tiene relación con la
de la plataforma SIAE.

### 8.3 Pestaña Clientes

A la izquierda está la lista de **Perfiles**, con los botones **+ Agregar cliente** y **Eliminar seleccionado**
(este último pide confirmación: *"¿Eliminar el perfil de …?"*). Un cliente nuevo aparece como "Nuevo cliente"
hasta que le pongas nombre. A la derecha está el formulario del cliente seleccionado.

#### Datos básicos

| Campo | Qué es |
|---|---|
| **Nombre para mostrar** | El nombre del cliente en el Panel, las alertas, la voz y la bitácora. Obligatorio. |
| **Cliente incluido en el monitoreo** (casilla) | Si la desmarcas, el cliente queda **inactivo**: sigue configurado pero no se puede iniciar. |
| **Sonido de alertas** | Cómo suena la alarma **de este cliente**: **Seguir el ajuste general** (usa lo que diga la pestaña Alarmas), **Forzar sonido para este cliente** (suena aunque el sonido general esté desactivado) o **Silenciar solo este cliente** (no suena su sirena; los avisos visuales y los demás clientes no cambian). Debajo, una línea explica cómo queda con tu ajuste actual. |

#### Paso 1 · Datos del trámite

Son los datos que el programa escribe en el paso 1 de la plataforma. **Los textos deben ser exactamente los
que muestra el portal** (no se distinguen mayúsculas ni tildes, pero sí las palabras).

| Campo | Qué es |
|---|---|
| **Cómo obtener el permiso (texto del portal)** | La opción de la primera lista del portal. Ejemplo: `Defensa Personal`. |
| **Tipo de trámite (texto del portal)** | La opción de la segunda lista. Ejemplo: `Armas personas naturales: Adquisición de arma`. |
| **Seccionales preferidas (de mayor a menor prioridad)** | Las seccionales que aceptas. Escribe una y pulsa **Agregar** (o Enter). Usa las flechas **↑ ↓** para cambiar el orden y **Quitar** para borrar. Pueden ser parciales: `Bogotá` coincide con "16 - Usaquén, Bogotá D.C.". Se prueban **todas** en cada consulta, en tu orden. |
| **Descripción del trámite** | El texto que se escribe en el formulario. Debe tener **entre 150 y 500 caracteres**; debajo hay un contador ("160 / 500 caracteres"). |

#### Selección del arma

| Campo | Qué es |
|---|---|
| **Reacción al encontrar un arma** | **Apartar el arma automáticamente**: el programa la aparta (paso 2) y luego te avisa. **Solo alertar (yo selecciono el arma)**: solo te avisa; tú seleccionas el arma en el portal. Una línea de ayuda cambia según tu elección. |

Los campos de esta sección y la siguiente **aparecen u ocultan** según la reacción:

| Campo | Solo se muestra… | Qué es |
|---|---|---|
| **Modalidad (botón que se pulsa en el portal)** | Si la reacción es **Apartar** | **PORTE** o **TENENCIA**: el botón que el programa pulsa después de seleccionar el arma. |
| **Departamento / Ciudad / Dirección completa** | Si Apartar **y** modalidad **TENENCIA** | La dirección que exige el modal de TENENCIA. Los tres son obligatorios en ese caso. |
| **Reintentos al apartar** | Si la reacción es **Apartar** | De 1 a 5 (2 por defecto). Cuántas veces intenta apartar si la plataforma no lo confirma. **Antes de cada reintento vuelve a leer el listado y a elegir**: si el arma sigue ahí, la intenta de nuevo; si desapareció, pasa a la siguiente que cumpla (otro objetivo, una alternativa o el comodín); si ya no queda ninguna, deja de intentar y avisa del apartado fallido. **Recomendado: 2**, para que si algo falla la tabla quede en tus manos cuanto antes. |

En modo **Solo alertar** esos campos se ocultan y la ayuda lo explica: *"no hay modalidad, reintentos ni
paso 2 que configurar"*. Lo que ocultas **no se borra**: si vuelves a Apartar, reaparece lo que habías escrito.

**Las armas: objetivos, alternativas, comodín y exclusiones.** Se explican con detalle en la sección 9. Aquí, qué
significa cada campo:

| Campo | Qué es |
|---|---|
| **1 · Objetivos prioritarios (se prueban en este orden)** | Las armas que quieres, en orden de preferencia. Cada fila son **palabras clave separadas por coma que deben aparecer todas** en el nombre del arma. Ejemplo: `glock, g-43`. |
| **Si no aparece ningún objetivo, ¿qué hacer?** | Define hasta dónde llega la búsqueda cuando ningún objetivo está (cuatro opciones; ver abajo). Una línea de ayuda explica el orden de la opción elegida. |
| **2 · Alternativas (se prueban en este orden)** | Armas de reemplazo, con el mismo formato que los objetivos. Opcional. |
| **3 · Tipo comodín: cualquier arma de este tipo (último recurso)** | Un *tipo* de arma, por ejemplo `Pistola`. Toma cualquier arma de ese tipo, sea cual sea su modelo. Vacío = sin comodín. |
| **Excluir** | Palabras que **descartan** un arma aunque coincida con un objetivo. Ejemplo: `22 lr`. |

Las cuatro opciones de **"Si no aparece ningún objetivo"**:

| Opción | Qué hace |
|---|---|
| **Solo objetivos: si no hay ninguno, seguir esperando** | No toma nada y sigue consultando. Alternativas y comodín no se usan. |
| **Objetivos → alternativas (sin comodín)** | Prueba las alternativas; si ninguna coincide, no toma nada. |
| **Objetivos → comodín (sin alternativas)** | Salta las alternativas y toma la primera arma del tipo comodín. |
| **Objetivos → alternativas → comodín** | Prueba las alternativas y, si ninguna coincide, toma la primera arma del tipo comodín. |

Las listas que la opción elegida **no usa** se ven atenuadas y no se pueden editar. Cambia la opción para
editarlas; no pierdes lo que habías escrito.

#### Paso 2 · Pre-asignación y cierre del trámite

*(Solo se muestra con la reacción **Apartar**.)*

| Campo | Qué es |
|---|---|
| **Alcance del paso 2** | **Pre-asignar el arma y dejar el trámite para revisión manual** *(predeterminado)*: aparta el arma (CONFIRMAR del primer cuadro) y se detiene; **tú** pulsas **FINALIZAR** y confirmas. **Pre-asignar el arma y sellar el trámite automáticamente**: además pulsa **FINALIZAR** y el CONFIRMAR del segundo cuadro, y el monitoreo se detiene; no rellena nada de lo que el portal muestre después (Formulario antecedentes Fiscalía). |

#### Ejemplo completo de un cliente

> **Nombre:** Ana López · **Incluido:** sí · **Sonido:** seguir el ajuste general
> **Permiso:** `Defensa Personal` · **Trámite:** `Armas personas naturales: Adquisición de arma`
> **Seccionales:** `Medellín`, `Bogotá` · **Descripción:** (160 caracteres)
> **Reacción:** Apartar · **Modalidad:** PORTE · **Reintentos:** 2
> **Objetivos:** `glock, g-43` y luego `glock, g-26` · **Si no hay objetivo:** Objetivos → alternativas → comodín
> **Alternativas:** `cordova, compacta` · **Comodín:** `Pistola` · **Excluir:** `22 lr`
> **Paso 2:** sellar el trámite automáticamente *(opción que se elige a propósito)*
>
> Resultado: el programa prueba Medellín y luego Bogotá. Si hay un Glock G-43, lo aparta; si no, un Glock G-26;
> si tampoco, una Cordova compacta; si tampoco, la primera pistola que no sea .22 LR; y si no hay ninguna,
> sigue esperando.

### 8.4 Pestaña Alarmas

Aquí se configura cómo te avisa el programa. *Silenciar el audio no detiene el monitoreo ni oculta las alertas
visuales.*

#### Canales de aviso

| Opción | Qué hace |
|---|---|
| **Sonido para clientes que heredan el ajuste general** | Activa o desactiva la sirena para los clientes cuyo "Sonido de alertas" sea *Seguir el ajuste general*. Cada cliente puede forzarlo o silenciarlo por su cuenta (8.3). |
| **Leer las alertas en voz alta** | Activa la voz. Este ajuste se aplica a **todos** los clientes. La voz repite cada alerta activa, con una pausa de unos 3 segundos, hasta que se reconozca. |

Ambos vienen activados por defecto.

#### Patrón de sonido

Elige cómo suena la sirena y pulsa **Escuchar 3 segundos** para oírlo (tres ciclos):

| Tono | Descripción |
|---|---|
| **Sirena clásica (agudo y grave)** | El predeterminado. |
| **Doble tono (agudo y grave)** | Más agudo. |
| **Tono suave (agudo y grave)** | Más grave y discreto. |

Cada patrón alterna un tono agudo y uno grave. La alarma real repite el patrón mientras la alerta siga
activa. Las **oportunidades** suenan más agudas y los **problemas** (sesión vencida, errores repetidos), más
graves, para distinguirlas de oído.

#### Escalamiento de insistencia

*Opcional; viene desactivado.* Sirve para que una alerta que **nadie ha reconocido** se vuelva más difícil de
ignorar con el tiempo.

1. Activa la casilla **"Insistir más si una alerta sigue sin reconocerse"**.
2. Define un nivel: **"Cuando lleve [N] minutos sin reconocer"**, y marca qué hacer:
   - **Forzar sonido**: suena aunque el sonido esté silenciado.
   - **Forzar voz**: habla aunque la voz esté desactivada.
   - **Tono**: pasa a otro patrón de sonido.
3. Pulsa **Agregar nivel**. Puedes añadir varios; se aplican por orden de tiempo. **Quitar** borra el nivel
   seleccionado.

*Ejemplo:* nivel 1 a los 2 min: forzar sonido; nivel 2 a los 5 min: forzar sonido y voz, con otro tono. Si
nadie atiende la alerta, a los 2 minutos empieza a sonar aunque estuviera silenciada y a los 5 minutos además
habla. Al reconocer la alerta todo se detiene y el contador vuelve a cero.

---

## 9. Cómo elige el arma el sistema

En cada consulta, cuando el paso 2 trae inventario, el programa decide así:

1. **Descarta** las armas que contengan alguna palabra de **Excluir**. Esas no participan en nada más.
2. **Objetivos, en orden.** Recorre la lista de objetivos del primero al último. El primer objetivo que
   tenga alguna arma coincidente gana, y se toma la **primera arma** que lo cumple. Si el objetivo 1 coincide,
   nunca se mira el 2.
3. **Si ningún objetivo coincide**, se aplica la opción de **"Si no aparece ningún objetivo"**:
   - *Solo objetivos*: no se toma nada.
   - *Objetivos → alternativas*: se recorren las alternativas en orden, igual que los objetivos.
   - *Objetivos → comodín*: se toma la primera arma cuyo **tipo** sea el comodín.
   - *Objetivos → alternativas → comodín*: primero alternativas; si ninguna, el comodín.
4. **Si nada coincide**, no se toma ningún arma y se sigue esperando.

**Cómo coincide una fila de palabras.** Cada objetivo o alternativa es una lista de palabras clave; deben estar
**todas** en el nombre del arma. `glock, g-43` exige las dos palabras. No importan las tildes, las mayúsculas ni
los guiones: `g43`, `G-43` y `g 43` son lo mismo. Las palabras se buscan como palabras completas (`22` no
coincide dentro de `1922`).

**Qué es el comodín.** Es el *tipo* de arma, la primera palabra del nombre en la plataforma ("**Pistola** Glock…",
"**Revólver** Taurus…"). Con comodín `Pistola`, el sistema toma la primera pistola del inventario aunque no sea
ninguno de tus modelos. Es el último recurso y el más arriesgado: puede apartar un arma que no era ninguno de
tus objetivos. Úsalo solo si de verdad aceptas cualquier arma de ese tipo.

**Cómo te enteras de cuál se eligió.** La alerta indica si es el objetivo o un reemplazo:
- *"Pistola Glock… en Medellín."* → era un objetivo.
- *"… (alternativa, el objetivo no estaba)"* → salió de las alternativas.
- *"… (reemplazo por tipo, el objetivo no estaba)"* → salió del comodín.

En el historial de inventario (sección 13) también se ve la regla aplicada: *Objetivo prioritario #1*,
*Alternativa #2* o *Comodín por tipo*.

---

## 10. Las alertas

Una alerta es lo que exige tu atención. Hay dos clases:

| Clase | Cuándo | Cómo se distingue |
|---|---|---|
| **Oportunidad** | Hay un arma apartada, disponible (modo alerta) o con apartado fallido. | Tarjeta naranja; sirena **más aguda**. |
| **Problema** | El monitor se detuvo y te necesita (sesión vencida, errores repetidos). | Tarjeta roja; sirena **más grave**. |

### Qué pasa cuando salta una alerta

- Se abre la ventana **"Monitor SIAE · Alertas"**, siempre al frente, con el encabezado **"Requiere tu
  atención"**.
- Suena la **sirena** en bucle y la **voz** repite el aviso, según tus ajustes de Alarmas.
- El icono del programa **parpadea en la barra de tareas**.
- Una alerta nueva del **mismo cliente reemplaza** a la anterior.
- Cada alerta tiene dos botones: **Ir al navegador** (trae al frente la ventana de ese cliente) y **Entendido**
  (la reconoce).

### Los avisos y qué hacer

| Título | Mensaje | Qué hacer |
|---|---|---|
| **Arma apartada: Ana** | *"… en Medellín. Termine el trámite en la ventana del navegador de este cliente."* | Ve al navegador y termina el trámite. |
| **Arma disponible: Ana** | *"… Selecciónela ya en la ventana del navegador de este cliente."* | Selecciona el arma tú en el portal. |
| **Arma disponible (no se pudo apartar): Ana** | *"La plataforma no confirmó el apartado tras 2 intento(s); revise la ventana del navegador."* | Revisa de inmediato. Quizá otra persona la tomó, o aún puedes apartarla a mano. |
| **Inventario sin arma de interés: Ana** | *"Apareció inventario en … (14 armas), pero ninguna cumple los objetivos ni las alternativas; no se seleccionó nada…"* | Revisa la lista en la bitácora o el historial. Si alguna te sirve, ajusta los objetivos o apártala a mano. |
| **Sesión cerrada: Ana** | *"La plataforma cerró la sesión. Inicie sesión de nuevo en la ventana de este cliente y reanude el monitoreo."* | Inicia sesión en el navegador y pulsa **Iniciar / reanudar**. |
| **Monitoreo detenido: Ana** | *"La plataforma respondió con errores varias veces seguidas. Revise la ventana de este cliente y reanude el monitoreo."* | Mira la bitácora (captura) y reanuda cuando se resuelva. |
| **Revise la solicitud: Ana** | Explica qué pasó y da los pasos: abrir «Estado de trámite» en el navegador de Ana y buscar la solicitud EN PROGRESO; si está, elegir «Solicitud existente», escribir su número y pulsar «Iniciar / reanudar»; si no está, dejar «Nueva solicitud (paso 1)» y reanudar. | Sigue los pasos de la alerta. El mismo texto queda en el aviso rojo de arriba del Panel. |

> **Inventario sin arma de interés.** Si aparece inventario pero ninguna arma cumple tus objetivos, alternativas ni
> comodín, la alarma suena y **el monitoreo se detiene dejando la tabla abierta**, para que elijas a mano si quieres. Si
> reanudas sin tomar nada, el mismo listado no vuelve a detenerlo; una arma nueva, o un inventario nuevo después
> de vaciarse, sí. La lista
> completa queda en la bitácora (evento *Inventario sin interés*) y en el historial del cliente, marcada como
> *Ningún objeto elegido*.
>
> **Lo que NO suena.** Que la solicitud abierta siga sin inventario, que las seccionales estén cerradas o que el
> portal falle un momento: eso solo queda en la fila del cliente.

La voz dice una frase corta, sin describir el arma. Por ejemplo: *"Atención. Arma apartada para Ana. Termine
el trámite."*

### Reconocer una alerta

- **Entendido** reconoce solo esa alerta.
- **Reconocer todas** (Panel) las reconoce todas.
- Reconocer **detiene el sonido y la voz y cierra la ventana**. No detiene ningún monitoreo.
- Si pides **Ir al navegador** y esa sesión ya se cerró, el programa reconoce esa alerta (porque ya no hay
  nada que ver) y te dice que abras una sesión nueva.

### Qué pasa con el monitoreo después de una alerta

| Situación | ¿El monitoreo se detiene solo? |
|---|---|
| Arma apartada | **Sí** (estado *Arma apartada*). |
| Arma disponible (modo alerta) | **Sí** (estado *Alerta encontrada*). |
| Sesión vencida | **Sí**. |
| Tres fallos seguidos | **Sí**. |
| No se encuentra la solicitud (*Solicitud existente*) | **Sí**, con alerta de problema. |
| Apartado fallido | **Sí**, dejando la tabla del inventario abierta para apartarla a mano. |
| Inventario sin arma de interés (con armas que no habías visto) | **Sí**, dejando la tabla abierta para elegir a mano. |

**Después de una de estas alertas** la fila queda resaltada y su botón dice **Ir al navegador**: úsalo para
elegir a mano en la tabla, que el programa ya no toca. Si no tomas ninguna arma y quieres seguir vigilando,
usa **⋯ → Reanudar monitoreo**: sigue con la misma solicitud, sin crear otra. Si el inventario sigue siendo el
mismo que ya viste (o se redujo), no vuelve a detenerse ni a sonar; solo lo hace si aparece alguna arma nueva
o si el inventario se vacía y luego llega otro.

---

## 11. Bitácora

La **Bitácora** es un registro de lo importante que ha ocurrido. **No** anota cada consulta sin novedad (eso
llenaría la lista de ruido). Solo lee: nada de lo que hagas aquí cambia el monitoreo.

### 11.1 Qué registra

| Tipo (etiqueta de color) | Cuándo se anota |
|---|---|
| **Monitoreo iniciado** (azul) | Al iniciar un monitoreo. Incluye las seccionales que busca el cliente. |
| **Monitoreo detenido** (gris) | Al detenerse, con el motivo y **cuánto duró** ("Duró 5 min 12 s"). |
| **Oportunidad** (naranja) | Se encontró un arma en modo **Solo alertar**. |
| **Arma apartada** (verde) | Se apartó un arma. |
| **Apartado fallido** (rojo) | Había arma pero no se pudo apartar. |
| **Sesión vencida** (rojo) | La plataforma cerró la sesión. |
| **Respuesta inesperada** (ámbar) | La plataforma respondió algo raro; guarda una captura de pantalla. |
| **Portal sin respuesta** (ámbar) | El servidor del portal no contestó a GUARDAR (timeout o error 5xx). Se anota una vez mientras dure y el monitoreo sigue intentando. |

Un mismo hallazgo repetido seguido se anota **una sola vez**. Las **oportunidades** y las **armas apartadas** se
ven en **negrita con fondo naranja claro** para que no se pierdan.

### 11.2 Las tarjetas de resumen

Arriba hay tres tarjetas con lo ocurrido en las **últimas 24 horas**: **Oportunidades**, **Armas apartadas** y
**Problemas** (sesión vencida, apartados fallidos y respuestas inesperadas). Siempre cuentan todo, aunque tengas
filtros puestos.

### 11.3 Buscar y filtrar

| Control | Qué hace |
|---|---|
| **Buscar: glock, bogotá o medellín…** | Busca texto en el **cliente, tipo, detalle, seccional y arma**. No importan tildes ni mayúsculas. Todas las palabras deben estar (`glock luis`). Para **alternativas** usa **"o"**, coma o barra: `medellin o bogota`, `Medellín, Bogotá`, `cz o glock luis`. |
| **Todos los clientes** | Muestra solo un cliente. |
| **Todos los tipos** | Muestra solo un tipo de evento. |
| **Todo el periodo** | Última hora, últimas 24 horas o últimos 7 días. |
| **Quitar filtros** | Vuelve a mostrar todo (se activa solo si hay algún filtro). |
| **Actualizar** | Vuelve a leer los datos. |

Debajo de la tabla, el contador dice, por ejemplo, *"3 de 12 evento(s) con los filtros actuales"* o *"12
evento(s) en total"*.

> **Para ver la seccional de un cliente** escribe su nombre en el buscador (`medellin`). Verás los monitoreos
> que la buscan (desde que se registran las seccionales al iniciar) y los hallazgos encontrados allí. El nombre de
> la seccional o del arma de un hallazgo solo existe si **hubo inventario**.

### 11.4 La tabla

Columnas: **Hora** ("hace 5 min"; al pasar el mouse sale la fecha completa), **Cliente**, **Tipo**,
**Seccional** y **Detalle**. Si un evento tiene una captura de pantalla, el detalle lleva un pequeño **icono de
imagen** al final.

### 11.5 Ver el detalle de un evento (doble clic)

**Haz doble clic en una fila** (o selecciónala y pulsa **Enter**) para abrir la ventana **Detalle del evento**:

- **Tipo, Cuándo, Cliente**, y —si los hay— **Seccional, Arma y Duración**.
- **Detalle completo**: el texto íntegro (la tabla lo puede cortar).
- **Captura de pantalla del fallo**, si el evento la tiene, con el botón **Abrir imagen completa**. Si la imagen
  ya se borró dice *"La captura … ya no está en la carpeta de capturas."*
- **Ver inventario guardado**, en las oportunidades, armas apartadas y apartados fallidos: abre el inventario
  que el programa vio en ese momento (sección 13). Si ya no está dice *"No se conserva el inventario de ese
  momento: el historial guarda solo los 100 más recientes."*

### 11.6 Exportar a CSV (Excel)

**Exportar CSV** guarda los eventos que **ves ahora** (con los filtros aplicados) en un archivo que abre Excel.
Incluye: fecha y hora, cliente, tipo, seccional, arma, duración, captura y detalle. Si no hay eventos
a exportar, te lo dice. Por seguridad, las celdas que empezarían con `=`, `+`, `-` o `@` se marcan con un
apóstrofe para que Excel no las ejecute como fórmulas.

### 11.7 Cuánto se conserva

- Se guardan como máximo **1000 eventos** de los **últimos 30 días**; lo más antiguo se borra solo.
- Las **capturas de pantalla** de los fallos también se borran solas a los **30 días** (al abrir el programa y
  una vez al día).
- **Vaciar bitácora** (botón rojo arriba, junto a *Exportar CSV*) borra **todos** los eventos de todos los
  clientes para liberar espacio. Pide confirmación con **Aceptar** o **Cancelar**; Cancelar es la opción por
  defecto. No se puede deshacer, así que si quieres conservarlos, exporta antes el CSV. No detiene ningún
  monitoreo (los eventos nuevos se siguen anotando) y no borra el historial de inventarios ni las capturas.
- Cuando una captura se borra, su evento sigue en la bitácora, con el aviso de que ya no está.

---

## 12. Diagnóstico

**Diagnóstico** revisa en un momento si el equipo y la configuración están listos. Pulsa **Ejecutar
diagnóstico**; tarda unos segundos (consulta las voces del sistema). **No abre el portal ni cambia nada.**

Cada comprobación termina en uno de tres niveles: **Correcto** (verde), **Revisar** (ámbar) o **Problema**
(rojo). El **veredicto general** es el peor de todos:

- *"Todo en orden: no se encontraron problemas."*
- *"Funciona, pero hay puntos que conviene revisar."*
- *"Hay problemas que impiden o ponen en riesgo el monitoreo."*

Cada resultado trae una línea **"Qué hacer"** cuando corresponde.

| Comprobación | Qué revisa | Cuándo es Problema o Revisar |
|---|---|---|
| **Configuración** | Que los datos de los clientes y los ajustes sean válidos. | **Problema** si hay datos inválidos o faltantes (lista los primeros). **Revisar** si no hay ningún cliente. |
| **Navegador** | Que Chrome o Edge (el que elegiste) esté instalado. | **Problema** si falta y usas el **portal real**. **Revisar** si falta pero usas el simulador (no lo necesita). |
| **Almacenamiento** | Que la carpeta de datos se pueda escribir y que haya espacio. | **Problema** si no se puede escribir. **Revisar** si quedan menos de 100 MB. |
| **Audio y voz** | Que haya salida de audio y una voz en español. | **Problema** si el sonido está activo y no hay audio. **Revisar** si la voz está activa y no hay voz en español, o si **sonido y voz están desactivados** (solo verás alertas en pantalla). |
| **Horario** | Que el horario tenga al menos un día y rangos válidos, y si **ahora** estás dentro. | **Problema** si no hay ninguna franja válida. **Revisar** si ahora estás fuera de horario. Con el horario desactivado: Correcto. |

Una comprobación que falla no oculta a las demás.

---

## 13. Historial de inventario

Cada vez que una consulta trae inventario real y el programa elige un arma, guarda una "foto" de lo que vio.
Se consulta desde el Panel: **⋯ → Ver historial de inventario** (solo se habilita si ya hay historial).

**La lista.** Se titula **Historial de inventarios** ("Selecciona una consulta para revisar el inventario y la
elección del sistema"). Cada entrada muestra la **fecha**, la **seccional** y el **arma elegida**. Se conservan
las **100 más recientes** (entre todos los clientes).

**El detalle.** Al elegir una entrada se abre **Inventario analizado** ("Evidencia de la decisión del sistema"):

- **OBJETO ELEGIDO POR EL SISTEMA**: el arma, con la regla aplicada (*Objetivo prioritario #1*, *Alternativa
  #2* o *Comodín por tipo*).
- **CLIENTE, SECCIONAL, FECHA** de esa consulta.
- **Inventario observado · N objetos**: la lista completa de armas que había, con la elegida resaltada.

Sirve para entender **por qué** el sistema eligió esa arma y no otra.

---

## 14. Dónde se guardan tus datos

Todo es local, en tu equipo, en la carpeta **`%APPDATA%\MonitorSIAE`** (puedes pegar eso en el Explorador de
archivos).

| Archivo o carpeta | Qué contiene |
|---|---|
| `config.json` | Tu configuración (clientes, ajustes, alarmas). |
| `config.json.bak` | Copia de respaldo de la configuración anterior. |
| `inventarios.json` | El historial de inventarios (los 100 más recientes). |
| `bitacora.json` | La bitácora (hasta 1000 eventos de 30 días). |
| `capturas\` | Capturas de pantalla de los fallos (se borran a los 30 días). |
| `perfiles\` | Los perfiles de navegador de cada cliente. |
| `logs\` | Registros técnicos (`monitor.log`, un archivo por día), útiles para soporte. |

**Cuidado con `perfiles\`.** Ahí viven las cookies y datos de sesión de cada navegador. Son **datos sensibles**:
no compartas ni copies esa carpeta.

**Si la configuración se daña.** Si al abrir el programa no puede leer `config.json`, ofrece **restaurar la copia
de respaldo más reciente** (*"¿Restaurar la copia de respaldo más reciente?"*). El programa nunca guarda las contraseñas de la
plataforma SIAE.

---

## 15. Solución de problemas

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El botón **Iniciar / reanudar** está gris. | Cliente inactivo, configuración incompleta o (portal real) sin sesión abierta. | Pasa el mouse sobre el botón: dice el motivo. Revisa Ajustes → Clientes. |
| Aparece *"Completa la configuración antes de iniciar el monitoreo."* | Faltan o sobran datos. | Pulsa **Iniciar / reanudar** para ver la lista, o revisa el Diagnóstico. |
| No se puede guardar en Ajustes. | Un dato es inválido. | Lee la ventana *"No se pudo guardar la configuración"*; la lista dice qué corregir. |
| Dice **"Seccionales cerradas"** todo el tiempo. | La plataforma realmente las tiene cerradas. | Normal; sigue intentando. |
| Dice **"Abiertas pero no objetivo: …"**. | Hay seccionales abiertas, pero ninguna coincide con las del cliente. | Compara el texto de la seccional del cliente con la lista de la plataforma. |
| Dice **"Opción del trámite no disponible"**. | Un texto de "cómo obtener el permiso" o "tipo de trámite" no coincide con el portal. | Escríbelos tal como los muestra la plataforma. |
| Dice **"Inventario … sin armas de interés"**. | Hay armas, pero ninguna cumple tus reglas. | Revisa objetivos, alternativas, comodín y exclusiones (sección 9). |
| **Sesión cerrada** / alerta de sesión vencida. | La plataforma cerró la sesión o cerraste el navegador. | **Abrir sesión**, inicia sesión de nuevo y **Iniciar / reanudar**. |
| El monitoreo se detuvo con **"Monitoreo detenido: errores de la plataforma"**. | Tres respuestas inesperadas seguidas. | Abre la **Bitácora**, mira la captura del último fallo, corrige y reanuda. |
| La captura de un fallo muestra la **página de inicio** de la plataforma. | La sesión no estaba iniciada o venció. | Inicia sesión de nuevo en el navegador de ese cliente. |
| No suena la alarma. | Volumen bajo, sonido desactivado, o el cliente está silenciado. | Usa **Probar alarma**; revisa Ajustes → Alarmas y el "Sonido de alertas" del cliente; ejecuta el Diagnóstico. |
| La voz habla con acento inglés. | No hay voz en español. | Instala una voz en español (sección 2). |
| **Ir al navegador** no trae la ventana al frente. | Windows limita la activación de ventanas. | Haz clic en el icono parpadeante del navegador en la barra de tareas. |
| Cambié el intervalo o el escenario y no lo noto. | Esos cambios rigen desde el siguiente inicio. | Detén y vuelve a iniciar el monitoreo. |
| El Diagnóstico dice que no encuentra el navegador. | Chrome/Edge no está instalado o está en otra ubicación. | Instálalo o elige el otro navegador en Ajustes → General. |
| Olvidé la contraseña de Ajustes. | — | Cierra la aplicación, abre `config.json` (sección 14), borra el valor del campo `hash` dentro de `acceso` y vuelve a abrirla: pedirá crear una contraseña nueva. |

---

## 16. Glosario

- **Alerta**: aviso que exige tu atención (ventana, sonido y voz).
- **Alternativa**: arma de reemplazo cuando no hay objetivos.
- **Apartar**: seleccionar el arma y pulsar la modalidad y Confirmar en el portal. Deja el arma pre-asignada.
- **Bitácora**: registro de los eventos importantes.
- **Comodín**: tipo de arma aceptado como último recurso.
- **Consulta / ciclo**: una vuelta completa del monitoreo de un cliente.
- **Escalamiento**: aumento progresivo de la insistencia de una alerta no reconocida.
- **Fuera de horario**: el monitoreo está activo pero espera su franja horaria.
- **Inventario**: lista de armas disponibles en el paso 2.
- **Jitter (variación aleatoria)**: pequeño margen al azar sobre el tiempo de espera entre consultas.
- **Modalidad**: Porte o Tenencia.
- **Monitoreo**: ejecución automática y repetida del ciclo de un cliente.
- **Objetivo**: arma que el cliente quiere, descrita con palabras clave.
- **Paso 1 / Paso 2**: las dos pantallas de la plataforma que el programa recorre (datos del trámite; listado de
  armas y apartado).
- **Perfil**: los datos y la sesión de navegador propios de cada cliente.
- **Reconocer**: confirmar que viste una alerta; silencia su sonido y voz.
- **Seccional**: oficina donde se realiza el trámite.
- **Sellar**: terminar el cierre del paso 2 (**FINALIZAR** y su confirmación).
- **Sesión**: el navegador abierto de un cliente, con la sesión iniciada en la plataforma.
- **Simulador**: portal de práctica incluido en el programa.
