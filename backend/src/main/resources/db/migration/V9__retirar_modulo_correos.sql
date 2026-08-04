-- =========================================================================
-- V9 — Retirada del modulo de correos institucionales
-- =========================================================================
-- El area decidio dejar de gestionar las cuentas de correo desde este sistema,
-- asi que se retiran su tabla, su entrada de menu y el permiso asociado.
--
-- Se elimina tambien la pestana de mantenimiento preventivo del generador de
-- documentos, pero esa no deja rastro en la base: era un formulario que
-- construia un PDF al vuelo, sin persistir nada.
--
-- SOBRE EL BORRADO DE LA TABLA
--
-- `correo_institucional` no tiene claves foraneas que apunten a ella ni desde
-- ella, de modo que su retirada no arrastra ningun otro dato. Aun asi es una
-- operacion irreversible: si esta instalacion tiene cuentas registradas y se
-- quiere conservar el historial, exporte la tabla ANTES de aplicar esta
-- migracion.
--
--     \copy public.correo_institucional TO 'correos.csv' CSV HEADER
--
-- Idempotente: sobre una base donde ya no exista, no hace nada.
-- =========================================================================

-- ------------------------------------------- 1. ENTRADA DE MENU Y PERMISOS
-- El reparto rol_vista se borra primero: tiene clave foranea hacia vista.

DELETE FROM public.rol_vista
WHERE fn_vista_id IN (SELECT pn_id FROM public.vista WHERE s_ruta = '/admin/correos');

DELETE FROM public.vista WHERE s_ruta = '/admin/correos';

-- ------------------------------------------------------- 2. INDICE Y TABLA
-- El indice de V2 se retira explicitamente; la clave primaria y la restriccion
-- UNIQUE del correo caen solas al eliminar la tabla.

DROP INDEX IF EXISTS public.idx_correo_estado;

DROP TABLE IF EXISTS public.correo_institucional;
