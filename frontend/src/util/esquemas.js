import { z } from 'zod';

/**
 * Esquemas de validación de los formularios.
 *
 * Cada pantalla validaba a mano, con una cadena de `if` dentro del manejador
 * de envío: el usuario descubría los errores de uno en uno, solo al pulsar el
 * botón, y las reglas no coincidían con las del backend.
 *
 * Aquí las reglas viven en un único sitio y **espejan las anotaciones de
 * validación de los DTO de Java**. El servidor sigue siendo la autoridad —
 * estas comprobaciones solo evitan el viaje de ida y vuelta y señalan el campo
 * exacto.
 *
 * Al cambiar un límite, hay que cambiarlo también en el DTO correspondiente.
 */

/** Texto obligatorio con mensaje propio, ya recortado. */
const textoObligatorio = (campo, maximo, minimo = 1) =>
    z.string()
        .trim()
        .min(minimo, minimo === 1
            ? `${campo} es obligatorio.`
            : `${campo} debe tener al menos ${minimo} caracteres.`)
        .max(maximo, `${campo} no puede exceder ${maximo} caracteres.`);

/** Texto opcional: cadena vacía y `undefined` son equivalentes. */
const textoOpcional = (campo, maximo) =>
    z.string()
        .trim()
        .max(maximo, `${campo} no puede exceder ${maximo} caracteres.`)
        .optional()
        .or(z.literal(''));

/**
 * Alta de ticket. Espeja `TicketRequestRecord` del backend.
 *
 * La sede es obligatoria solo para soporte y administración, que levantan
 * tickets de sedes distintas a la suya; el empleado siempre reporta desde la
 * propia. Como esa regla depende del rol, se aplica con `superRefine` en lugar
 * de marcarla en el campo.
 */
export const esquemaTicket = (exigeSede = false) =>
    z.object({
        solicitante: textoObligatorio('El nombre de la persona afectada', 100),
        titulo: textoObligatorio('La falla principal', 255, 5),
        descripcion: textoObligatorio('La descripción', 5000, 15),
        sede: textoOpcional('La sede', 255),
        prioridad: z.string().min(1, 'Selecciona una prioridad.'),
        usuarioSoporteId: z.string().optional().or(z.literal('')),
    }).superRefine((datos, ctx) => {
        if (exigeSede && !datos.sede?.trim()) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                path: ['sede'],
                message: 'Indica la sede donde ocurre la falla.',
            });
        }
    });

/** Resolución de un ticket. Espeja `ResolucionRequest`. */
export const esquemaResolucion = z.object({
    planTrabajoClave: z.string().min(1, 'Selecciona la meta del plan de trabajo.'),
    justificacion: textoObligatorio('La actividad de solución', 2000, 10),
});
