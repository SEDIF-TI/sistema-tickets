-- =========================================================================
-- V6 — Datos minimos para que el sistema sea utilizable
-- =========================================================================
-- Una instalacion limpia quedaba sin roles, sin vistas y sin usuarios: la
-- aplicacion arrancaba correctamente pero nadie podia entrar, y no habia
-- forma de crear el primer usuario porque el alta exige estar autenticado
-- como ADMINISTRADOR. El unico camino era insertar filas a mano en la base.
--
-- Es el mismo problema que tenia la tabla `estadoticket` antes de la V4: el
-- esquema se creaba pero los datos de los que depende el arranque vivian solo
-- en la base heredada del volcado.
--
-- Todo va con ON CONFLICT DO NOTHING / WHERE NOT EXISTS, asi que sobre una
-- base que ya tiene estos datos la migracion no cambia nada.
-- =========================================================================

-- ------------------------------------------------------------- 1. ROLES
-- s_nivel_vision decide que tickets ve cada rol; lo consume TicketService.
--   GLOBAL   = todos los tickets de la institucion
--   PERSONAL = los asignados al tecnico y los que el mismo levanto
--   AREA     = solo los del area del usuario

INSERT INTO public.rol (s_nombre, s_nivel_vision) VALUES
    ('ADMINISTRADOR', 'GLOBAL'),
    ('SOPORTE',       'PERSONAL'),
    ('EMPLEADO',      'AREA')
ON CONFLICT (s_nombre) DO NOTHING;

-- ------------------------------------------------------------- 2. VISTAS
-- Cada fila es una entrada del menu lateral. s_ruta debe coincidir con una
-- ruta declarada en App.jsx, y s_icono con una clave del mapa ICONOS de
-- MainLayout.jsx: un icono desconocido cae en el generico.

INSERT INTO public.vista (s_nombre, s_ruta, s_icono, b_activo) VALUES
    ('Panel principal',    '/admin/dashboard',    'DashboardIcon',           true),
    ('Usuarios',           '/admin/usuarios',     'GroupIcon',               true),
    ('Areas',              '/admin/areas',        'DomainIcon',              true),
    ('Avisos',             '/admin/avisos',       'CampaignIcon',            true),
    ('Bitacora',           '/admin/bitacora',     'HistoryIcon',             true),
    ('Equipos',            '/admin/equipos',      'ComputerIcon',            true),
    ('Correos',            '/admin/correos',      'MailIcon',                true),
    ('Bandeja',            '/soporte/bandeja',    'AssignmentIcon',          true),
    ('Taller',             '/soporte/taller',     'HomeRepairServiceIcon',   true),
    ('Resguardos',         '/soporte/resguardos', 'InventoryIcon',           true),
    ('Documentos',         '/documentos/crear',   'DescriptionIcon',         true),
    ('Levantar ticket',    '/empleado/nuevo',     'AddCircleIcon',           true),
    ('Mis tickets',        '/empleado/historial', 'HistoryIcon',             true)
ON CONFLICT (s_ruta) DO NOTHING;

-- --------------------------------------------------- 3. VISTAS POR ROL
-- ADMINISTRADOR: todo el modulo de administracion y los documentos.

INSERT INTO public.rol_vista (fn_rol_id, fn_vista_id)
SELECT r.pn_id, v.pn_id
FROM public.rol r, public.vista v
WHERE r.s_nombre = 'ADMINISTRADOR'
  AND v.s_ruta IN ('/admin/dashboard', '/admin/usuarios', '/admin/areas',
                   '/admin/avisos', '/admin/bitacora', '/admin/equipos',
                   '/admin/correos', '/documentos/crear')
  AND NOT EXISTS (
      SELECT 1 FROM public.rol_vista rv
      WHERE rv.fn_rol_id = r.pn_id AND rv.fn_vista_id = v.pn_id
  );

-- SOPORTE: su bandeja, el taller, los resguardos, los correos, el alta de
-- tickets y los documentos que genera.

INSERT INTO public.rol_vista (fn_rol_id, fn_vista_id)
SELECT r.pn_id, v.pn_id
FROM public.rol r, public.vista v
WHERE r.s_nombre = 'SOPORTE'
  AND v.s_ruta IN ('/soporte/bandeja', '/soporte/taller', '/soporte/resguardos',
                   '/admin/correos', '/empleado/nuevo', '/documentos/crear')
  AND NOT EXISTS (
      SELECT 1 FROM public.rol_vista rv
      WHERE rv.fn_rol_id = r.pn_id AND rv.fn_vista_id = v.pn_id
  );

-- EMPLEADO: levantar tickets y consultar los de su area.

INSERT INTO public.rol_vista (fn_rol_id, fn_vista_id)
SELECT r.pn_id, v.pn_id
FROM public.rol r, public.vista v
WHERE r.s_nombre = 'EMPLEADO'
  AND v.s_ruta IN ('/empleado/nuevo', '/empleado/historial')
  AND NOT EXISTS (
      SELECT 1 FROM public.rol_vista rv
      WHERE rv.fn_rol_id = r.pn_id AND rv.fn_vista_id = v.pn_id
  );

-- ------------------------------------------------------------- 4. AREA
-- Area de destino del primer usuario. El alta de usuarios exige un area.

INSERT INTO public.area (s_nombre, b_activo, b_prioritaria, s_creado_por, d_fecha_creacion)
SELECT 'Tecnologias de la Informacion', true, true, 'sistema', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.area WHERE s_nombre = 'Tecnologias de la Informacion'
);

-- --------------------------------------------------- 5. USUARIO INICIAL
-- Cuenta con la que se entra por primera vez para crear el resto.
--
-- b_password_temporal = true fuerza el cambio de contrasena en el primer
-- acceso (lo comprueba PrimerCambioPassword en el frontend), asi que la
-- credencial de arranque no sobrevive al primer inicio de sesion.
--
-- La contrasena inicial es 'Sedif2026#' y viaja como hash BCrypt: la version
-- en claro no aparece en ningun archivo del repositorio.
--
-- CAMBIALA EN EL PRIMER ACCESO. Si esta instalacion es de produccion,
-- ademas conviene borrar esta cuenta una vez creada la del administrador
-- real.

INSERT INTO public.usuario (
    s_nombre, s_apellido_paterno, s_correo, s_username, s_password,
    b_activo, b_disponible_soporte, b_password_temporal,
    fn_rol_id, fn_area_id, s_creado_por, d_fecha_creacion
)
SELECT
    'Administrador', 'del Sistema',
    'admin@sedif.local', 'admin',
    '$2a$10$QDLjYG/9JTFWw5oEVg.ZVeBVEL7ERnJyC/B77yNt45xmVua2ggnkW',
    true, false, true,
    (SELECT pn_id FROM public.rol WHERE s_nombre = 'ADMINISTRADOR'),
    (SELECT pn_id FROM public.area WHERE s_nombre = 'Tecnologias de la Informacion'),
    'sistema', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.usuario WHERE s_correo = 'admin@sedif.local'
);
