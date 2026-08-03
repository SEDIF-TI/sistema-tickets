-- =========================================================================
-- V5 — La prioridad del ticket queda acotada al enum Prioridad
-- =========================================================================
-- La columna s_prioridad era texto libre con 'NORMAL' por defecto. El enum
-- Prioridad ya existia en el backend, pero la entidad guardaba un String sin
-- validar: cualquier texto entraba, y como el panel agrupa por esta columna,
-- un error de escritura creaba una categoria nueva en la grafica.
--
-- El formulario de alta, ademas, nunca enviaba el campo, asi que en la
-- practica todos los tickets quedaban en NORMAL y la grafica de prioridades
-- del panel era una sola barra.
-- =========================================================================

-- ------------------------------------------------- 1. NORMALIZACION
-- Se llevan los valores existentes al vocabulario del enum. Se contemplan las
-- variantes en minusculas y con espacios, y los sinonimos mas probables.

UPDATE public.ticket
SET s_prioridad = CASE UPPER(TRIM(COALESCE(s_prioridad, '')))
                      WHEN 'BAJA'     THEN 'BAJA'
                      WHEN 'LOW'      THEN 'BAJA'
                      WHEN 'NORMAL'   THEN 'NORMAL'
                      WHEN 'MEDIA'    THEN 'NORMAL'
                      WHEN 'MEDIO'    THEN 'NORMAL'
                      WHEN 'ALTA'     THEN 'ALTA'
                      WHEN 'HIGH'     THEN 'ALTA'
                      WHEN 'URGENTE'  THEN 'URGENTE'
                      WHEN 'CRITICA'  THEN 'URGENTE'
                      -- Vacio o desconocido: el valor por defecto del enum.
                      ELSE 'NORMAL'
                  END;

-- ------------------------------------------------- 2. RESTRICCIONES
-- Con todas las filas normalizadas, la base rechaza cualquier valor fuera del
-- enum aunque el codigo tuviera un fallo.

ALTER TABLE public.ticket
    ALTER COLUMN s_prioridad SET DEFAULT 'NORMAL';

ALTER TABLE public.ticket
    ALTER COLUMN s_prioridad SET NOT NULL;

ALTER TABLE public.ticket
    DROP CONSTRAINT IF EXISTS ticket_s_prioridad_check;

ALTER TABLE public.ticket
    ADD CONSTRAINT ticket_s_prioridad_check
    CHECK (s_prioridad IN ('BAJA', 'NORMAL', 'ALTA', 'URGENTE'));

COMMENT ON COLUMN public.ticket.s_prioridad IS
    'BAJA | NORMAL | ALTA | URGENTE. Ver el enum Prioridad del backend.';

-- ------------------------------------------------- 3. INDICE
-- La bandeja permite ordenar y filtrar por prioridad.

CREATE INDEX IF NOT EXISTS idx_ticket_prioridad ON public.ticket (s_prioridad);
