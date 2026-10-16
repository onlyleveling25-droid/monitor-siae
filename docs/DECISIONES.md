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

**Voz:** sintetizador de Windows (System.Speech) invocado por PowerShell, sin librerías extra.

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
3. **Cualquier otra cosa** (error del servidor, sesión vencida, captcha, mensaje nuevo): no se
   repite a ciegas. Se guarda una captura, se registra el motivo y se avisa al usuario.

**Motivo:** evita que el programa pase horas "vigilando" una sesión muerta sin que nadie lo note.

## D15. Maven Wrapper propio y mínimo

**Decisión:** `mvnw.cmd` + `herramientas/maven/mvnw.ps1` descargan una vez la versión exacta de Maven,
verifican su SHA-512 y la reutilizan.

**Motivo:** basta con tener el JDK instalado. Es un script corto y legible en lugar del wrapper
oficial, que no se pudo generar desde el entorno de desarrollo.

## D16. Empaquetado con `jpackage`

**Decisión:** instalador `.msi` o `.exe` para Windows que incluye su propio Java. Se genera en
Windows, porque `jpackage` solo produce instaladores para el sistema donde se ejecuta.

## D17. Fuera de alcance por ahora

- Venta o distribución a terceros, licencias y activación.
- Telegram u otras notificaciones remotas.
- macOS y Linux.

---

## Pendientes por confirmar

- Si el límite de 8 a 16 se aplica de verdad (D11).
- Qué hace exactamente el botón Porte y si existe un equivalente para Tenencia (D13).
- Si GUARDAR repetido crea duplicados en "trámites pendientes" (afecta el ciclo de D14).
- Si el formulario conserva los datos tras el mensaje de sin inventario.
- Si dos pestañas con la misma sesión funcionan (Medellín y Bogotá en paralelo).
- Cuántos clientes simultáneos necesita y cuánto dura una sesión.
- Términos de uso de la plataforma respecto al acceso automatizado.
- Versiones de librerías fijadas en `pom.xml`: son estables conocidas; revisar si hay más recientes.
