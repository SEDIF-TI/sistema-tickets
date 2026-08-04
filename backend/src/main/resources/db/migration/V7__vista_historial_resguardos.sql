-- =========================================================================
-- V7 — Vista de historial de resguardos en el menu de administracion
-- =========================================================================
-- La pantalla HistorialResguardos.jsx existia en el proyecto pero ninguna
-- ruta la alcanzaba: era codigo inalcanzable, imposible de abrir desde la
-- interfaz. Se le da ruta en App.jsx y aqui se registra su entrada de menu
-- para el rol ADMINISTRADOR.
--
-- No se confunde con /soporte/resguardos: alli se dan de alta los prestamos y
-- se registran las devoluciones; esta es la vista de seguimiento y consulta,
-- con reimpresion del documento.
--
-- Idempotente: sobre una base que ya tenga la fila, no cambia nada.
-- =========================================================================

INSERT INTO public.vista (s_nombre, s_ruta, s_icono, b_activo) VALUES
    ('Historial de resguardos', '/admin/resguardos', 'InventoryIcon', true)
ON CONFLICT (s_ruta) DO NOTHING;

INSERT INTO public.rol_vista (fn_rol_id, fn_vista_id)
SELECT r.pn_id, v.pn_id
FROM public.rol r, public.vista v
WHERE r.s_nombre = 'ADMINISTRADOR'
  AND v.s_ruta = '/admin/resguardos'
  AND NOT EXISTS (
      SELECT 1 FROM public.rol_vista rv
      WHERE rv.fn_rol_id = r.pn_id AND rv.fn_vista_id = v.pn_id
  );
