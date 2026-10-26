# Monitor SIAE

Aplicación de escritorio para Windows que agiliza la solicitud de adquisición de arma en la
plataforma SIAE 2.0 del DCCAE.

El tramitador inicia sesión con cada cliente. A partir de ahí, el programa llena el paso 1 del
trámite (cómo obtener el permiso, tipo de trámite, seccional y descripción), pulsa GUARDAR de forma
periódica mientras no haya inventario y, cuando el listado del paso 2 trae las armas buscadas,
aparta el arma según las reglas del cliente y lanza una alarma imposible de ignorar para que la
persona termine el proceso.

> Estado: rediseño de interfaz en `feature/redisenio-ui`, con las fases 1 a 5 implementadas y
> validaciones manuales pendientes. El estado detallado está en
> [el plan de rediseño](docs/PLAN-REDISENO-UI.md). El instalador sigue pendiente.

## Qué hace y qué no hace

**Hace**
- Llena el paso 1 con los parámetros configurados por cliente, respetando que cada lista depende de
  la anterior.
- En cada ciclo prueba **todas** las seccionales configuradas del cliente, en orden de preferencia:
  si la primera responde sin inventario, sigue con la siguiente antes de esperar al próximo ciclo.
- Puede limitarse a un horario con horas propias para cada día (por ejemplo, lunes a viernes de 8:00 a
  16:00 y sábado de 9:00 a 12:00, hora de Colombia).
- Lee el listado de armas y elige según las reglas del cliente: armas objetivo en orden de
  prioridad y, si no hay ninguna, seguir esperando, usar alternativas o tomar cualquiera de un tipo.
- Aparta el arma (modalidad y Confirmar) o, en modo alerta, solo avisa.
- Alerta con voz, sonido en bucle, ventana al frente y parpadeo en la barra de tareas, siempre que
  detecta inventario real (arma apartada, arma disponible en modo alerta, o apartado fallido), sin
  importar el horario de operación.
- Detecta sesión vencida o respuestas inesperadas y avisa en lugar de repetir a ciegas.
- Varía ligeramente (±20 %) el tiempo de espera entre consultas, para no seguir un patrón fijo.

**No hace**
- No inicia sesión ni resuelve captchas: eso lo hace siempre una persona.
- No guarda contraseñas de la plataforma.
- No carga documentos ni envía la solicitud (paso 3).

## Requisitos

- Windows 10 u 11.
- JDK 21 (para desarrollar). El instalador final incluye su propio Java.
- Google Chrome o Microsoft Edge instalado.
- No hace falta instalar Maven: el proyecto trae su propio wrapper.

### Voz en español

La alarma habla con las voces de Windows. Sin una voz en español, igual suenan la sirena y la
ventana, pero la voz hablará con acento inglés.

Comprobar qué voces hay (PowerShell normal):

```powershell
$null = [Windows.Media.SpeechSynthesis.SpeechSynthesizer,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime]; [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Select-Object DisplayName, Language
```

Si no aparece ninguna `es-...`, instalarla:

1. Opción gráfica: Configuración > Hora e idioma > Voz > Agregar voces > Español.
2. Si Configuración se cierra o muestra 0 MB, por comando en **PowerShell como administrador**:

   ```powershell
   Add-WindowsCapability -Online -Name "Language.TextToSpeech~~~es-MX~0.0.1.0"
   ```

   (`es-ES` para España). No requiere reiniciar; cerrar y abrir las aplicaciones que usen voz.

Probar la voz exactamente como la usa el programa, desde la carpeta del proyecto:

```powershell
$env:MONITOR_SIAE_VOZ = "Atención. Arma apartada para Ana. Termine el trámite."
powershell -NoProfile -ExecutionPolicy Bypass -File src\main\resources\co\monitorsiae\notificacion\hablar.ps1
```

## Compilar y probar

Desde PowerShell, en la carpeta del proyecto:

```powershell
.\mvnw.cmd -v          # la primera vez descarga Maven
.\mvnw.cmd verify      # compila y ejecuta las pruebas
```

La primera compilación descarga las dependencias (Playwright es la más grande) y puede tardar
varios minutos.

## Abrir la interfaz

Desde PowerShell, en la carpeta del proyecto:

```powershell
.\mvnw.cmd javafx:run
```

La pantalla principal permite administrar perfiles, revisar el estado, reconocer alertas y probar
la alarma. En **Configuración → General** se puede escoger el entorno global **Simulador** o
**Portal real**. El simulador ejecuta las pruebas sin conectarse a SIAE. En modo real, **Abrir
sesión** inicia Chrome o Edge con un perfil local aislado por cliente y abre el trámite; la persona
debe iniciar sesión y resolver el captcha. Las credenciales del portal no se solicitan ni se guardan
en Monitor SIAE.

La barra lateral también ofrece dos páginas de solo lectura:

- **Bitácora**: eventos operativos por cliente, tipo y periodo (inicio y detención del monitoreo,
  oportunidades, armas apartadas, apartados fallidos, sesión vencida y respuestas inesperadas). No
  registra cada consulta sin novedad. Se guarda en `bitacora.json`, dentro de la carpeta de datos, y
  se conservan como máximo 1000 eventos de los últimos 30 días. Las capturas de pantalla de los fallos (carpeta `capturas`) se borran solas a
  los 30 días, igual que los eventos. Permite buscar por texto, filtrar,
  ver el detalle de cada evento (doble clic), abrir el inventario guardado y exportar a CSV (Excel).
- **Diagnóstico**: revisa configuración, navegador, almacenamiento, audio/voz y horario, y propone
  qué hacer. No abre el portal ni cambia nada.

La integración real está en etapa de validación; las comprobaciones pendientes se documentan en
`docs/PORTAL.md`. Al cambiar el entorno global, detén primero los monitores en ejecución.
Los perfiles persistentes y sus cookies viven en `%APPDATA%\MonitorSIAE\perfiles`; son datos
sensibles locales y no deben compartirse.

El simulador se comporta según el **escenario** que elijas en **Configuración → General** (aparece
solo con el entorno Simulador) y cada vez que inicias un monitoreo el escenario arranca desde el principio:

- **Hay inventario desde la primera consulta:** encuentra armas de inmediato.
- **Sin inventario y luego aparece** (predeterminado): seccionales cerradas, luego abiertas sin inventario y,
  después de cinco consultas, inventario; se ve la espera y luego la selección según el cliente.
- **Hay inventario, pero sin los objetivos del cliente:** prueba las alternativas y el comodín.
- **El arma aparece pero otra persona la toma:** el apartado no se confirma (reintentos y aviso de fallo).
- **La plataforma falla tres veces seguidas:** el monitor se detiene por fallos repetidos y suena la alarma.
- **La sesión vence a mitad del monitoreo:** el monitor se detiene con alarma.
- **Las seccionales siempre están cerradas:** para dejar el monitoreo corriendo.

El inventario simulado tiene 15 armas con nombres al estilo de la plataforma (Glock G-43, G-26 y G-19,
CZ P10, Cordova, Jericho, Taurus, Beretta, Sig Sauer, Smith & Wesson, Canik, Walther, Ruger 22 LR y un
revólver), así que cualquier objetivo, alternativa, comodín o exclusión razonable se puede probar. Para ver una
alerta sin apartar un arma, crea un cliente en modo **ALERTA** y elige el escenario que quieras.
Las exclusiones se aplican antes de objetivos, alternativas y comodín. Si el cliente prefiere
cualquier pistola antes que modelos concretos, agrega `Pistola` como primer objetivo prioritario;
el tipo comodín sigue siendo el último recurso.

Cuando el sistema encuentra un arma, guarda el inventario completo y la regla que eligió el arma en
`%APPDATA%\MonitorSIAE\inventarios.json`. El historial conserva los últimos 100 hallazgos y se
consulta con **Historial** en la tarjeta del cliente. No se guarda cada consulta sin coincidencias.
Este archivo permanece local y puede contener información sensible del inventario.

Si hay inventario real pero no se pudo confirmar el apartado (la plataforma no respondió como se
esperaba tras los reintentos configurados), la alarma suena igual que si el arma se hubiera apartado
o detectado en modo alerta: el hallazgo no debe pasar inadvertido solo porque el apartado automático
falló. Esto ocurre sin importar el horario de operación configurado (D11): una vez detectado el
inventario, la alerta siempre suena.

## Estructura

```
src/main/java/co/monitorsiae/
  app/            Arranque y conexión de las piezas
  dominio/        Modelo del negocio, sin dependencias externas
  monitoreo/      Ciclo por cliente, horario y filtro de armas
  portal/         Interfaz PortalDriver, implementación con Playwright y simulador
  notificacion/   Voz, sonido, ventana y parpadeo
  persistencia/   Configuración e historial de inventarios en JSON
  acceso/         Contraseña opcional de la aplicación
  ui/             Pantallas JavaFX y editor protegido de configuración
herramientas/maven/  Wrapper de Maven (lo invoca mvnw.cmd)
src/main/resources/logback.xml  Registro en %APPDATA%\MonitorSIAE\logs (30 días)
docs/
  DECISIONES.md   Qué se decidió y por qué
  PORTAL.md       Lo que sabemos de la plataforma y lo que falta comprobar
```

## Configuración

Se guarda en `%APPDATA%\MonitorSIAE\config.json` y se edita desde la aplicación. Por cada cliente:

- Cómo obtener el permiso y tipo de trámite, escritos **como aparecen en la plataforma** (no
  importan tildes ni mayúsculas). Si la plataforma cambia un nombre, basta con corregirlo en la
  configuración.
- Seccionales en orden de preferencia. Basta una parte reconocible: `Bogotá` encuentra
  `16 - Usaquén, Bogotá D.C.`. En cada ciclo se prueban todas las seccionales configuradas que estén
  abiertas, en ese orden: si la primera no tiene inventario, se intenta la siguiente antes de pasar
  al próximo ciclo.
- Descripción de la solicitud (entre 150 y 500 caracteres).
- Modo: **APARTAR** (selecciona y confirma el arma) o **ALERTA** (solo avisa).
- Reglas de selección:

```json
"seleccion": {
  "modalidad": "PORTE",
  "objetivos": [
    { "palabrasClave": ["glock", "g-43"] },
    { "palabrasClave": ["glock", "g-26"] }
  ],
  "siNoHayObjetivo": "USAR_ALTERNATIVAS_LUEGO_TIPO",
  "alternativas": [ { "palabrasClave": ["cordova", "compacta"] } ],
  "tipoComodin": "Pistola",
  "excluir": ["22 lr"],
  "reintentos": 2
}
```

  - `objetivos`: en orden de prioridad. Todas las palabras clave deben aparecer en el nombre del
    arma como palabras completas. No importan tildes, mayúsculas ni guiones: `g43` encuentra
    `G-43`, pero `22` no encuentra `225`. Escriba las marcas como aparecen en la plataforma
    (por ejemplo, `cordova` y no `córdoba`).
  - `siNoHayObjetivo`: `SEGUIR_ESPERANDO`, `USAR_ALTERNATIVAS`, `CUALQUIERA_DEL_TIPO` o
    `USAR_ALTERNATIVAS_LUEGO_TIPO`.
  - `USAR_ALTERNATIVAS_LUEGO_TIPO` prueba primero todas las alternativas en orden. Si ninguna
    coincide, elige la primera arma del inventario cuyo tipo sea `tipoComodin` (por ejemplo,
    `Pistola`). Las alternativas pueden quedar vacías para pasar directamente al tipo comodín.
  - `excluir` se aplica antes de objetivos, alternativas y comodín: cualquier palabra excluida
    descarta el arma aunque coincida con otra regla.
  - `excluir`: palabras que descartan un arma aunque coincida.

Además, a nivel general: navegador (Chrome o Edge), segundos entre consultas (con una variación aleatoria
de cada espera, 20 % por defecto y configurable de 0 a 50 %) y horario de operación (desactivado por defecto).

Antes de monitorear, la aplicación valida la configuración y explica cada error. El botón
**Verificar configuración** comprueba, con la sesión abierta, que cada texto exista en las
listas reales.

### Contraseña de la aplicación

Opcional. Protege la edición de la configuración, no el monitoreo. Se guarda cifrada con PBKDF2,
nunca en texto plano.

**Si se olvida:** cerrar la aplicación, abrir `config.json`, borrar el valor del campo `hash` dentro
de `acceso` y volver a abrir la aplicación, que pedirá crear una nueva.

## Uso diario

1. Abrir la aplicación.
2. Iniciar sesión en la plataforma con cada cliente, en su propia ventana.
3. Pulsar **Iniciar monitoreo**.
4. Atender la alarma cuando suene y terminar el trámite manualmente.

## Plan de desarrollo

1. Estructura base, documentación y wrapper.
2. Modelo y configuración JSON.
3. Contraseña de la aplicación.
4. Núcleo: filtro de armas, `PortalDriver`, simulador y monitor.
5. Alarmas: voz y sirena. **(implementadas; validación de audio local pendiente)**
6. Interfaz JavaFX. **(rediseño en progreso; ver `docs/PLAN-REDISENO-UI.md`)**
7. Integración con el portal real. **(implementada parcialmente; validación pendiente según
   `docs/PORTAL.md`)**
8. Instalador con `jpackage`.

## Límites conocidos y responsabilidad

- Depende de la estructura de la página. Si la plataforma cambia, hay que ajustar
  `PlaywrightPortalDriver`.
- Quien sepa editar el JSON puede desactivar la contraseña de la aplicación.
- El uso de las cuentas en la plataforma y el cumplimiento de sus condiciones de uso corresponden a
  quien opera el programa.
