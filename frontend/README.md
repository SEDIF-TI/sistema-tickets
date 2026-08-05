# Frontend — Sistema de Tickets

Interfaz en **React 19** con **Vite 8** y **MUI 9**.

```bash
cp .env.example .env    # solo la primera vez
npm install
npm run dev
```

Queda en `http://localhost:5173`. Si aparece en el **5174**, hay un Vite
anterior sin cerrar: `pkill -f vite`.

Necesita el backend levantado en `http://localhost:8080`; si no, la interfaz
carga pero sale sin datos. Consulta la [guía de la raíz](../README.md).

| Comando           | Qué hace                        |
| ----------------- | ------------------------------- |
| `npm run dev`     | servidor de desarrollo con HMR  |
| `npm run build`   | compila a `dist/`               |
| `npm run preview` | sirve lo compilado              |
| `npm run lint`    | ESLint                          |

### Variables

Van en `frontend/.env` (distinto del `.env` de la raíz, que es del backend):

| Variable                     | Para qué                                      |
| ---------------------------- | --------------------------------------------- |
| `VITE_API_URL`               | URL base de la API                            |
| `VITE_TELEGRAM_BOT_USERNAME` | nombre público del bot, para el enlace de vinculación |

> Todo lo que empiece por `VITE_` **se incrusta en el bundle** que descarga el
> navegador: es información pública. Nunca pongas ahí contraseñas ni tokens.

---

## Estructura

```
pages/          Una carpeta por rol
  admin/          panel, usuarios, áreas, avisos, equipos, historial
  soporte/        bandeja, taller, resguardos
  empleado/       levantamiento de tickets
  tickets/        listado compartido
components/     Componentes transversales
  DynamicTable      tabla del sistema: búsqueda, filtros, orden, paginación
  formato/          formularios de los documentos oficiales
context/        Sesión, notificaciones y WebSocket
hooks/          useTablaPaginada, useRol, useNetworkStatus
services/       Un archivo por dominio; espejo de los controladores del backend
util/           Formato, enums, paleta de gráficas
theme/          Tema MUI
```

---

## Convenciones

**El menú lo define el backend.** Las entradas salen de la tabla `vista` según
el rol, y se refrescan con `GET /v1/perfil/vistas` al entrar. No se escriben a
mano en el JSX ni se cachean en `localStorage`: una vista retirada debe
desaparecer del menú sin obligar a cerrar sesión.

**Los catálogos se piden al backend** (`/plan-trabajo`, `/prioridades`,
`/calificaciones`, `/estados`), nunca se copian al código.

**La lógica compara la constante del enum, no su etiqueta.** El backend envía
los dos campos (`estatus` y `estatusEtiqueta`); el segundo es solo para mostrar.
Cambiar un texto en pantalla nunca debe alterar una condición.

**Paginación, búsqueda y filtros los resuelve el servidor.** `DynamicTable`
muestra los controles y avisa del cambio, pero no trocea ni reordena en memoria:
solo tiene la página actual, no el conjunto completo. El estado lo lleva
`useTablaPaginada`.

**MUI es la base; React Hook Form + Zod solo en formularios**, porque MUI no
gestiona estado ni validación.

**La paleta de gráficas está validada para daltonismo**
(`util/paletaGraficas.js`). El color sigue siempre a la categoría, nunca a la
posición: la misma serie conserva su color aunque cambie el orden.

**En pantallas estrechas las tablas se convierten en tarjetas.** Una fila de
ocho columnas es ilegible en un teléfono, incluso con scroll horizontal.

---

## Notas conocidas

- **Ordenar por prioridad ordena alfabéticamente** por el nombre del enum
  (`ALTA`, `BAJA`, `NORMAL`, `URGENTE`), no por urgencia real. Sirve para
  agrupar; lo urgente se distingue por color.
- Quedan unos diez avisos de ESLint por el patrón `setState` dentro de efectos
  asíncronos, que el linter no distingue del caso problemático. Están en
  archivos ya revisados.
