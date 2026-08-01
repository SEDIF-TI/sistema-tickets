-- =========================================================================
-- V3 — Integridad referencial y limpieza de columnas obsoletas
-- =========================================================================

-- ------------------------------------------------- 1. CLAVE FORANEA FALTANTE
-- equipo_reparacion.fn_ticket_id apunta a un ticket pero no tenia restriccion
-- de clave foranea, asi que la base de datos permitia guardar reparaciones
-- ligadas a tickets inexistentes y no impedia borrar un ticket referenciado.
--
-- Se usa NOT VALID: la restriccion se aplica a los registros nuevos sin
-- bloquear la tabla para validar los existentes. Los datos actuales tienen
-- fn_ticket_id nulo, asi que no hay nada que corregir, pero si en el futuro
-- se quiere validar el historial completo, basta con:
--     ALTER TABLE public.equipo_reparacion VALIDATE CONSTRAINT fk_reparacion_ticket;

ALTER TABLE public.equipo_reparacion
    DROP CONSTRAINT IF EXISTS fk_reparacion_ticket;

ALTER TABLE public.equipo_reparacion
    ADD CONSTRAINT fk_reparacion_ticket
    FOREIGN KEY (fn_ticket_id) REFERENCES public.ticket (pn_id)
    NOT VALID;

-- ------------------------------------------- 2. COLUMNAS DUPLICADAS EN RESGUARDO
-- La tabla resguardo arrastra cuatro columnas duplicadas de una version
-- anterior del modulo:
--
--     condiciones          <-->  s_condiciones
--     departamento         <-->  s_departamento
--     numero_inventario    <-->  s_numero_inventario
--     telefono             <-->  s_telefono
--
-- La entidad Resguardo.java mapea unicamente las variantes con prefijo s_,
-- que son las que reciben datos desde que el modulo esta en uso. Las cuatro
-- sin prefijo estan a NULL en todos los registros del volcado revisado.
--
-- NO se eliminan en esta migracion. Borrar columnas es irreversible y
-- conviene que el equipo confirme antes que ningun proceso externo (reportes,
-- consultas manuales, respaldos) las esta leyendo. Se marcan como obsoletas
-- para que quede constancia en el propio esquema.
--
-- Cuando se confirme, la retirada consiste en anadir una V4 con:
--
--     ALTER TABLE public.resguardo
--         DROP COLUMN IF EXISTS condiciones,
--         DROP COLUMN IF EXISTS departamento,
--         DROP COLUMN IF EXISTS numero_inventario,
--         DROP COLUMN IF EXISTS telefono;

COMMENT ON COLUMN public.resguardo.condiciones IS
    'OBSOLETA: sustituida por s_condiciones. Pendiente de eliminar.';
COMMENT ON COLUMN public.resguardo.departamento IS
    'OBSOLETA: sustituida por s_departamento. Pendiente de eliminar.';
COMMENT ON COLUMN public.resguardo.numero_inventario IS
    'OBSOLETA: sustituida por s_numero_inventario. Pendiente de eliminar.';
COMMENT ON COLUMN public.resguardo.telefono IS
    'OBSOLETA: sustituida por s_telefono. Pendiente de eliminar.';

-- --------------------------------------------- 3. DOCUMENTACION DEL ESQUEMA
-- Columnas cuyo proposito no es evidente por su nombre.

COMMENT ON COLUMN public.rol.s_nivel_vision IS
    'GLOBAL = ve todos los tickets; AREA = solo los de su area; '
    'PERSONAL = los asignados a el y los que creo.';

COMMENT ON COLUMN public.ticket.plan_trabajo_clave IS
    'Clave de la meta del plan anual de trabajo a la que se imputa la '
    'resolucion. Ver el enum PlanTrabajo del backend.';

COMMENT ON COLUMN public.actividad_extra.plan_trabajo_clave IS
    'Clave de la meta del plan anual de trabajo. Ver el enum PlanTrabajo.';

COMMENT ON COLUMN public.resguardo.s_estado_resguardo IS
    'ENTREGADO | DEVUELTO | VENCIDO. La revision nocturna pasa a VENCIDO los '
    'prestamos entregados cuya fecha de vencimiento ya paso.';

-- Columnas del ticket que no tienen equivalente en la entidad Ticket.java:
-- conservan datos historicos pero el backend actual no las lee ni escribe.
COMMENT ON COLUMN public.ticket.s_justificacion IS
    'Historica: la justificacion vigente se guarda en la tabla bitacora. '
    'Sin mapear en la entidad Ticket.';
