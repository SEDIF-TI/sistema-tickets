-- =========================================================================
-- V2 — Indices
-- =========================================================================
-- PostgreSQL crea indices automaticamente para las claves primarias y las
-- restricciones UNIQUE, pero NO para las claves foraneas. Estas columnas se
-- usan en casi todas las consultas del sistema (filtros por area, por
-- tecnico, por estatus), asi que sin indice cada consulta recorre la tabla
-- completa.
--
-- Se usa IF NOT EXISTS para que la migracion sea segura de reejecutar.
-- =========================================================================

-- ---------------------------------------------------------------- USUARIO
-- El filtro JWT resuelve el rol en cada peticion autenticada.
CREATE INDEX IF NOT EXISTS idx_usuario_rol   ON public.usuario (fn_rol_id);
CREATE INDEX IF NOT EXISTS idx_usuario_area  ON public.usuario (fn_area_id);
-- El balanceador busca tecnicos activos y disponibles en cada alta de ticket.
CREATE INDEX IF NOT EXISTS idx_usuario_disponible_soporte
    ON public.usuario (b_disponible_soporte, b_activo);

-- ----------------------------------------------------------------- TICKET
-- Sostienen las tres estrategias de visibilidad (GLOBAL, AREA, PERSONAL).
CREATE INDEX IF NOT EXISTS idx_ticket_estatus       ON public.ticket (fn_estadoticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_usuario_area  ON public.ticket (fn_usuario_area_id);
CREATE INDEX IF NOT EXISTS idx_ticket_soporte       ON public.ticket (fn_usuario_soporte_id);
-- La bandeja siempre ordena por fecha descendente.
CREATE INDEX IF NOT EXISTS idx_ticket_fecha_creacion
    ON public.ticket (d_fecha_creacion DESC);
-- Metricas del panel agrupadas por prioridad.
CREATE INDEX IF NOT EXISTS idx_ticket_prioridad     ON public.ticket (s_prioridad);

-- --------------------------------------------------------------- BITACORA
-- El historial se consulta por ticket y ordenado por fecha.
CREATE INDEX IF NOT EXISTS idx_bitacora_ticket
    ON public.bitacora (fn_ticket_id, d_fecha_creacion DESC);

-- ------------------------------------------------------------------ AREA
CREATE INDEX IF NOT EXISTS idx_area_soporte_fijo ON public.area (fn_soporte_fijo_id);

-- ----------------------------------------------------------------- AVISO
-- El panel consulta los avisos vigentes cada 30 segundos.
CREATE INDEX IF NOT EXISTS idx_aviso_activo
    ON public.aviso (b_activo, b_eliminado);
CREATE INDEX IF NOT EXISTS idx_aviso_area ON public.aviso (pn_area_id);

-- ------------------------------------------------------------- RESGUARDO
-- La revision nocturna busca por estado y fecha de vencimiento.
CREATE INDEX IF NOT EXISTS idx_resguardo_estado_vencimiento
    ON public.resguardo (s_estado_resguardo, d_fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_resguardo_creador
    ON public.resguardo (fn_usuario_creador_id);
CREATE INDEX IF NOT EXISTS idx_resguardo_fecha_creacion
    ON public.resguardo (d_fecha_creacion DESC);

-- ------------------------------------------------------ EQUIPO_REPARACION
CREATE INDEX IF NOT EXISTS idx_reparacion_estado  ON public.equipo_reparacion (s_estado_taller);
CREATE INDEX IF NOT EXISTS idx_reparacion_tecnico ON public.equipo_reparacion (fn_tecnico_id);
CREATE INDEX IF NOT EXISTS idx_reparacion_ticket  ON public.equipo_reparacion (fn_ticket_id);

-- --------------------------------------------------- CORREO_INSTITUCIONAL
CREATE INDEX IF NOT EXISTS idx_correo_estado ON public.correo_institucional (s_estado);

-- ------------------------------------------------------- ACTIVIDAD_EXTRA
CREATE INDEX IF NOT EXISTS idx_actividad_usuario ON public.actividad_extra (fn_usuario_id);
CREATE INDEX IF NOT EXISTS idx_actividad_plan    ON public.actividad_extra (plan_trabajo_clave);
CREATE INDEX IF NOT EXISTS idx_actividad_fecha
    ON public.actividad_extra (d_fecha_actividad DESC);

-- -------------------------------------------------------------- ROL_VISTA
-- Se consulta en cada login para construir el menu del usuario.
CREATE INDEX IF NOT EXISTS idx_rol_vista_rol ON public.rol_vista (fn_rol_id);
