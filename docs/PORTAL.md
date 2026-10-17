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

- Los desplegables son componentes personalizados con `input[role="combobox"]`, no `<select>`
  nativos. [visto en código del script de terceros]
- Seccionales prioritarias: Medellín y Bogotá. [dicho]
- La lista de seccionales solo muestra las que están abiertas. Formato `NN - Ciudad, Departamento`,
  por ejemplo `16 - Usaquén, Bogotá D.C.` o `73 - Cali, Valle del Cauca`. [visto en capturas]
- Por eso el estado se conoce **antes** de pulsar GUARDAR. Estados que distinguía el script de
  terceros [visto en capturas]:
  - La opción "Adquisición de arma" no aparece en el tipo de trámite.
  - "Seccionales cerradas": la lista de seccionales está vacía.
  - "Abiertas pero no objetivo": hay seccionales, pero no la configurada.
- El script de terceros recargaba la página en cada ciclo, aproximadamente cada 10 segundos.
  [visto en capturas]

### Resultado de GUARDAR

- Sin inventario aparece un mensaje parecido a "No hay inventario disponible por parte de INDUMIL en
  esta seccional", sin pasar al paso 2. [dicho, texto exacto por confirmar]
- Con inventario se pasa al paso 2 con el listado de armas. [dicho]
- GUARDAR crea un borrador que no se radica hasta el paso 3. [hipótesis, viene de otra IA]

### Paso 2: Formulario de Solicitud

- Ventana titulada "FG-1, solicitud adquisición de armas para personas naturales". [visto en
  capturas]
- El listado es una tabla con columnas, entre ellas "Tipo de Arma", "Marca" y "Calibre"; tiene
  paginación ("Registros por página 100", "Mostrando registros 0-0 de un total de 0") y un botón
  FINALIZAR. [visto en capturas]
- El mensaje "No hay inventario disponible por par..." aparece dentro de esa ventana. Falta
  comprobar si el paso 2 puede abrirse con la tabla vacía. [visto parcialmente]
- El listado puede traer armas que no son pistolas, o ninguna pistola. [visto en capturas]
- Apartar un arma sigue esta secuencia [visto en capturas del script de terceros]:
  1. Seleccionar la fila del arma.
  2. Pulsar el botón **Porte**.
  3. Pulsar **Confirmar**.
  4. "Asignación exitosa". Todo el proceso tomó entre 1 y 12 segundos.
- Una fila leída como texto completo tuvo el formato
  `PISTOLA Pistola Glock Mod. G-26 Cal 9mm 9T 2P (Pequeña) 9MM PORTE 1`: tipo, descripción, calibre,
  modalidad y, posiblemente, cantidad. [visto en capturas]

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

Más ejemplos vistos en otras capturas:

```
Pistola Cordova Estandar cal 9MM 9T 3P (Mdiana) CORDOVA 9MM
Pistola Cordova Compacta Cal 9MM 9T 3P (Pequeña) CORDOVA 9MM
Pistola CZ P10 CAL 9 mm 2P 9T (Grande) CZ 9MM
Pistola Beretta 92 FS COMPACT CAL 9MM BERETTA 9MM
Pistola Beretta 92X Performance Cal 9mm 2P 9T BERETTA 9MM
Pistola Glock Mod G-43 RAIL CAL 9mm 9T 2P GLOCK 9MM
PISTOLA Pistola Glock Mod. G-26 Cal 9mm 9T 2P (Pequeña) 9MM PORTE 1
```

Marcas de interés mencionadas por el cliente: Córdoba (en la plataforma aparece "Cordova"),
Smith & Wesson, Jericho, Beretta. [dicho]

---

## Lista de comprobación en horario hábil

La aplicación ya puede abrir el portal con Playwright en un perfil persistente independiente por
cliente. La automatización del trámite real permanece deshabilitada hasta completar y probar esta
lista con una cuenta autorizada. No compartir credenciales, cookies ni encabezados de autenticación.

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
- [ ] Si el aviso de 8 a 16 se aplica: probar GUARDAR fuera de ese horario.
- [ ] Qué hacen los botones Porte y Confirmar, y si hay un botón Tenencia.
- [ ] Selectores estables de cada campo (atributos `id`, `name`, `aria-label` o `formcontrolname`).

Al compartir capturas de la pestaña Red, borrar antes los encabezados `Cookie` y `Authorization`.
