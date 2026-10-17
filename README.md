# Monitor SIAE

Aplicación de escritorio para Windows que agiliza la solicitud de adquisición de arma en la
plataforma SIAE 2.0 del DCCAE.

El tramitador inicia sesión con cada cliente. A partir de ahí, el programa llena el paso 1 del
trámite (cómo obtener el permiso, tipo de trámite, seccional y descripción), pulsa GUARDAR de forma
periódica mientras no haya inventario y, cuando el listado del paso 2 trae las armas buscadas,
aparta el arma según las reglas del cliente y lanza una alarma imposible de ignorar para que la
persona termine el proceso.

> Estado: **fase 4 de 8 en curso** (contraseña y filtro de armas listos). Todavía no hay funcionalidad
> ejecutable.

## Qué hace y qué no hace

**Hace**
- Llena el paso 1 con los parámetros configurados por cliente, respetando que cada lista depende de
  la anterior.
- Puede limitarse a un horario (por ejemplo, lunes a viernes de 8:00 a 16:00, hora de Colombia).
- Lee el listado de armas y elige según las reglas del cliente: armas objetivo en orden de
  prioridad y, si no hay ninguna, seguir esperando, usar alternativas o tomar cualquiera de un tipo.
- Aparta el arma (modalidad y Confirmar) o, en modo alerta, solo avisa.
- Alerta con voz, sonido en bucle, ventana al frente y parpadeo en la barra de tareas.
- Detecta sesión vencida o respuestas inesperadas y avisa en lugar de repetir a ciegas.

**No hace**
- No inicia sesión ni resuelve captchas: eso lo hace siempre una persona.
- No guarda contraseñas de la plataforma.
- No carga documentos ni envía la solicitud (paso 3).

## Requisitos

- Windows 10 u 11.
- JDK 21 (para desarrollar). El instalador final incluye su propio Java.
- Google Chrome o Microsoft Edge instalado.
- No hace falta instalar Maven: el proyecto trae su propio wrapper.

## Compilar y probar

Desde PowerShell, en la carpeta del proyecto:

```powershell
.\mvnw.cmd -v          # la primera vez descarga Maven
.\mvnw.cmd verify      # compila y ejecuta las pruebas
```

La primera compilación descarga las dependencias (Playwright es la más grande) y puede tardar
varios minutos.

## Estructura

```
src/main/java/co/monitorsiae/
  app/            Arranque y conexión de las piezas
  dominio/        Modelo del negocio, sin dependencias externas
  monitoreo/      Ciclo por cliente, horario y filtro de armas
  portal/         Interfaz PortalDriver, implementación con Playwright y simulador
  notificacion/   Voz, sonido, ventana y parpadeo
  persistencia/   Configuración en JSON
  acceso/         Contraseña opcional de la aplicación
  ui/             Pantallas JavaFX
herramientas/maven/  Wrapper de Maven (lo invoca mvnw.cmd)
docs/
  DECISIONES.md   Qué se decidió y por qué
  PORTAL.md       Lo que sabemos de la plataforma y lo que falta comprobar
```

## Configuración

Se guarda en `%APPDATA%\MonitorSIAE\config.json` y se edita desde la aplicación. Por cada cliente:

- Cómo obtener el permiso, tipo de trámite y seccionales, escritos **exactamente como aparecen en
  la plataforma**. Si la plataforma cambia un nombre, basta con corregirlo en la configuración.
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
  "siNoHayObjetivo": "USAR_ALTERNATIVAS",
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
  - `siNoHayObjetivo`: `SEGUIR_ESPERANDO`, `USAR_ALTERNATIVAS` o `CUALQUIERA_DEL_TIPO`.
  - `excluir`: palabras que descartan un arma aunque coincida.

Además, a nivel general: navegador (Chrome o Edge), segundos entre consultas y horario de operación
(desactivado por defecto).

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
4. Núcleo: filtro de armas (listo), `PortalDriver`, simulador y monitor. **(actual)**
5. Notificaciones.
6. Interfaz JavaFX.
7. Integración con el portal real.
8. Instalador con `jpackage`.

## Límites conocidos y responsabilidad

- Depende de la estructura de la página. Si la plataforma cambia, hay que ajustar
  `PlaywrightPortalDriver`.
- Quien sepa editar el JSON puede desactivar la contraseña de la aplicación.
- El uso de las cuentas en la plataforma y el cumplimiento de sus condiciones de uso corresponden a
  quien opera el programa.
