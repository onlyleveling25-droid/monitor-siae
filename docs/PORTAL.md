# Conocimiento de la plataforma SIAE 2.0 (DCCAE)

Lo que sabemos del portal y cómo lo sabemos. Este documento alimenta `PlaywrightPortalDriver`.

**Regla:** no incluir aquí nombres, cédulas, contraseñas, cookies ni tokens reales.

Leyenda: **[visto]** comprobado en pantalla · **[dicho]** contado por el cliente ·
**[hipótesis]** sin comprobar.

---

## Acceso

- URL del trámite: `https://dccae.cgfm.mil.co/dccae/main/SolicitudTramite` [visto]
- El inicio de sesión pide un captcha "No soy un robot". [visto en video]
- El registro está abierto a cualquier persona natural. [visto]
- El tramitador inicia sesión con las credenciales de cada cliente. [dicho]

## Horario

- Aviso en pantalla: "Señor(a) usuario(a), solo se pueden generar trámites de 8 am a 4 pm (zona
  horaria colombia) entre semana." [visto]
- Los inventarios y las seccionales abren en distintos momentos del día. [dicho]

## Asistente de tres pasos

1. Inicio de Trámite
2. Formulario de Solicitud
3. Cargue de Documentos

### Paso 1: Inicio de Trámite [visto]

| Campo | Tipo | Notas |
|---|---|---|
| Tipo de Usuario | Prellenado | Ej.: PERSONA NATURAL |
| Identificación | Prellenado | |
| Nombre Usuario | Prellenado | Sirve para nombrar al cliente en la alerta |
| Seleccione como desea obtener el permiso | Desplegable | Dos opciones. Se usa "Defensa Personal" [dicho] |
| Tipo de Tramite / Clase de Tramite | Desplegable | Depende del anterior. Ej.: "Armas personas naturales: Adquisición de arma" |
| Seccional | Desplegable | Depende del trámite. Con adquisición muestra varias ciudades; con otros trámites, una sola [dicho] |
| Descripción | Texto | Mínimo 150, máximo 500 caracteres. Contador visible |
| GUARDAR | Botón | Deshabilitado hasta completar el formulario |

- Los desplegables parecen componentes personalizados, no `<select>` nativos. [hipótesis]
- Seccionales prioritarias: Medellín y Bogotá. [dicho]

### Resultado de GUARDAR

- Sin inventario aparece un mensaje parecido a "No hay inventario disponible por parte de INDUMIL en
  esta seccional", sin pasar al paso 2. [dicho, texto exacto por confirmar]
- Con inventario se pasa al paso 2 con el listado de armas. [dicho]
- GUARDAR crea un borrador que no se radica hasta el paso 3. [hipótesis, viene de otra IA]

### Paso 2: Formulario de Solicitud

- Muestra el listado de armas disponibles en la seccional. [dicho]
- El listado puede traer armas que no son pistolas, o ninguna pistola. [visto en capturas de un
  script de terceros]

### Paso 3: Cargue de Documentos

- Se adjuntan los documentos en PDF y se envía la solicitud. [hipótesis]
- **El programa nunca actúa en este paso.**

---

## Ejemplos de nombres en el listado

Tomados de capturas de un script de terceros. Sirven como datos de prueba para el filtro de armas.
Nótese el ruido: abreviaturas, marcas repetidas al final, errores de digitación ("Revovler") y
entradas que no son armas completas ("9MM").

```
Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P
9MM
Escopeta Indumil de un Cartucho Cal. 16 (Defens Per) INDUMIL 16
Escopeta Hatsan Cal. 12 7T Folding Stock HATSAN 12
Revolver S&W CAL .357 4" M66 SKU 162662 SMITH & WESSON 357
Revolver Scorpio Ultra Cal 38L 2" Cacha Plástica INDUMIL 38L
Pistola Glock Mod G-44 CAL 22 LR 9T 2P GLOCK 22LR
Revolver Indumil Llama Martial Cal 38L 4"micrf (Grande) LLAMA 38L
Revolver Indumil Cassidy Ultra Cal 38L 3" 6T SPL (Mediano) INDUMIL 38L
Revolver Martial Ultra Cal 38 4" Cacha Madera INDUMIL 38L
Revolver Scorpio Ultra Cal. 38L 2" Cacha Madera LLAMA 38L
Escopeta Indumil Cap 1T Cal 12 (Defens Per) INDUMIL 12
Revolver Indumil Llama Cassidy Cal 38L 4"micrf (Grande) LLAMA 38L
Revolver Indumil Cassidy Ultra Cal 38L 4" 6T SPL (Grande) INDUMIL 38L
Revovler Martial Ultra Cal 38 4" Cacha Plástica INDUMIL 38L
```

Marcas de interés mencionadas por el cliente: Córdoba, Smith & Wesson, Jericho, Beretta. [dicho]

---

## Lista de comprobación en horario hábil

Hacer con una cuenta propia, con F12 → pestaña Red y "Conservar registro" activado. No subir
documentos ni enviar nada en el paso 3.

- [ ] Mensaje exacto al pulsar GUARDAR con seccional sin inventario.
- [ ] Si aparece algo nuevo en "trámites pendientes" después de GUARDAR.
- [ ] Si pulsar GUARDAR varias veces crea duplicados.
- [ ] Si tras el mensaje de sin inventario el formulario conserva los datos o se limpia.
- [ ] Llamadas de red al abrir cada desplegable y al pulsar GUARDAR (URL, método, tipo de respuesta).
- [ ] Si el listado del paso 2 llega como JSON o como HTML.
- [ ] Si dos pestañas con la misma sesión funcionan a la vez.
- [ ] Si el captcha o códigos por correo reaparecen después del inicio de sesión.
- [ ] Cuánto dura una sesión inactiva.
- [ ] Selectores estables de cada campo (atributos `id`, `name`, `aria-label` o `formcontrolname`).

Al compartir capturas de la pestaña Red, borrar antes los encabezados `Cookie` y `Authorization`.
