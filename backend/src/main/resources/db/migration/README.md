# Migraciones Flyway

Versionado del esquema de base de datos. Flyway está activo y es la única vía
por la que cambia el esquema.

## Migraciones

| Archivo | Contenido |
|---|---|
| `V1__baseline_esquema_inicial.sql` | Baseline: el esquema completo tomado del volcado de producción. Todo con `IF NOT EXISTS`. |
| `V2__add_indexes.sql` | Índices sobre claves foráneas y columnas de filtrado. |
| `V3__integridad_referencial_y_columnas_obsoletas.sql` | Clave foránea faltante en `equipo_reparacion` y documentación del esquema con `COMMENT ON COLUMN`. |
| `V4__estado_ticket_a_enum.sql` | La tabla `estadoticket` pasa a ser el enum `EstadoTicket`. |
| `V5__prioridad_ticket_a_enum.sql` | La prioridad pasa a ser el enum `Prioridad`. |
| `V6__datos_iniciales.sql` | Roles, vistas del menú, área inicial y usuario administrador. |
| `V7__vista_historial_resguardos.sql` | Entrada de menú del historial de resguardos. |
| `V8__encuesta_satisfaccion.sql` | Calificación del servicio al cerrar el ticket. |
| `V9__retirar_modulo_correos.sql` | Retirada de la tabla de correos institucionales y su menú. |
| `V10__dictamen_e_historial_unificado.sql` | Tabla `dictamen` y unificación del historial en una entrada de menú. |
| `V11__resguardo_numero_empleado_opcional.sql` | `s_solicitante_numero` deja de ser obligatorio. |

## Comportamiento al arrancar

La configuración es `baseline-on-migrate=true` con `baseline-version=1`:

- **Sobre una base de datos existente**, Flyway marca `V1` como aplicada sin
  ejecutarla y corre de `V2` en adelante.
- **Sobre una base vacía**, `V1` crea el esquema completo. Usa `IF NOT EXISTS`
  en todas sus sentencias, por lo que es segura en ambos escenarios.

Hibernate arranca en `ddl-auto=validate` en todos los perfiles: comprueba que
las entidades coinciden con las tablas, pero no modifica nada. Una entidad que
no cuadre con el esquema aborta el arranque.

## Reglas

- **Una migración aplicada no se modifica.** Flyway guarda el checksum de cada
  archivo; si cambia, el arranque falla en toda instalación donde ya corrió.
  Las correcciones van en una migración nueva.
- Numeración incremental sin huecos: `V12__`, `V13__`, …
- Nombre en minúsculas con guion bajo y doble guion bajo tras la versión:
  `V12__add_columna_folio.sql`.
- Las migraciones son idempotentes siempre que sea posible (`IF NOT EXISTS`,
  `ON CONFLICT DO NOTHING`), de modo que reaplicarlas sobre una base ya
  actualizada no altere nada.

## Particularidades del esquema

Registradas en `V3` mediante `COMMENT ON COLUMN`, visibles desde el propio
esquema.

**Columnas duplicadas en `resguardo`.** Las columnas `condiciones`,
`departamento`, `numero_inventario` y `telefono` conviven con sus equivalentes
`s_*`, que son las que mapea la entidad. Las primeras están a `NULL` en todos
los registros y no las lee la aplicación.

**Columnas sin mapear.** `ticket.s_justificacion`, `ticket.plan_trabajo_clave`,
`usuario.s_apellido_paterno` y `usuario.s_apellido_materno` existen en la base
con datos históricos, pero no forman parte de las entidades. Hibernate las
ignora y quedan a `NULL` en los registros nuevos.
