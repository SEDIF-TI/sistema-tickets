# Backend — Sistema de Tickets

API REST en **Spring Boot 4.0.6** sobre **Java 17**, con PostgreSQL y esquema
versionado por Flyway.

Para levantarlo, consulta la [guía de puesta en marcha](../README.md) en la raíz:
base de datos en Docker y arranque desde VS Code con la extensión de Spring Boot.

---

## Estructura

```
core/          Un paquete por dominio (entidad, repositorio, servicio, recurso, DTOs)
  ticket/        tickets, bitácora y estrategias de filtrado por rol
  taller/        equipos recibidos para reparación
  resguardo/     préstamos de equipo
  dictamen/      dictámenes técnicos emitidos
  documento/     generación de PDF (OpenPDF) y Excel (Apache POI)
  usuarios/      usuarios, roles y vistas del menú
  area/ aviso/ equipo/ actividad/ dashboard/ telegram/
security/      JWT, filtros, rate limiting
exception/     manejo centralizado de errores y respuestas uniformes
util/          enums, auditoría
config/        CORS, WebSocket, caché
```

---

## Endpoints

Todos cuelgan de `/api`. La autenticación es **JWT en la cabecera
`Authorization: Bearer <token>`**.

| Ruta                      | Rol                       |
| ------------------------- | ------------------------- |
| `/v1/auth`                | público (login)           |
| `/salud`                  | público (healthcheck)     |
| `/v1/tickets`             | según rol (ver más abajo) |
| `/v1/perfil`              | autenticado               |
| `/taller`                 | SOPORTE, ADMINISTRADOR    |
| `/v1/resguardos`          | SOPORTE, ADMINISTRADOR    |
| `/v1/dictamenes`          | SOPORTE, ADMINISTRADOR    |
| `/v1/documentos`          | SOPORTE, ADMINISTRADOR    |
| `/v1/equipos`             | SOPORTE, ADMINISTRADOR    |
| `/v1/actividades-extras`  | SOPORTE, ADMINISTRADOR    |
| `/v1/avisos`              | lectura autenticada       |
| `/v1/admin/*`             | ADMINISTRADOR             |

### Visibilidad de tickets

Cada rol ve un subconjunto distinto, resuelto en la base con una estrategia por
nivel (`core/ticket/filtros/`), no filtrando en memoria:

| Rol           | Nivel      | Ve                          |
| ------------- | ---------- | --------------------------- |
| ADMINISTRADOR | `GLOBAL`   | todos los tickets           |
| SOPORTE       | `PERSONAL` | los que tiene asignados     |
| EMPLEADO      | `AREA`     | los de su área              |

---

## Decisiones de diseño

**Los enums viajan en dos campos.** Cada DTO expone la constante estable
(`estatus`) y su texto legible (`estatusEtiqueta`). La lógica compara siempre la
constante; la etiqueta es solo para mostrar. Igual con prioridad y calificación.
Así, cambiar un texto en pantalla nunca rompe una condición.

**Paginación, búsqueda y ordenación se resuelven en la base**, nunca sobre la
página ya descargada, y los campos ordenables van en lista blanca.

**Patrón obligatorio en las consultas con búsqueda opcional:**

```java
WHERE (CAST(:busqueda AS string) IS NULL OR LOWER(r.campo) LIKE :busqueda ESCAPE '!')
```

El `CAST` es imprescindible: PostgreSQL no infiere el tipo de un parámetro nulo
dentro de `LOWER()` y falla con `function lower(bytea) does not exist`.

**El esquema lo gobierna Flyway, y solo Flyway.** Hibernate corre en
`ddl-auto=validate`: valida que las entidades coincidan con las tablas, pero no
las modifica. Si una migración olvida una columna, el arranque falla ahí mismo
en vez de que Hibernate la cree en silencio.

**Los documentos oficiales guardan las firmas como texto**, no como referencia
al usuario: un documento emitido debe conservar el nombre que llevaba impreso
aunque después esa persona cambie de puesto o cause baja.

---

## Migraciones

En `src/main/resources/db/migration`. El detalle de cada versión está en su
[README](src/main/resources/db/migration/README.md).

| Versión | Contenido                                          |
| ------- | -------------------------------------------------- |
| V1–V3   | baseline, índices, integridad referencial          |
| V4      | `estadoticket` (tabla) → enum `EstadoTicket`       |
| V5      | `prioridad` → enum `Prioridad`                     |
| V6      | datos iniciales: roles, vistas, área, usuario admin |
| V7      | vista de historial de resguardos                   |
| V8      | encuesta de satisfacción                           |
| V9      | retirada del módulo de correos                     |
| V10     | tabla `dictamen` e historial unificado             |

> **No edites una migración ya aplicada.** Flyway valida su *checksum*: cambiar
> un archivo que ya corrió rompe el arranque en toda instalación donde se
> aplicó. Los cambios van siempre en una migración nueva.

---

## Generación de documentos

`core/documento/DocumentoService` produce los PDF con **OpenPDF** y el reporte
de actividades en Excel con **Apache POI**.

Los endpoints que recorren relaciones perezosas (`ticket.usuarioArea.area`,
`actividad.usuario.area`) van anotados con `@Transactional(readOnly = true)`.
Sin eso, la sesión de Hibernate se cierra al volver del repositorio y componer
el documento lanza `LazyInitializationException`.

El **dictamen técnico se registra al emitirse** (tabla `dictamen`, migración
V10). El guardado no bloquea la entrega del PDF: si falla, el técnico se queda
igualmente con su documento y el error consta en el log.

---

## Pruebas

```bash
sh mvnw test
```

Cubren la validación de la clave JWT y la emisión de tokens
(`security/JwtSecretValidatorTest`, `JwtTokenProviderTest`), además del arranque
del contexto.

---

## Perfiles

- `application.properties` — desarrollo. SQL visible en consola.
- `application-prod.properties` — producción. Se activa con
  `SPRING_PROFILES_ACTIVE=prod`, que ya fija `docker-compose.prod.yml`.

Ninguno contiene secretos: solo referencias `${VARIABLE}` que se resuelven desde
el entorno. Una variable ausente **aborta el arranque** a propósito, para no
desplegar sin darse cuenta con valores de ejemplo.
