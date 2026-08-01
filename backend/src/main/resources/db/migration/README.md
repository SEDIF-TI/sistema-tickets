# Migraciones Flyway

Versionado del esquema de base de datos. **Flyway está activo.**

## Migraciones actuales

| Archivo | Contenido |
|---|---|
| `V1__baseline_esquema_inicial.sql` | Baseline: las 12 tablas tal como existen hoy en producción, tomadas del volcado real. |
| `V2__add_indexes.sql` | Índices sobre claves foráneas y columnas de filtrado. |
| `V3__integridad_referencial_y_columnas_obsoletas.sql` | FK faltante en `equipo_reparacion` y documentación del esquema. |

## Cómo se comporta al arrancar

La configuración es `baseline-on-migrate=true` con `baseline-version=1`:

- **Sobre la base de datos existente** (la que ya tiene los datos), Flyway marca `V1` como aplicada **sin ejecutarla** y solo corre de `V2` en adelante. Los índices se crean; nada se recrea ni se pierde.
- **Sobre una base vacía** (un entorno nuevo, o el contenedor de Docker recién creado), `V1` crea el esquema completo. Usa `IF NOT EXISTS` en todo, así que es segura en ambos escenarios.

## Reglas de trabajo

- **Nunca se modifica una migración ya aplicada.** Flyway guarda un checksum de cada archivo: si cambia, el arranque falla. Para corregir algo, se añade una migración nueva.
- Numeración incremental sin huecos: `V4__`, `V5__`, …
- Nombre descriptivo en minúsculas con guion bajo, y doble guion bajo tras la versión: `V4__add_columna_folio.sql`.
- Toda migración debe revisarse antes de llegar a producción: es la única vía por la que cambia el esquema.

## Estado de `ddl-auto`

| Perfil | Valor | Motivo |
|---|---|---|
| desarrollo | `update` | Comodidad al iterar sobre las entidades. |
| `prod` | `validate` | Hibernate **nunca** modifica el esquema en producción; solo comprueba que las entidades cuadren con las tablas. |

Cuando el equipo confirme que las entidades están estables, conviene pasar
desarrollo también a `validate`, para que cualquier cambio de esquema entre
obligatoriamente por una migración.

## Deuda técnica documentada

Estas situaciones están registradas en `V3` mediante `COMMENT ON COLUMN`, de
modo que quedan visibles desde el propio esquema:

**Columnas duplicadas en `resguardo`.** Cuatro columnas de una versión
anterior conviven con sus equivalentes `s_*`, que son las que la entidad usa
realmente:

```
condiciones  ·  departamento  ·  numero_inventario  ·  telefono
```

Están a `NULL` en todos los registros revisados. **No se eliminaron**: borrar
columnas es irreversible y conviene confirmar antes que ningún reporte ni
consulta externa las lee. Cuando se confirme, basta una `V4` con los
`DROP COLUMN IF EXISTS`.

**Columnas sin mapear en la entidad.** `ticket.s_justificacion`,
`ticket.plan_trabajo_clave`, `usuario.s_apellido_paterno` y
`usuario.s_apellido_materno` existen en la base de datos y contienen datos
históricos, pero no están en las entidades Java. Hibernate las ignora y
quedan a `NULL` en los registros nuevos. Decidir si se recuperan o se retiran
es una decisión de producto, no de infraestructura.
