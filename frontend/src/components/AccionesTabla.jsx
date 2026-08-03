import { Stack, IconButton, Tooltip } from '@mui/material';

/**
 * Botonera de acciones de una fila.
 *
 * Sustituye a los botones de texto ("Editar", "Baja", "Reset Clave") que
 * ocupaban media tabla y obligaban a ensancharla. Cada acción es un icono con
 * su tooltip al pasar el cursor.
 *
 * Accesibilidad: el tooltip **no** aporta nombre accesible, así que cada botón
 * lleva su propio `aria-label`. Sin él, un lector de pantalla anuncia
 * "botón" y nada más, y la tabla se vuelve inutilizable con teclado.
 *
 * Uso:
 *
 *   <AccionesTabla
 *     acciones={[
 *       { icono: <EditIcon />, titulo: 'Editar', onClick: () => editar(fila),
 *         etiqueta: `Editar a ${fila.nombre}` },
 *       { icono: <DeleteIcon />, titulo: 'Eliminar', color: 'error',
 *         onClick: () => borrar(fila), etiqueta: `Eliminar ${fila.nombre}` },
 *     ]}
 *   />
 *
 * @param {Array} acciones Lista de acciones. Cada una admite:
 *   - `icono`     (obligatorio) elemento de icono.
 *   - `titulo`    (obligatorio) texto del tooltip.
 *   - `onClick`   (obligatorio) manejador.
 *   - `etiqueta`  descripción para lectores de pantalla; si se omite se usa
 *                 `titulo`. Conviene incluir a quién afecta la acción.
 *   - `color`     color del tema ('error', 'success'…). Por defecto, heredado.
 *   - `oculta`    si es `true`, la acción no se dibuja.
 *   - `deshabilitada` y `motivoDeshabilitada` para explicar el porqué.
 */
export default function AccionesTabla({ acciones = [], justificar = 'center' }) {
    const visibles = acciones.filter((a) => !a.oculta);

    if (visibles.length === 0) {
        return null;
    }

    return (
        <Stack
            direction="row"
            spacing={0.5}
            justifyContent={justificar}
            // Evita que el clic en una acción dispare también el onClick de la
            // fila cuando la tabla es clicable.
            onClick={(e) => e.stopPropagation()}
        >
            {visibles.map((accion, indice) => {
                const deshabilitada = Boolean(accion.deshabilitada);
                const textoTooltip = deshabilitada
                    ? accion.motivoDeshabilitada || accion.titulo
                    : accion.titulo;

                const boton = (
                    <IconButton
                        size="small"
                        color={accion.color || 'default'}
                        onClick={accion.onClick}
                        disabled={deshabilitada}
                        aria-label={accion.etiqueta || accion.titulo}
                    >
                        {accion.icono}
                    </IconButton>
                );

                return (
                    <Tooltip key={accion.titulo ?? indice} title={textoTooltip} arrow>
                        {/* El span es necesario: MUI no puede posicionar un
                            tooltip sobre un botón deshabilitado, que no emite
                            eventos de ratón. */}
                        <span>{boton}</span>
                    </Tooltip>
                );
            })}
        </Stack>
    );
}
