<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
# Auditoría de endurecimiento — Fase 13

Resultado de las auditorías que el master prompt §13 exige antes de la 1.0.
Cada una es **ejecutable**: vive como prueba, no como una casilla marcada en un
documento que envejece sin que nadie lo note.

_Ejecutado el 2026-08-29 sobre `experiment`. 319 pruebas en 57 clases, `./gradlew build` en verde._

---

## Regresión

`./gradlew build` compila los siete módulos, ejecuta las 319 pruebas y las
comprobaciones de frontera de ArchUnit. Cobertura por capa:

| Capa | Qué se prueba |
| --- | --- |
| Dominio | Invariantes puras: dinero, ciclos de vida, cadena de auditoría, plegado de texto, numeración. |
| Aplicación | Casos de uso contra dobles en memoria: permisos, validaciones, flujos completos. |
| Infraestructura | Adaptadores SQLite reales, motor de copias sobre ficheros reales, plantillas de impresión. |
| App | Escenarios end-to-end sobre el grafo real: `MunicipalLoopIT`, `MunicipalModulesIT`, `DocumentPrintingIT`, `BackupRecoveryIT`, `SecurityHardeningIT`, `FrontOfficeUiIT`. |

---

## Rendimiento (§45)

`PerformanceAuditIT`, con **20 000 ciudadanos y 20 000 trámites** — más de lo que
un municipio mediano acumula en varios años.

| Comprobación | Resultado |
| --- | --- |
| Búsqueda de ciudadano | < 400 ms |
| Cola de trabajo | < 400 ms |
| Historial de un ciudadano | < 400 ms |
| Búsqueda por número | < 50 ms |
| Conteo | < 400 ms y en SQL, no contando filas traídas |
| Paginación | Una búsqueda de 50 devuelve 50, no la tabla entera |

Las cifras están sueltas a propósito: un PC de mostrador es más lento que una
máquina de compilación, y un test que falla cuando el CI está cargado enseña a
la gente a ignorarlo. Lo que detectan es un **orden de magnitud**, que es lo que
cuesta un índice ausente.

La prueba también lee el **plan de ejecución** de las consultas que la UI hace.
Un tiempo que pasa hoy porque la tabla está en caché no dice nada; un
`SCAN person` sí.

---

## Migraciones (§13)

`MigrationAuditTest` comprueba propiedades del esquema completo, no de una
migración concreta — son justo las que solo fallan años después:

- migrar dos veces no cambia nada;
- `PRAGMA integrity_check` y `foreign_key_check` limpios;
- las claves foráneas están **activadas**, no solo declaradas (SQLite las
  desactiva por defecto: declararlas sin el pragma es decoración);
- toda tabla tiene clave primaria;
- **ningún importe se guarda como coma flotante**, y toda tabla con importes
  nombra su moneda (o la hereda de su fila padre, con la excepción registrada);
- los disparadores que hacen la auditoría append-only siguen ahí.

### Hallazgo corregido

**30 claves foráneas sin índice.** SQLite indexa las primarias pero nunca las
foráneas. Cuesta en dos sitios, ambos invisibles hasta que hay años de datos: una
consulta por esa columna se vuelve un escaneo completo, y borrar la fila padre
escanea la tabla hija entera.

`V0010__foreign_key_indexes.sql` indexa las doce que la aplicación recorre de
verdad. Las demás nombran **quién hizo algo** para la auditoría: nada consulta
por ellas, e indexarlas costaría escritura en las tablas más activas del sistema
para servir un informe que nadie ejecuta. Esa lista de exenciones vive en el
propio test, así que saltarse una es **una decisión registrada**, no un descuido:
una clave foránea nueva que no esté en la lista rompe la auditoría.

---

## Accesibilidad y UX (§36, §78)

`AccessibilityAuditTest`:

- todo control visible se puede alcanzar con el teclado;
- nada en pantalla es una clave de traducción sin resolver;
- toda clave del catálogo tiene texto (más de 200 claves);
- **ningún mensaje filtra detalle técnico** — nada de `Exception`, nombres de
  clase ni fragmentos de SQL llega al operador;
- toda ruta de navegación tiene título traducido.

Verificado además a mano en la aplicación empaquetada: atajos `Ctrl+K`,
`Alt+Home`, `Ctrl+Shift+G`, `F1`, `Ctrl+Q`, y tema claro/oscuro.

### Hallazgos corregidos

- **La ventana por defecto (1200×780) no cabía en 1280×720**, resolución muy
  común en un mostrador. El botón principal del primer arranque quedaba fuera de
  pantalla: la instalación no se podía completar. Ahora la ventana se ajusta a la
  pantalla real y se centra.
- La tarjeta de acceso se estiraba a todo el alto de la ventana.
- `LoginViewLayoutTest` medía **sin la hoja de estilos**, es decir, un layout que
  la aplicación nunca renderiza. Así es exactamente como una tarjeta mal colocada
  sobrevive a un test en verde.

---

## Impresión (§59B–§59F)

- `NarrowReceiptLayoutTest`: nada desborda el ancho del rollo en 58 ni en 80 mm;
  un concepto largo **se parte**, no se trunca; el cambio y el código de
  verificación aparecen; una reimpresión lo dice en la primera línea.
- `DocumentPrintingIT`: PDF real (`%PDF-`) en ambos formatos, reimpresión que no
  renumera ni duplica el pago, diálogo cancelado que no registra nada.
- **La garantía §59F tiene su propia prueba**: se emite un documento, se cambia
  el RNC, la dirección y el pie del ayuntamiento, y el documento emitido **sigue
  diciendo lo mismo**.

Pendiente de verificación con hardware físico: la salida sobre una impresora de
impacto real. Está anotado en el Release Gate.

---

## Copias y restauración (§41, §42)

`SqliteBackupEngineTest` y `BackupRecoveryIT`, sobre ficheros reales:

- un archivo cifrado es ilegible sin la frase, y la base no aparece dentro;
- una frase incorrecta **falla ruidosamente** sin tocar la base viva;
- un archivo manipulado se rechaza (AES-GCM autentica además de cifrar);
- un fichero que no es una copia de SIRMAX se rechaza antes de sobrescribir nada;
- la secuencia §42 completa, con la copia de emergencia tomada antes del punto de
  no retorno;
- la retención purga copias rutinarias y **nunca** las de emergencia.

### Hallazgos corregidos

- Tras el intercambio del fichero, la conexión apuntaba a una base que ya no
  existía; `SqliteDatabase.reopen()` reabre sobre la restaurada.
- La base restaurada **es anterior a su propio historial de copias**: no conocía
  ni la copia de la que venía ni la de emergencia recién tomada. Ahora la
  restauración **reinscribe su procedencia**; sin eso, la copia que guarda el
  estado descartado sería imposible de encontrar desde el sistema que la
  reemplazó, que es justo para lo que se tomó.

---

## Seguridad (§40, §43)

`SecurityHardeningIT` **elimina los disparadores** de la base y edita el registro
de auditoría, para comprobar que la alteración sigue viéndose. Esa es la única
prueba honesta de una cadena de integridad: si solo se comprueba con los
disparadores puestos, se está probando el disparador.

Cubierto: bloqueo de cuenta con expiración, respuesta idéntica para usuario
inexistente y contraseña errónea, registro de todos los intentos, validación de
adjuntos por contenido.

---

## Documentación

- La guía de usuario documenta lo que existe, no lo que se planea.
- `docusaurus.config.ts` tiene `onBrokenLinks: "throw"`: un enlace interno roto
  **rompe el CI**.
- Corregidos en esta fase: la URL de Docusaurus apuntaba a un dominio que nadie
  posee (assets con 404), y la landing enlazaba a `/docs`, una ruta que el sitio
  desplegado no tiene.

---

## Lo que esta auditoría **no** cubre

Decirlo importa tanto como lo anterior:

- **Impresión sobre hardware real.** Las plantillas se verifican como PDF y como
  texto; ninguna prueba automatizada sustituye a una impresora de impacto.
- **El MSI.** Se verifica la imagen autocontenida, que arrancó en Windows 11 sin
  Java en el `PATH`. El instalador necesita WiX y una máquina que lo tenga.
- **Google Drive.** El adaptador es real, pero requiere credenciales de un
  municipio; no hay forma honesta de probarlo en CI.
- **Carga multiusuario.** SIRMAX es local-first y monousuario por diseño; no se
  ha probado con varios puestos contra la misma base en red.
