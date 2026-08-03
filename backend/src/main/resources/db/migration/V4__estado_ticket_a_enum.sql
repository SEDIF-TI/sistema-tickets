-- =========================================================================
-- V4 — El estado del ticket pasa de tabla catalogo a enum
-- =========================================================================
-- La tabla `estadoticket` era un catalogo de tres filas (ABIERTO, EN PROCESO,
-- CERRADO) que el codigo trataba de todos modos como constantes: las buscaba
-- con findByNombre("ABIERTO") escrito literalmente, y las transiciones entre
-- estados viven en el servicio, no en la tabla. Anadir una fila nunca habria
-- creado un flujo nuevo.
--
-- A cambio, la tabla causaba dos problemas reales:
--
--   1. Nada la sembraba. Una base recien creada arrancaba con el catalogo
--      vacio y el sistema fallaba al crear el primer ticket, al registrar el
--      aviso de "voy en camino" y al cerrarlo: los tres por estatus
--      inexistente, y sin mensaje que apuntara a la causa.
--   2. El endpoint de administracion permitia dar de alta estatus arbitrarios
--      que ninguna parte del codigo sabia interpretar, dejando tickets en
--      estados muertos que ningun filtro mostraba.
--
-- El estado pasa a la columna `s_estado` del propio ticket, validada por el
-- enum EstadoTicket del backend. Es el mismo criterio que ya seguian el taller
-- (EstadoTaller), los correos (EstadoCorreo) y los resguardos.
--
-- La migracion es reversible hasta el paso 5: hasta ese punto conviven la
-- columna nueva y la antigua.
-- =========================================================================

-- ------------------------------------------------- 1. COLUMNA NUEVA
-- Se crea aceptando nulos: los registros existentes aun no tienen valor.

ALTER TABLE public.ticket
    ADD COLUMN IF NOT EXISTS s_estado varchar(20);

-- ------------------------------------------------- 2. TRASLADO DE LOS DATOS
-- Se copia el nombre desde el catalogo y se normaliza al vocabulario del enum:
-- el espacio de "EN PROCESO" pasa a guion bajo, y los sinonimos historicos se
-- unifican. "RESUELTO" y "ASIGNADO" aparecian en el codigo del frontend como
-- estados posibles, asi que se contemplan por si algun registro los usa.

UPDATE public.ticket t
SET s_estado = CASE UPPER(TRIM(e.s_nombre))
                   WHEN 'EN PROCESO' THEN 'EN_PROCESO'
                   WHEN 'EN_PROCESO' THEN 'EN_PROCESO'
                   WHEN 'ASIGNADO'   THEN 'EN_PROCESO'
                   WHEN 'RESUELTO'   THEN 'CERRADO'
                   WHEN 'CERRADO'    THEN 'CERRADO'
                   WHEN 'ABIERTO'    THEN 'ABIERTO'
                   -- Cualquier estado desconocido se trata como abierto: es
                   -- preferible que el ticket siga visible y atendible a que
                   -- desaparezca de la bandeja del tecnico.
                   ELSE 'ABIERTO'
               END
FROM public.estadoticket e
WHERE t.fn_estadoticket_id = e.pn_id
  AND t.s_estado IS NULL;

-- Red de seguridad para tickets que hubieran quedado sin correspondencia.
UPDATE public.ticket
SET s_estado = 'ABIERTO'
WHERE s_estado IS NULL;

-- ------------------------------------------------- 3. RESTRICCIONES
-- Ya con todas las filas informadas, la columna pasa a obligatoria y se acota
-- a los valores del enum: asi la base rechaza un estado invalido aunque el
-- codigo tuviera un fallo.

ALTER TABLE public.ticket
    ALTER COLUMN s_estado SET NOT NULL;

ALTER TABLE public.ticket
    DROP CONSTRAINT IF EXISTS ticket_s_estado_check;

ALTER TABLE public.ticket
    ADD CONSTRAINT ticket_s_estado_check
    CHECK (s_estado IN ('ABIERTO', 'EN_PROCESO', 'CERRADO'));

COMMENT ON COLUMN public.ticket.s_estado IS
    'ABIERTO | EN_PROCESO | CERRADO. Ver el enum EstadoTicket del backend. '
    'Sustituye a la tabla catalogo estadoticket, retirada en esta migracion.';

-- ------------------------------------------------- 4. INDICE
-- El indice de V2 apuntaba a la clave foranea, que deja de existir. Se
-- sustituye por el equivalente sobre la columna nueva: la bandeja filtra por
-- estado en casi todas sus consultas.

DROP INDEX IF EXISTS public.idx_ticket_estatus;

CREATE INDEX IF NOT EXISTS idx_ticket_estado ON public.ticket (s_estado);

-- La bandeja del tecnico ordena por fecha dentro de un estado concreto.
CREATE INDEX IF NOT EXISTS idx_ticket_estado_fecha
    ON public.ticket (s_estado, d_fecha_creacion DESC);

-- ------------------------------------------------- 5. RETIRADA DEL CATALOGO
-- Se elimina primero la clave foranea y su columna, y despues la tabla.

ALTER TABLE public.ticket
    DROP COLUMN IF EXISTS fn_estadoticket_id;

DROP TABLE IF EXISTS public.estadoticket;
