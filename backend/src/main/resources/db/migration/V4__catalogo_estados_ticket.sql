-- =========================================================================
-- V4 — Catalogo de estados del ticket
-- =========================================================================
-- La tabla `estadoticket` se creaba vacia en el baseline y ningun punto del
-- codigo la sembraba: los estados existian solo en la base de datos de
-- produccion, heredados del volcado original.
--
-- En una instalacion limpia eso deja el sistema inutilizable sin que nada lo
-- advierta hasta que un usuario lo usa:
--
--   * crear un ticket falla con "Estatus ABIERTO no configurado en la base de
--     datos", porque TicketService lo busca por nombre;
--   * el aviso de "voy en camino" falla con "El estatus EN PROCESO no existe
--     en la BD";
--   * cerrar un ticket falla por el mismo motivo con CERRADO.
--
-- Los tres nombres son los que el codigo busca literalmente con
-- findByNombre(), asi que deben escribirse exactamente asi, en mayusculas y
-- con el espacio de "EN PROCESO".
--
-- ON CONFLICT DO NOTHING hace la migracion idempotente y segura sobre las
-- bases que ya tienen estos registros: no los duplica ni cambia sus ids, de
-- modo que los tickets existentes conservan su estado.
-- =========================================================================

INSERT INTO public.estadoticket (s_nombre) VALUES
    ('ABIERTO'),
    ('EN PROCESO'),
    ('CERRADO')
ON CONFLICT (s_nombre) DO NOTHING;
