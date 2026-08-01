# Migraciones Flyway

Esta carpeta contiene el versionado del esquema de base de datos.

## Estado actual: PENDIENTE DE ACTIVAR

Flyway está **desactivado** (`spring.flyway.enabled=false` en
`application.properties`) porque todavía no existe la migración base.

El proyecto viene funcionando con `spring.jpa.hibernate.ddl-auto=update`, es
decir, Hibernate creando y modificando las tablas por su cuenta a partir de las
entidades Java. La base de datos ya está en producción con datos reales, así que
**no se puede generar el baseline adivinando el esquema desde las entidades**:
cualquier diferencia de tipo, longitud o restricción produciría un `V1` que
miente sobre la estructura real, y la primera migración posterior rompería.

## Cómo activarlo (procedimiento)

1. **Volcar el esquema real** de la base de datos actual, sin datos:

   ```bash
   pg_dump --schema-only --no-owner --no-privileges \
           -U postgres -d sistema_tickets_db > esquema_actual.sql
   ```

2. **Crear `V1__baseline.sql`** en esta carpeta con ese volcado. Debe reflejar
   la base de datos tal y como está hoy, sin inventar nada.

3. **Crear `V2__add_indexes.sql`** con los índices que faltan. En PostgreSQL las
   claves foráneas **no** se indexan solas, y estas se consultan constantemente:

   ```sql
   CREATE INDEX IF NOT EXISTS idx_usuario_rol      ON usuario (fn_rol_id);
   CREATE INDEX IF NOT EXISTS idx_usuario_area     ON usuario (fn_area_id);
   CREATE INDEX IF NOT EXISTS idx_ticket_estatus   ON ticket (fn_estatus_id);
   CREATE INDEX IF NOT EXISTS idx_ticket_soporte   ON ticket (fn_usuario_soporte_id);
   CREATE INDEX IF NOT EXISTS idx_ticket_area      ON ticket (fn_usuario_area_id);
   ```

   > Verifica los nombres reales de tabla y columna contra el volcado del paso 1
   > antes de aplicarlo.

4. **Activar Flyway** en `application.properties`:

   ```properties
   spring.flyway.enabled=true
   ```

   `baseline-on-migrate=true` y `baseline-version=1` ya están configurados: sobre
   la base de datos existente, Flyway marcará `V1` como aplicada sin volver a
   ejecutarla, y solo correrá de `V2` en adelante.

5. **Cambiar a `validate`** en desarrollo, una vez comprobado que todo cuadra:

   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```

   En el perfil `prod` ya está forzado a `validate`.

## Reglas de trabajo

- **Nunca** se modifica una migración ya aplicada. Flyway guarda un checksum: si
  cambia, el arranque falla. Para corregir algo, se añade una migración nueva.
- Numeración incremental sin huecos: `V1__`, `V2__`, `V3__`…
- Nombres descriptivos en minúsculas con guion bajo:
  `V4__add_columna_telegram_chat_id.sql`.
- Doble guion bajo entre la versión y la descripción: `V4__descripcion.sql`.
- Toda migración debe ser revisada antes de llegar a producción: es la única vía
  por la que cambia el esquema.
