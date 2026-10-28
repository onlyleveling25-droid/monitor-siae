# Guía de empaquetado e instalación de Monitor SIAE (Windows)

Esta guía tiene dos partes:

- **Parte A – Generar el paquete.** Se hace **una vez por versión**, en un equipo Windows con JDK 21.
- **Parte B – Instalar en el equipo donde se va a usar.** Ese equipo **no necesita JDK**: el paquete
  trae su propio Java.

> **Estado de verificación.** El script de Windows
> ([herramientas/empaquetar.ps1](../herramientas/empaquetar.ps1)) ya generó el instalador `.msi` en
> Windows 11 con JDK 21 y WiX 3.14 (unos 270 MB). Falta registrar la primera instalación en un equipo
> destino. Si algo falla, el mensaje del script indica el paso; la sección 10 lista los problemas
> previsibles.

---

## 1. Requisitos

### Equipo donde se usa el programa (Parte B)

| Requisito | Detalle |
|---|---|
| Sistema | Windows 10 u 11 de 64 bits. |
| Navegador | **Google Chrome o Microsoft Edge** instalado (Edge ya viene con Windows 10/11). El programa usa el navegador del equipo; no descarga el suyo. |
| Java | **No hace falta.** Va dentro del paquete. |
| Internet | Solo para llegar al portal SIAE. El programa no descarga nada más al arrancar. |
| Espacio | Unos 400 MB para el programa (más lo que ocupen bitácora y capturas). |
| Sonido | Altavoces o audífonos funcionando y volumen activo. |
| Voz en español | Recomendada (sección 5). Sin ella suena la sirena, pero la voz habla con acento inglés. |
| Fecha y hora | Zona horaria **(UTC-05:00) Bogotá, Lima, Quito** y hora sincronizada. El horario de operación usa el reloj del equipo. |
| Energía | El equipo **no debe suspenderse** mientras monitorea (sección 7). |

### Equipo donde se genera el paquete (Parte A)

| Requisito | Detalle |
|---|---|
| Sistema | **Windows.** `jpackage` solo crea paquetes para el sistema en que se ejecuta; desde un Mac no sale un instalador de Windows. |
| JDK 21 (completo) | Por ejemplo Eclipse Temurin 21 (x64, `.msi`). Debe incluir `jpackage.exe`. |
| El proyecto | Clonado con Git o descargado como `.zip`. |
| Internet | La primera vez descarga Maven y las librerías (Playwright es la más grande). |
| WiX Toolset 3.x | **Solo** si quieres un instalador `.msi`/`.exe`. Para la versión portable no hace falta. |

---

## 2. Parte A – Generar el paquete

### Paso A1. Instalar el JDK 21

1. Descarga **Temurin 21 (JDK, Windows x64, `.msi`)** desde adoptium.net.
2. En el instalador activa las opciones **"Set JAVA_HOME variable"** y **"Add to PATH"**.
3. Abre una **PowerShell nueva** y comprueba:

   ```powershell
   $env:JAVA_HOME
   & "$env:JAVA_HOME\bin\jpackage.exe" --version
   ```

   Debe mostrar la ruta del JDK y un número de versión `21.x`. Si `JAVA_HOME` sale vacío, defínelo
   (Configuración del sistema → Variables de entorno) apuntando a la carpeta del JDK, por ejemplo
   `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot`, y abre otra PowerShell.

### Paso A2. Obtener el proyecto

```powershell
git clone https://github.com/onlyleveling25-droid/monitor-siae.git
cd monitor-siae
git checkout feature/redisenio-ui-reconocimiento
```

(O descomprime el `.zip` del proyecto y entra en la carpeta.)

### Paso A3. Generar la versión portable (recomendado para empezar)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\empaquetar.ps1
```

El script:

1. Comprueba que `JAVA_HOME` sea un JDK 21.
2. Compila y **ejecuta las pruebas** (si alguna falla, se detiene). Añade `-SinPruebas` para omitirlas.
3. Reúne las librerías y crea el paquete con `jpackage`.
4. Comprime el resultado.

La primera vez tarda varios minutos (descarga Maven y las librerías). Al terminar verás:

```
Listo. Carpeta portable: ...\target\instalador\salida\Monitor SIAE  (ejecutable: Monitor SIAE.exe)
Comprimido para copiar a otro equipo: ...\target\instalador\salida\Monitor-SIAE-1.0.0-portable.zip
```

El `.zip` es lo que llevas al equipo destino.

### Paso A4 (opcional). Generar un instalador `.msi`

El instalador agrega acceso directo, entrada en el menú Inicio y permite actualizar versiones. Se
instala **solo para el usuario actual**, así que no pide permisos de administrador.

1. Instala **WiX Toolset 3.14**. El `jpackage` de JDK 21 solo trabaja con la serie 3.x: WiX 4, 5 o
   posteriores no le sirven. WiX solo hace falta **en este equipo**, no en el equipo destino.

   La forma más simple, sin permisos de administrador, es usar los binarios oficiales. Pega esto en
   PowerShell:

   ```powershell
   $dest = "$env:LOCALAPPDATA\Programs\WiX314"
   $zip  = "$env:TEMP\wix314-binaries.zip"
   Invoke-WebRequest 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip' -OutFile $zip -UseBasicParsing
   Expand-Archive $zip -DestinationPath $dest -Force
   Remove-Item $zip
   # Agregar la carpeta al PATH del usuario
   $p = [Environment]::GetEnvironmentVariable('Path', 'User')
   if ($p -notlike "*$dest*") { [Environment]::SetEnvironmentVariable('Path', ($p.TrimEnd(';') + ";$dest"), 'User') }
   ```

   (Con `winget install WiXToolset.WiXToolset` también se puede, pero pide administrador y activar
   .NET Framework 3.5 en Windows.)

   Cierra y abre PowerShell (o VS Code) y comprueba que se encuentran:

   ```powershell
   Get-Command candle.exe, light.exe
   ```

2. Genera el instalador indicando la versión:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\empaquetar.ps1 -Tipo msi -Version 1.0.1
   ```

   Si las pruebas ya pasaron y solo quieres repetir el paquete, añade `-SinPruebas`.

3. El instalador queda en `target\instalador\salida\Monitor SIAE-<versión>.msi`.

#### Qué se lleva al equipo destino

**Solo el `.msi`.** Lleva dentro el programa, todas sus librerías y su propio Java.

- `target\instalador\lib\` y `target\monitor-siae-*.jar` son material intermedio que el script usa para
  armar el paquete. No se copian.
- El `.jar` **no es un ejecutable por sí solo**: necesita Java y las librerías de `lib\`. Ya va dentro
  del `.msi`.

#### Versiones y actualizaciones

- Cada versión nueva debe llevar un número **mayor** que la anterior (`1.0.1`, `1.1.0`, `2.0.0`…). El
  primer número no puede ser 0. Si repites un número, Windows no lo toma como actualización.
- Todas las versiones comparten un **identificador de actualización** fijo
  (`$idActualizacion` en [empaquetar.ps1](../herramientas/empaquetar.ps1)). Gracias a él, Windows sabe
  que es el mismo programa y la versión nueva **reemplaza** a la anterior, sin desinstalar.
  - No hay que guardarlo aparte: ya está en el script y cada `.msi` lo lleva automáticamente.
  - **No lo cambies nunca.** Si cambia, Windows lo trata como otro programa y quedan dos instalados.
- Lleva un registro de qué versión instalaste en cada equipo (por ejemplo, en las notas del commit).

### Icono

El proyecto ya trae uno (un monitor con una línea de pulso sobre fondo azul):

- `herramientas\icono\monitor-siae.ico`: el del `.exe`, el acceso directo y el instalador. El script lo
  usa solo si existe.
- `src\main\resources\co\monitorsiae\ui\icono-aplicacion.png`: el de la ventana (barra de título y
  barra de tareas).

Para cambiarlo por otro propio, reemplaza el `.ico` conservando ese nombre (debe llevar varios tamaños:
16, 32, 48 y 256) y el `.png` (256×256). Para regenerar el actual después de retocar el dibujo:
`java herramientas\icono\GenerarIcono.java` desde la raíz del proyecto. No uses el logo de ninguna
entidad oficial.

---

## 3. Parte B – Instalar en el equipo destino

### Opción 1: versión portable (`.zip`)

1. Copia el `.zip` al equipo (memoria USB o red).
2. Descomprímelo en una carpeta estable, por ejemplo `C:\Users\<usuario>\MonitorSIAE` (evita
   `Descargas` y el Escritorio, para no borrarlo por accidente).
3. Abre `Monitor SIAE.exe`. Opcional: clic derecho → **Enviar a → Escritorio (crear acceso directo)**.

Para actualizar: cierra el programa, sustituye la carpeta por la nueva versión. Los datos se
conservan porque viven aparte (sección 8).

### Opción 2: instalador (`.msi` o `.exe`)

1. Copia el instalador al equipo y ábrelo con doble clic.
2. Elige la carpeta (por defecto `%LOCALAPPDATA%\Monitor SIAE`) y confirma.
3. Abre **Monitor SIAE** desde el menú Inicio o el acceso directo del escritorio.

Para actualizar, cierra el programa y ejecuta el instalador de la versión nueva encima: reemplaza la
anterior y conserva los datos. **No hace falta desinstalar antes**, y el equipo destino no necesita
WiX ni Java.

### Advertencias de Windows en el primer arranque

El programa **no está firmado digitalmente**, así que es normal que Windows avise:

- **"Windows protegió tu PC" (SmartScreen):** pulsa **Más información → Ejecutar de todos modos**.
- **El antivirus lo detiene o lo pone en cuarentena:** agrega la carpeta del programa y
  `%APPDATA%\MonitorSIAE` como excepciones. Ocurre porque el programa abre y controla un navegador.

---

## 4. Primer arranque

1. Aparece el **asistente de bienvenida**: elige el navegador (Chrome o Edge), crea el primer cliente y
   escoge el entorno. **Empieza con "Simulador"** para comprobar todo sin tocar el portal real.
2. Define la **contraseña de la aplicación** si el programa la pide.
3. Abre **Diagnóstico** en la barra lateral: debe mostrar el navegador, el almacenamiento y el
   audio/voz sin problemas. Cada aviso trae qué hacer.
4. Pulsa **Probar alarma** en el Panel. Debes oír la sirena y la voz; reconócela para silenciarla.
5. Con el simulador, inicia un monitoreo y verifica que aparecen eventos en la **Bitácora**.
6. Cuando todo esté bien, en **Ajustes → General** cambia el entorno a **Portal real** (detén antes
   los monitores) y usa **Abrir sesión**: se abrirá Chrome/Edge con un perfil aislado del cliente; inicia
   sesión y resuelve el captcha tú mismo. El programa no pide ni guarda las credenciales del portal.

El detalle de cada pantalla está en el [manual de usuario](MANUAL-DE-USUARIO.md).

---

## 5. Voz en español

La voz usa las voces de Windows. La **sirena y la ventana de alerta suenan siempre**, con o sin voz.

**Comprobar qué voces hay** (PowerShell normal):

```powershell
$null = [Windows.Media.SpeechSynthesis.SpeechSynthesizer,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime]; [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Select-Object DisplayName, Language
```

Si aparece alguna `es-...` (por ejemplo `es-MX`), ya está lista.

**Si no hay ninguna:**

1. Gráficamente: **Configuración → Hora e idioma → Voz → Agregar voces → Español**.
2. Si Configuración se cierra o muestra 0 MB, por comando en **PowerShell como administrador**:

   ```powershell
   Add-WindowsCapability -Online -Name "Language.TextToSpeech~~~es-MX~0.0.1.0"
   ```

   (Usa `es-ES` para la voz de España.) Requiere internet y **no necesita reiniciar**; cierra y vuelve a
   abrir Monitor SIAE.

**Probar la voz como la usa el programa**, desde la carpeta del proyecto (solo en el equipo de desarrollo):

```powershell
$env:MONITOR_SIAE_VOZ = "Atención. Arma apartada para Ana. Termine el trámite."
powershell -NoProfile -ExecutionPolicy Bypass -File src\main\resources\co\monitorsiae\notificacion\hablar.ps1
```

En el equipo destino basta con **Probar alarma** en el Panel y el apartado de voz del Diagnóstico.

---

## 6. Alarma: volumen y sonido

- Sube el volumen del equipo y comprueba que el **dispositivo de salida** correcto esté seleccionado
  (altavoces, no un monitor HDMI sin parlantes).
- La alarma suena aunque no estés mirando la pantalla. Se repite hasta que la **reconoces** (botón
  **Reconocer** o **Reconocer todas**). Las repeticiones y el escalamiento se ajustan en **Ajustes → Alarmas**.
- Los modos "No molestar" o "Asistente de concentración" de Windows no silencian el audio del programa,
  pero un volumen en cero o audífonos desconectados sí.

---

## 7. Dejar el equipo listo para monitorear

- **Suspensión:** Configuración → Sistema → Inicio/apagado y batería → **Pantalla y suspensión**: pon
  **"Suspender: Nunca"** al estar conectado a la corriente. Un equipo suspendido no monitorea. La
  pantalla sí puede apagarse.
- **Actualizaciones de Windows:** programa los reinicios fuera del horario de operación.
- **No cierres** la ventana de Chrome/Edge del cliente: cerrarla termina su sesión del portal.
- **Sesión del portal:** si vence, el programa lo avisa con alarma y debes iniciar sesión de nuevo en la
  ventana de ese cliente.

---

## 8. Dónde quedan los datos

Todo en `%APPDATA%\MonitorSIAE`, que en la práctica es
`C:\Users\<usuario>\AppData\Roaming\MonitorSIAE`.

La carpeta `AppData` está **oculta**, así que no aparece al navegar por el Explorador. Para llegar a ella:

- **La más rápida:** pulsa **Windows + R**, escribe `%APPDATA%\MonitorSIAE` y pulsa Enter. También
  sirve pegar esa ruta en la barra de direcciones del Explorador.
- **Desde PowerShell:** `explorer "$env:APPDATA\MonitorSIAE"` abre la carpeta en el Explorador, y
  `Get-ChildItem "$env:APPDATA\MonitorSIAE"` lista su contenido. (`ls -a` es de Mac/Linux: en PowerShell
  el equivalente para ver lo oculto es `ls -Force`, por ejemplo `ls -Force $env:USERPROFILE`.)
- **Ver siempre las carpetas ocultas:** en el Explorador, **Ver → Mostrar → Elementos ocultos**
  (Windows 11) o pestaña **Vista → Elementos ocultos** (Windows 10).

Contenido:

| Qué | Dónde |
|---|---|
| Configuración y clientes | `config.json` |
| Bitácora | `bitacora.json` |
| Historial de inventarios | `inventarios.json` |
| Capturas de fallos | `capturas\` (se borran solas a los 30 días) |
| Sesiones de navegador por cliente | `perfiles\` (**sensible**: contiene cookies; no la compartas) |
| Registro técnico | `logs\monitor.log` |

- **Actualizar o reinstalar no borra estos datos. Desinstalar tampoco.** Para empezar desde cero,
  borra esa carpeta a mano con el programa cerrado.
- **Copia de seguridad:** copia `config.json` (lleva clientes y ajustes).
- **Cambiar de equipo:** copia `config.json` a la misma carpeta del equipo nuevo. Las sesiones de
  `perfiles\` conviene crearlas de nuevo iniciando sesión.

---

## 9. Lista de comprobación antes de dar por bueno un equipo

- [ ] Chrome o Edge instalado y abre normalmente.
- [ ] El programa abre y muestra el asistente (o el Panel, si ya estaba configurado).
- [ ] Diagnóstico sin avisos en rojo.
- [ ] **Probar alarma:** se oye la sirena y la voz.
- [ ] Una corrida con **Simulador** deja eventos en la Bitácora.
- [ ] Hora y zona horaria correctas; horario de operación revisado.
- [ ] Equipo sin suspensión automática.
- [ ] **Abrir sesión** en el portal real: inicia sesión y el Panel lo reconoce.

---

## 10. Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El script dice que `JAVA_HOME` no está definido o no es JDK 21 | Se instaló un JRE, otra versión, o PowerShell se abrió antes de instalar | Instala Temurin 21 (JDK), marca "Set JAVA_HOME", abre PowerShell nueva. |
| El script falla en las pruebas | Una prueba falló en ese equipo | Revisa el mensaje; para generar igual el paquete, `-SinPruebas`, y avísanos. |
| El script dice que falta WiX | Pediste `msi`/`exe` sin WiX 3.x | Instala WiX 3.14 o usa la versión portable. |
| `jpackage` rechaza la versión | Empieza en 0 o tiene letras | Usa `-Version 1.0.1`. |
| `candle.exe` no se encuentra aunque WiX está instalado | La PowerShell se abrió antes de cambiar el PATH | Cierra y abre PowerShell o VS Code. |
| Al instalar la versión nueva quedan **dos** Monitor SIAE | Se cambió `$idActualizacion` en el script | Restaura el identificador original, desinstala la copia sobrante desde Configuración → Aplicaciones. |
| El instalador nuevo no actualiza nada | Se generó con el mismo número de versión | Genera de nuevo con un número mayor. |
| El programa no abre y no muestra nada | Error al arrancar | Abre `%APPDATA%\MonitorSIAE\logs\monitor.log` y mira las últimas líneas. |
| Al ejecutar desde consola sale *"Unsupported JavaFX configuration"* | Aviso informativo de JavaFX | Es normal; no afecta al funcionamiento. |
| "No se pudo abrir el navegador o el portal SIAE" | Chrome/Edge no instalado, o distinto al elegido | Instala el navegador o cambia la elección en Ajustes → General. |
| "El navegador se cerró al arrancar, incluso al reintentar" | Edge/Chrome se está actualizando, o tiene una ventana colgada | El programa ya reintentó una vez solo. Espera un minuto o cierra todas las ventanas del navegador y vuelve a intentar. |
| Diagnóstico marca el navegador con problema | No encuentra Chrome/Edge en las rutas habituales | Reinstálalo en su ruta estándar. |
| La voz habla con acento inglés | No hay voz en español | Sección 5. |
| No suena nada | Volumen en cero o salida equivocada | Sección 6 y Diagnóstico. |
| SmartScreen o el antivirus bloquean el programa | No está firmado | Sección 3, "Advertencias de Windows". |

---

## 11. Para quien mantiene el proyecto

- **Cómo está empaquetado:** `jpackage` en modo *classpath*: el `.jar` de la aplicación más todas las
  librerías (JavaFX incluido, en su variante de Windows) en `app\`, y el JDK 21 completo en `runtime\`. No
  se recorta el runtime con `jlink` a propósito: evita que falte un módulo que solo se necesite al usar
  una función concreta; el costo es un paquete más grande.
- **Playwright no descarga su Chromium.** Se le indica `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` al crearlo:
  el programa siempre usa el Chrome o Edge del equipo (canal). Así el primer arranque no baja ~170 MB y
  funciona sin acceso al CDN de Playwright.
- **Reloj:** el horario de operación usa la zona horaria del sistema.
- **JavaFX por sistema:** las librerías se resuelven según el sistema donde se compila; por eso el
  paquete de Windows debe generarse en Windows.
- **Ruta del JDK:** el script quita la barra final de `JAVA_HOME`. En PowerShell 5.1 una ruta con
  espacios terminada en `\` llega a `jpackage` con una comilla suelta (*"Illegal char <\">"*).
- **Pendiente:** firma digital del instalador y recorte del runtime con `jlink`.
