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

/**
 * Alta y edición de usuario. Espeja `UsuarioRequest` y
 * `ActualizarUsuarioRequest`.
 *
 * El área es obligatoria salvo para ADMINISTRADOR, que no pertenece a ninguna
 * en concreto. Como esa regla depende del rol elegido, se comprueba con
 * `superRefine` sobre el conjunto y no en el campo.
 */
export const esquemaUsuario = (rolesPorId = {}) =>
    z.object({
        nombre: textoObligatorio('El nombre', 150),
        apellidoPaterno: textoObligatorio('El apellido paterno', 255),
        apellidoMaterno: textoOpcional('El apellido materno', 255),
        correo: z.string()
            .trim()
            .min(1, 'El correo es obligatorio.')
            .max(100, 'El correo no puede exceder 100 caracteres.')
            .email('Escribe un correo con formato válido, por ejemplo nombre@sedif.gob.mx.'),
        username: textoOpcional('El nombre de usuario', 50),
        rolId: z.string().min(1, 'Selecciona el rol del usuario.'),
        areaId: z.string().optional().or(z.literal('')),
    }).superRefine((datos, ctx) => {
        const rol = rolesPorId[datos.rolId];
        const exigeArea = rol && rol !== 'ADMINISTRADOR';

        if (exigeArea && !datos.areaId) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                path: ['areaId'],
                message: 'Selecciona el área a la que pertenece.',
            });
        }
    });

/** Alta y edición de un área. Espeja `AreaRecord`. */
export const esquemaArea = z.object({
    nombre: textoObligatorio('El nombre del área', 100, 3),
    prioritaria: z.boolean().optional(),
});

/** Alta y edición de un aviso. Espeja `AvisoRequestRecord`. */
export const esquemaAviso = z.object({
    titulo: textoObligatorio('El título', 150, 4),
    mensaje: textoObligatorio('El mensaje', 2000, 10),
    // Cadena vacía = aviso global para toda la institución.
    areaId: z.string().optional().or(z.literal('')),
});

/** Alta y edición de un equipo del catálogo. Espeja `EquipoRequest`. */
export const esquemaEquipo = z.object({
    descripcion: textoObligatorio('La descripción', 255, 3),
    marca: textoOpcional('La marca', 255),
    modelo: textoOpcional('El modelo', 255),
});

/**
 * Alta de un resguardo. Espeja `ResguardoRequest`.
 *
 * La duración solo se exige si el préstamo tiene plazo: con `indefinido`, el
 * resguardo queda sin fecha de vencimiento y la cantidad sobra.
 */
export const esquemaResguardo = z.object({
    solicitanteNombre: textoObligatorio('El nombre del solicitante', 100, 3),
    solicitanteNumero: textoObligatorio('El número de empleado', 30),
    departamento: textoOpcional('El departamento', 100),
    telefono: textoOpcional('El teléfono', 50),
    equipoNombre: textoObligatorio('El equipo entregado', 100, 3),
    numeroSerie: textoObligatorio('El número de serie', 50),
    numeroInventario: textoOpcional('El número de inventario', 50),
    condiciones: textoOpcional('Las condiciones', 500),
    accesorios: textoOpcional('Los accesorios', 500),
    duracionTipo: z.string().min(1, 'Indica la duración del préstamo.'),
    duracionCantidad: z.string().optional().or(z.literal('')),
}).superRefine((datos, ctx) => {
    if (datos.duracionTipo === 'indefinido') return;

    const cantidad = Number(datos.duracionCantidad);
    if (!datos.duracionCantidad || Number.isNaN(cantidad) || cantidad < 1 || cantidad > 365) {
        ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ['duracionCantidad'],
            message: 'Indica un número entre 1 y 365.',
        });
    }
});

/** Ingreso y actualización de un equipo en el taller. Espeja `EquipoReparacionRequest`. */
export const esquemaTaller = z.object({
    solicitanteNombre: textoObligatorio('El nombre del solicitante', 200, 3),
    solicitanteNumero: textoOpcional('El número de empleado', 50),
    departamento: textoOpcional('El departamento', 150),
    equipoTipo: textoObligatorio('El tipo de equipo', 100),
    marca: textoOpcional('La marca', 100),
    modelo: textoOpcional('El modelo', 100),
    numeroSerie: textoOpcional('El número de serie', 100),
    numeroInventario: textoOpcional('El número de inventario', 100),
    condicionRecepcion: textoOpcional('La condición de recepción', 500),
    accesorios: textoOpcional('Los accesorios', 500),
    fallaReportada: textoObligatorio('La falla reportada', 1000, 10),
    diagnostico: textoOpcional('El diagnóstico', 1000),
    solucion: textoOpcional('La solución', 1000),
});

/** Alta y edición de un correo institucional. Espeja `CorreoRequest`. */
export const esquemaCorreo = z.object({
    nombre: textoObligatorio('El nombre', 100, 2),
    apellidoPaterno: textoObligatorio('El apellido paterno', 100, 2),
    apellidoMaterno: textoOpcional('El apellido materno', 100),
    correo: z.string()
        .trim()
        .min(1, 'El correo es obligatorio.')
        .max(150, 'El correo no puede exceder 150 caracteres.')
        .email('Escribe un correo con formato válido, por ejemplo nombre@sedif.gob.mx.'),
    area: textoObligatorio('El área', 150, 2),
    cargo: textoOpcional('El cargo', 150),
    extension: textoOpcional('La extensión', 20),
    cuotaAlmacenamiento: textoOpcional('La cuota de almacenamiento', 30),
});

/** Resolución de un ticket. Espeja `ResolucionRequest`. */
export const esquemaResolucion = z.object({
    planTrabajoClave: z.string().min(1, 'Selecciona la meta del plan de trabajo.'),
    justificacion: textoObligatorio('La actividad de solución', 2000, 10),
});
