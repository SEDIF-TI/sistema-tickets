-- =========================================================================
-- V8 — Encuesta de satisfaccion del solicitante
-- =========================================================================
-- Al cerrar su ticket, quien lo levanto puede calificar el servicio recibido:
-- malo, regular o bueno. El resultado alimenta la grafica de calificacion por
-- tecnico del panel de control.
--
-- POR QUE EN LA TABLA `ticket` Y NO EN UNA TABLA APARTE
--
-- La relacion es uno a uno estricta: un ticket admite como mucho una encuesta,
-- y una encuesta no existe sin su ticket. Una tabla propia solo aportaria un
-- JOIN mas en cada consulta del panel y la posibilidad de quedar huerfana. El
-- tecnico calificado tampoco se guarda aparte: es el que ya figura en
-- fn_usuario_soporte_id, y duplicarlo abriria la puerta a que ambos dejaran de
-- coincidir.
--
-- Las tres columnas son NULL a proposito: la encuesta es voluntaria y solo
-- existe para los tickets cerrados por su solicitante. Un NULL significa "sin
-- responder", que no es lo mismo que una mala calificacion, y los promedios
-- deben ignorarlo en lugar de contarlo como cero.
-- =========================================================================

ALTER TABLE public.ticket
    ADD COLUMN IF NOT EXISTS s_calificacion varchar(20);

ALTER TABLE public.ticket
    ADD COLUMN IF NOT EXISTS s_comentario_encuesta varchar(500);

ALTER TABLE public.ticket
    ADD COLUMN IF NOT EXISTS d_fecha_encuesta timestamp(6);

-- La base rechaza cualquier valor fuera del enum aunque el codigo fallara.
ALTER TABLE public.ticket
    DROP CONSTRAINT IF EXISTS ticket_s_calificacion_check;

ALTER TABLE public.ticket
    ADD CONSTRAINT ticket_s_calificacion_check
    CHECK (s_calificacion IS NULL OR s_calificacion IN ('MALO', 'REGULAR', 'BUENO'));

COMMENT ON COLUMN public.ticket.s_calificacion IS
    'MALO | REGULAR | BUENO. Encuesta que responde el solicitante al cerrar su '
    'ticket. NULL = sin responder, que no equivale a una mala calificacion. '
    'Ver el enum Calificacion del backend.';

COMMENT ON COLUMN public.ticket.s_comentario_encuesta IS
    'Comentario opcional del solicitante al calificar el servicio.';

COMMENT ON COLUMN public.ticket.d_fecha_encuesta IS
    'Momento en que se respondio la encuesta. Permite medir por periodo sin '
    'confundirlo con la fecha de cierre del ticket.';

-- El panel agrupa por tecnico filtrando los tickets ya calificados.
CREATE INDEX IF NOT EXISTS idx_ticket_calificacion
    ON public.ticket (s_calificacion)
    WHERE s_calificacion IS NOT NULL;
