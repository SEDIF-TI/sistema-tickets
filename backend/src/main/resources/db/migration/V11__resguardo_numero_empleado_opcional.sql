-- =========================================================================
-- V11 — El numero de empleado deja de ser obligatorio en los resguardos
-- =========================================================================
-- El formulario de responsiva lo rellena el personal de soporte en el momento
-- de entregar el equipo, y no siempre tiene a mano el numero de empleado de
-- quien lo recibe. Exigirlo obligaba a inventar un valor de relleno, que es
-- peor que no tenerlo: un dato falso en un documento firmado.
--
-- La columna pasa a aceptar nulos. No se toca ninguna fila existente: las que
-- ya tienen numero lo conservan.
--
-- Idempotente: DROP NOT NULL sobre una columna que ya admite nulos no hace
-- nada.
-- =========================================================================

ALTER TABLE public.resguardo
    ALTER COLUMN s_solicitante_numero DROP NOT NULL;
