# Registro de decisiones

Cada entrada explica qué se decidió y por qué. Si una decisión cambia, no se borra: se marca como
reemplazada y se agrega la nueva con la fecha.

---

## D01. Aplicación de escritorio para Windows

**Decisión:** programa instalado en el PC del usuario, con interfaz gráfica. No es una aplicación web
ni un script de terminal.

**Motivo:** el usuario opera desde un solo equipo Windows, necesita la alerta en esa misma pantalla y
no es técnico. La terminal sirve para quien programa, no para quien opera con prisa.

## D02. Java 21 LTS

**Motivo:** versión de soporte extendido, ya instalada en el equipo de desarrollo (Temurin 21) y
compatible con todas las librerías elegidas.

## D03. JavaFX para la interfaz

**Alternativas:** Swing (incluido en el JDK, aspecto anticuado) y una web local con Spring Boot (más
pesada, otro servidor que puede fallar).

**Motivo:** la pantalla principal es un panel de estado en vivo por cliente. Las propiedades
observables y las tablas de JavaFX lo resuelven con poco código.

**Consecuencia:** el proyecto usa classpath, no módulos de Java (JPMS). Por eso la clase con `main`
no puede extender `Application`: se usa un lanzador separado que la invoca.

## D04. Playwright conectado al navegador instalado

**Decisión:** Playwright para Java controla el Chrome que ya está instalado (`channel = chrome`), con
Edge como alternativa configurable. No se descarga un Chromium propio.

**Motivo:** evita unos 150 MB extra y la dependencia de internet en el primer arranque.

**Por evaluar:** abrir el Chrome normal con el puerto de depuración y conectarse con
`connectOverCDP`. Podría reducir la fricción con el captcha, pero no está comprobado.

## D05. Arquitectura por capas con `PortalDriver`

**Decisión:** todo lo que toca la página (llenar el paso 1, pulsar GUARDAR, leer el listado,
detectar sesión vencida) queda detrás de la interfaz `PortalDriver`, con dos implementaciones:
`PlaywrightPortalDriver` (portal real) y un simulador.

**Motivo:** cuando la plataforma cambie, se modifica una sola clase. El simulador permite
desarrollar y probar fuera del horario de 8 a 16 y sin inventario real.

Dependencias permitidas entre paquetes:

```
ui ──► monitoreo ──► dominio
 │         │
 │         ├──► portal (interfaz) ◄── implementaciones
 │         └──► notificacion (interfaz) ◄── implementaciones
 └──► persistencia, acceso
app: conoce todo y conecta las piezas.
dominio: no depende de nadie.
```

## D06. Un perfil de navegador y un hilo por cliente

**Decisión:** cada cliente tiene su propio contexto persistente de navegador (carpeta de perfil
propia) y su propio hilo con su propia instancia de Playwright.

**Motivo:** las pestañas de un mismo contexto comparten cookies, así que dos clientes en el mismo
contexto se pisarían la sesión. Además, Playwright para Java no es seguro entre hilos. Las
actualizaciones de pantalla pasan por `Platform.runLater`.

**Consecuencia (2026-10-17):** el hilo de cada cliente vive mientras su ventana esté abierta, no solo
mientras se monitorea. `MonitorCliente` recibe el `PortalDriver` ya creado, se ejecuta en ese hilo y
**no cierra el navegador al terminar**: si apartó un arma, la ventana debe quedar abierta para que la
persona termine el trámite. Reanudar el monitoreo reutiliza el mismo hilo.

## D07. La persona inicia sesión; el programa no maneja credenciales del portal

**Decisión:** el tramitador inicia sesión con cada cliente y resuelve el captcha. El programa trabaja
desde esa sesión abierta. No guarda contraseñas del portal ni intenta resolver captchas.

**Motivo:** el captcha existe para verificar que hay una persona; saltarlo pone en riesgo las cuentas
de los clientes. Además, no guardar contraseñas evita custodiar datos de terceros.

**Consecuencia:** el programa no queda totalmente desatendido. Cada mañana alguien inicia sesión. Si
una sesión se cae, la alerta es distinta a la de inventario.

## D08. Configuración en un archivo JSON, sin base de datos

**Decisión:** la configuración vive en `%APPDATA%\MonitorSIAE\config.json`, leída y escrita con
Jackson.

**Reglas:**
- Escritura atómica: se escribe un temporal y luego se reemplaza el original.
- Antes de reemplazar se guarda `config.json.bak`.
- Campo `version` para migrar archivos de versiones anteriores.
- Si el archivo está dañado, se avisa y se ofrece restaurar el respaldo; nunca se cierra en silencio.

**Motivo:** son decenas de registros como mucho. Instalar y mantener PostgreSQL en el PC de un
cliente sería un problema de soporte. Si más adelante hace falta historial consultable, se evalúa
SQLite o H2 embebidos.

## D09. Contraseña de la aplicación opcional, con PBKDF2

**Decisión:** la configuración puede protegerse con una contraseña de la aplicación. Se guarda como
`pbkdf2$<iteraciones>$<sal>$<hash>` usando `PBKDF2WithHmacSHA256` del JDK, sal aleatoria de 16 bytes
y 600.000 iteraciones. La comparación usa `MessageDigest.isEqual`.

**Por qué no MD5:** es tan rápido que una contraseña corta se descubre en segundos.

**Alcance:** protege la edición de la configuración, no el monitoreo. Cualquiera puede ver el estado
y atender una alarma.

**Desactivar:** interruptor en la configuración; pide la contraseña actual.

**Si se olvida:** soporte borra el campo `hash` del JSON y el programa pide crear una nueva.

**Límite conocido:** quien sepa editar el JSON puede quitarla. Es aceptable para el uso previsto.

## D10. Alertas solo locales

**Decisión:** voz ("Hay pistolas para <cliente>"), sonido en bucle hasta que el usuario lo reconozca,
ventana al frente, parpadeo en la barra de tareas y botón "Probar alarma". Advertencia si el volumen
está en silencio.

**Descartado:** Telegram. El usuario trabaja frente al PC; una notificación en el celular no ayuda.

**Voz (actualizada 2026-10-17):** guion `hablar.ps1` (recurso del programa) que usa las voces
modernas de Windows (`Windows.Media.SpeechSynthesis`), las que se instalan desde Configuración > Voz.
Prefiere español y, si esas voces no están disponibles, recurre al sintetizador clásico
(System.Speech). El sintetizador clásico no ve las voces modernas: en el equipo de desarrollo solo
encontraba Zira (inglés) aunque había voces en español instaladas. El guion tiene un modo
`-SoloVerificar` para que la interfaz avise si el equipo no tiene voz en español. El texto viaja en una variable de entorno y no dentro
del comando, porque incluye nombres escritos por el usuario.

**Implementación (2026-10-17):**
- `CentralAlertas` guarda una alerta activa por cliente (la más reciente reemplaza a la anterior) y
  envía la lista completa a cada canal. Lista vacía significa callar.
- Solo generan alerta: arma apartada, arma disponible (modo alerta), sesión vencida y monitoreo
  detenido por errores repetidos. Lo demás queda en el registro.
- Voz y sirena se repiten hasta que la persona reconozca la alerta. La sirena se genera en memoria
  (sin archivos): aguda para oportunidades, grave para problemas.
- La ventana al frente y el parpadeo de la barra de tareas se implementan con la interfaz (fase 6).

**Descartado por ahora:** detectar si el volumen está en silencio. Java no lo permite sin
librerías nativas; se cubre con el botón "Probar alarma".

**Actualización (2026-10-24): también alerta el apartado fallido, con la misma urgencia.**

Si la plataforma devolvió inventario real pero el apartado no se pudo confirmar tras agotar los
reintentos (`ResultadoCiclo.ApartadoFallido`), antes no se generaba ninguna alerta: el cliente se
quedaba sin aviso aunque hubiera un arma disponible. Ahora `TraductorAlertas` trata ese resultado
igual que `ArmaApartada`/`ArmaEncontrada`: genera una alerta de `Gravedad.OPORTUNIDAD` (misma
urgencia sonora) indicando cuántos intentos se hicieron y pidiendo revisar la ventana del navegador.
`CoordinadorMonitores` pasa el estado de la sesión a `ALERTA_ENCONTRADA` en ese caso, igual que en el
modo ALERTA, para que suene la alarma sin importar si el apartado quedó sellado o no.

**Motivo:** un apartado fallido por un error transitorio de la plataforma no debe significar perder
la oportunidad en silencio; la persona debe poder intervenir manualmente de inmediato.

**Alertas de oportunidad ignoran el horario de operación:** la restricción horaria (D11) solo decide
*cuándo se consulta* el portal; una vez que una consulta (dentro o fuera de horario) trae inventario
real, la alerta y la alarma suenan siempre. El horario nunca silencia un hallazgo ya detectado.

## D11. Horario de operación

**Decisión (actualizada 2026-10-15):** la restricción horaria existe en la configuración
(días, hora inicial, hora final, zona `America/Bogota`) pero viene **desactivada por defecto**.

**Motivo:** la plataforma muestra el aviso "solo se pueden generar trámites de 8 am a 4 pm (zona
horaria colombia) entre semana", pero capturas del script de terceros muestran inventario detectado
a las 19:02 y a las 20:13. Hasta comprobar si el límite se aplica, perder un inventario nocturno
cuesta más que consultar de más. Si se confirma el límite, se activa por defecto.

*Versión anterior: el monitoreo solo consultaba de lunes a viernes de 08:00 a 16:00.*

## D12. Desplegables dependientes y textos configurables

**Decisión:** el paso 1 se llena en orden (cómo obtener el permiso → tipo de trámite → seccional) y
tras cada selección se espera a que la lista siguiente termine de cargar. Los valores se configuran
con el texto exacto que muestra la plataforma, comparados sin distinguir tildes ni mayúsculas.

**Motivo:** cada lista depende de la anterior. Si la plataforma renombra una opción, el usuario la
corrige en la configuración sin tocar código. Un botón "Verificar configuración" comprueba que cada
texto exista antes de que abra el inventario.

**Validación:** la descripción debe tener entre 150 y 500 caracteres, como exige la plataforma.

## D13. Modos de reacción y reglas de selección de arma

**Decisión (actualizada 2026-10-15):** dos modos por cliente:

- **APARTAR** (principal): elige el arma según las reglas, pulsa la modalidad (Porte o Tenencia) y
  Confirmar, detiene el monitor de ese cliente y alerta para que la persona termine el trámite.
- **ALERTA:** aplica las mismas reglas pero no selecciona nada; solo alerta.

**Reglas de selección, por cliente:**

- `objetivos`: lista en orden de prioridad. Cada objetivo es un conjunto de palabras clave que deben
  aparecer todas en el nombre del arma, sin distinguir tildes ni mayúsculas. Gana el primero que
  aparezca.
- `siNoHayObjetivo`: qué hacer si ningún objetivo está en el inventario.
  - `SEGUIR_ESPERANDO`: descarta el inventario y sigue buscando solo los objetivos.
  - `USAR_ALTERNATIVAS`: toma la primera coincidencia de la lista `alternativas`.
  - `CUALQUIERA_DEL_TIPO`: toma cualquier arma cuyo tipo sea `tipoComodin` (por ejemplo, Pistola).
- `excluir`: palabras que descartan un arma aunque coincida.
- `reintentos`: intentos de apartar antes de rendirse.

**Motivo:** capturas del script de terceros muestran la secuencia "Arma seleccionada → Botón Porte →
Botón Confirmar → Asignación exitosa" y que su filtro era por arma específica: con varias pistolas
en inventario reportaba "No hay armas permitidas". Se usan palabras clave y no el nombre completo
porque los nombres traen ruido y errores de digitación.

*Versión anterior: ALERTA era el alcance confirmado y SELECCIONAR estaba pendiente.*

## D14. Tres resultados posibles en cada consulta

1. **Avanzó al paso 2:** hay inventario; se lee el listado.
2. **Mensaje de sin inventario:** se espera y se repite.
3. **Sesión vencida:** se detiene de inmediato y se avisa; solo una persona puede volver a entrar.
4. **Cualquier otra cosa** (error del servidor, mensaje nuevo, página distinta): se guarda una
   captura y se registra. Se toleran hasta 3 seguidas con espera creciente (2x, 3x el intervalo),
   porque la plataforma falla por momentos; a la tercera, el monitor se detiene y avisa.

Antes de pulsar GUARDAR también se distinguen: opción de trámite no disponible, seccionales cerradas
y seccionales abiertas que no son objetivo. En esos casos se espera y se repite.

**Motivo:** evita que el programa pase horas "vigilando" una sesión muerta sin que nadie lo note, sin
rendirse ante un error aislado del servidor.

## D14b. Seccionales por nombre parcial

**Decisión:** una seccional configurada coincide si es igual a la opción de la plataforma o si
aparece dentro de ella como palabras completas: "Bogotá" coincide con "16 - Usaquén, Bogotá D.C.".
Gana el orden de preferencia de la configuración, no el de la lista.

**Motivo:** la plataforma antepone un código y agrega el departamento; exigir el texto completo
haría la configuración frágil.

## D14c. Un ciclo prueba todas las seccionales configuradas, no solo la primera

**Decisión (2026-10-24):** `SelectorSeccional.elegirTodas(...)` devuelve **todas** las seccionales
abiertas aceptadas por el cliente, en el orden de preferencia de la configuración (sin repetidos).
`CicloMonitoreo` recorre esa lista completa en el paso 1: si una seccional responde "sin inventario",
prueba la siguiente en el mismo ciclo antes de rendirse. `elegir(...)` (una sola seccional) sigue
existiendo como atajo sobre `elegirTodas(...)` para quien no necesite la lista completa.

El ciclo solo se detiene en una seccional cuando esta devuelve inventario (con o sin arma de
interés) o cuando el portal cambia de estado por otra razón (sesión vencida, trámite no disponible,
error). Si **todas** las seccionales candidatas quedan sin inventario, el resultado
`ResultadoCiclo.SinInventario` reporta la lista completa de seccionales probadas, unidas con `" | "`
(por ejemplo, `"05 - Medellín, Antioquia | 16 - Usaquén, Bogotá D.C."`).

**Motivo:** antes el ciclo se quedaba con la primera seccional aceptada y, si no tenía inventario,
esperaba al siguiente ciclo sin probar las demás seccionales configuradas del mismo cliente en la
misma vuelta. Con varias seccionales de interés (p. ej. Medellín y Bogotá), eso retrasaba detectar
inventario en la segunda hasta el siguiente intervalo de espera.

## D14d. Variación aleatoria (jitter) en las esperas

**Decisión (2026-10-24):** `Pausa.real()` aplica una variación aleatoria de hasta ±20 % sobre la
duración de cada espera (entre ciclos, en la espera creciente tras fallos y en la espera fuera de
horario), mediante el método estático `Pausa.conJitter(Duration base, double factor)`
(`ThreadLocalRandom`, nunca negativo). La interfaz `Pausa` no cambia su firma pública: el jitter vive
solo dentro de la implementación real, para que las pruebas que usan duraciones exactas (dobles de
prueba) no se vean afectadas.

**Motivo:** un intervalo fijo entre consultas es un patrón de tráfico fácil de distinguir del de una
persona real. Añadir una variación aleatoria reduce la previsibilidad frente a la plataforma sin
cambiar el comportamiento observable del programa (sigue respetando aproximadamente el intervalo
configurado).

**Actualización (2026-10-26):** la variación es configurable en Configuración → General, de 0 a 50 %
(`variacionEsperaPorcentaje`, 20 % por defecto; 0 la desactiva y cada espera dura exactamente el
intervalo). Se aplica al crear el monitor de un cliente, así que un cambio rige desde el siguiente
inicio. Un archivo de configuración anterior, que no trae el campo, toma el 20 %. La voz de las
alarmas conserva su ±20 % fijo.

## D15. Maven Wrapper propio y mínimo

**Decisión:** `mvnw.cmd` + `herramientas/maven/mvnw.ps1` descargan una vez la versión exacta de Maven,
verifican su SHA-512 y la reutilizan.

**Motivo:** basta con tener el JDK instalado. Es un script corto y legible en lugar del wrapper
oficial, que no se pudo generar desde el entorno de desarrollo.

## D16. Empaquetado con `jpackage`

**Decisión:** instalador `.msi` o `.exe` para Windows que incluye su propio Java. Se genera en
Windows, porque `jpackage` solo produce instaladores para el sistema donde se ejecuta.

**Detalles (2026-10-27):** `herramientas/empaquetar.ps1` genera una versión portable (`app-image`, sin
herramientas extra) o un `.msi`/`.exe` (requiere WiX 3.x). Va en modo *classpath* con el JDK 21 completo
como runtime (sin `jlink`, para no arriesgar módulos faltantes) y se instala por usuario, sin
administrador. Playwright se crea con `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`: la app siempre usa el Chrome o
Edge del equipo y no debe descargar su Chromium. Ver `docs/GUIA-INSTALACION.md`.

## D17. Fuera de alcance por ahora

- Venta o distribución a terceros, licencias y activación.
- Telegram u otras notificaciones remotas.
- macOS y Linux.

---

## D18. Asistente de primera ejecución, editor visual de horario y escalamiento de alarmas (Fase 8 del rediseño de UI)

**Decisión (2026-10-26):** se implementó la Fase 8 del rediseño de UI excluyendo explícitamente el
modo ensayo, por indicación del usuario:

- `AsistentePrimeraEjecucion` (dominio puro, sin JavaFX) decide cuándo hace falta el asistente
  (`esNecesario`: configuración nula o sin clientes), valida el nombre del primer cliente y aplica el
  resultado del asistente sobre una configuración base sin tocar horario ni preferencias de alertas.
  `AsistentePrimeraEjecucionDialogo` es el `Dialog<Configuracion>` de JavaFX que presenta los pasos
  (Bienvenida, Navegador, Primer cliente, Entorno del portal, Resumen) y delega toda la lógica de
  validación/aplicación en la clase de dominio. Se invoca una sola vez al arrancar
  `AplicacionJavaFx`, antes de construir la ventana principal, y su resultado se guarda con
  `ServicioAplicacion.guardarConfiguracionInicial(...)` (sin pasar por el validador completo, porque
  el asistente deja a propósito un cliente incompleto para terminar de llenarlo en Configuración).
- El editor visual semanal (`EditorConfiguracion.EditorHorarioSemanal`) es solo una representación
  distinta del `HorarioOperacion`: una grilla de 7 días × 24 horas donde se resalta la ventana de
  cada día. En esta fase la grilla reflejaba una única ventana compartida; D19 la amplió a un rango
  por día.
- El escalamiento de alarmas reutiliza `PerfilEscalamiento`/`NivelEscalamiento` (ya existentes en el
  dominio) y se conecta con `CentralAlertas.tiempoActivo(clienteId)`, un método nuevo que devuelve
  cuánto lleva activa sin reconocer la alerta de un cliente (usa un `Clock` inyectable para poder
  probarlo con tiempo controlado). `CanalSonido` y `CanalVoz` reciben una sobrecarga de constructor
  que acepta una función `id -> Duration` para consultar ese tiempo transcurrido y así decidir si
  forzar sonido o voz al cruzar cada nivel configurado; cuando el perfil de escalamiento está
  deshabilitado, el comportamiento es idéntico al anterior.
- **Modo ensayo:** no se implementó. Quedó fuera a propósito porque necesita una especificación
  explícita de qué puede preparar en el portal y qué nunca debe enviar, y pruebas aisladas antes de
  poder incluirse con seguridad.

**Motivo:** reducir la fricción de la primera configuración sin forzar a una persona nueva a entender
todo el formulario de Configuración de una sola vez, dar una lectura visual inmediata del horario sin
reinventar sus reglas, y permitir que las alertas se vuelvan más insistentes con el tiempo sin obligar
a nadie a usarlo (el perfil de escalamiento sigue siendo opcional y deshabilitado por defecto).

## D19. Horario de operación por día (aprobado por el usuario, 2026-10-26)

**Decisión:** cada día de la semana tiene su propio rango de horas (`FranjaHoraria`); un día sin
rango no se consulta. Hay como máximo una franja por día y no cruza la medianoche. El formato de
configuración sube a la versión 3; los archivos de la versión 2 (días con un único rango
compartido) se migran a una franja idéntica por cada día marcado, así que su comportamiento no
cambia. El horario sigue desactivado por defecto (D11).

**Motivo:** el usuario necesita horas distintas según el día (por ejemplo, un sábado más corto).

**Consecuencias:** una versión anterior de la aplicación se niega a abrir un archivo de versión 3
(en lugar de interpretar mal el horario). El editor ofrece campos de hora por día y "Copiar Lunes a
todos los días". Quedan fuera dos franjas en un mismo día y horarios que cruzan la medianoche.

## Pendientes por confirmar

- Si el límite de 8 a 16 se aplica de verdad (D11).
- Qué hace exactamente el botón Porte y si existe un equivalente para Tenencia (D13).
- Si GUARDAR repetido crea duplicados en "trámites pendientes" (afecta el ciclo de D14).
- Si el formulario conserva los datos tras el mensaje de sin inventario.
- Si dos pestañas con la misma sesión funcionan (Medellín y Bogotá en paralelo).
- Cuántos clientes simultáneos necesita y cuánto dura una sesión.
- Términos de uso de la plataforma respecto al acceso automatizado.
- Versiones de librerías fijadas en `pom.xml`: son estables conocidas; revisar si hay más recientes.
