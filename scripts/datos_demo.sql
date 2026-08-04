-- =========================================================================
-- DATOS DE DEMOSTRACION — 1 de mayo a 4 de agosto de 2026
-- =========================================================================
-- Genera tickets, dictamenes de taller y resguardos repartidos por ese
-- periodo, para poder ver el comportamiento del panel con volumen realista.
--
-- NO es una migracion de Flyway: es un script que se ejecuta a mano sobre un
-- entorno de pruebas. Los datos ficticios no deben viajar en el versionado del
-- esquema ni acabar en produccion por descuido.
--
-- Es idempotente por marca: todo lo que crea lleva 'demo' en s_creado_por, de
-- modo que volver a ejecutarlo borra lo anterior y regenera. Los registros
-- reales, que no llevan esa marca, quedan intactos.
-- =========================================================================

BEGIN;

-- ------------------------------------------------- 0. LIMPIEZA DE LA TANDA
DELETE FROM public.bitacora
WHERE fn_ticket_id IN (SELECT pn_id FROM public.ticket WHERE s_creado_por = 'demo');
DELETE FROM public.equipo_reparacion WHERE s_creado_por = 'demo';
DELETE FROM public.resguardo        WHERE s_creado_por = 'demo';
DELETE FROM public.ticket           WHERE s_creado_por = 'demo';
DELETE FROM public.usuario          WHERE s_creado_por = 'demo';
DELETE FROM public.area             WHERE s_creado_por = 'demo';

-- --------------------------------------------------------------- 1. AREAS
-- Varias areas para que la grafica por departamento tenga algo que comparar.

INSERT INTO public.area (s_nombre, b_activo, b_prioritaria, s_creado_por, d_fecha_creacion)
VALUES
    ('Direccion General',        true, true,  'demo', '2026-05-01'),
    ('Recursos Humanos',         true, false, 'demo', '2026-05-01'),
    ('Finanzas',                 true, true,  'demo', '2026-05-01'),
    ('Juridico',                 true, false, 'demo', '2026-05-01'),
    ('Comunicacion Social',      true, false, 'demo', '2026-05-01'),
    ('Alimentaria',              true, false, 'demo', '2026-05-01'),
    ('Delegacion Norte',         true, false, 'demo', '2026-05-01');

-- ------------------------------------------------------------ 2. PERSONAL
-- La contrasena es la misma para todos: es un entorno de pruebas y estas
-- cuentas no deben existir fuera de el. Hash BCrypt de 'Sedif2026#'.

-- Tecnicos de soporte: cinco, para que la grafica de calificacion compare.
INSERT INTO public.usuario (
    s_nombre, s_apellido_paterno, s_apellido_materno, s_correo, s_username,
    s_password, b_activo, b_disponible_soporte, b_password_temporal,
    fn_rol_id, fn_area_id, s_creado_por, d_fecha_creacion)
SELECT v.nombre, v.paterno, v.materno, v.correo, v.usuario,
       '$2a$10$QDLjYG/9JTFWw5oEVg.ZVeBVEL7ERnJyC/B77yNt45xmVua2ggnkW',
       true, true, false,
       (SELECT pn_id FROM public.rol WHERE s_nombre = 'SOPORTE'),
       (SELECT pn_id FROM public.area WHERE s_nombre = 'Tecnologias de la Informacion'),
       'demo', '2026-05-01'
FROM (VALUES
    ('MIGUEL',  'HERNANDEZ', 'CRUZ',    'miguel.hernandez@sedif.local',  'mhernandez'),
    ('ADRIANA', 'VAZQUEZ',   'ROJAS',   'adriana.vazquez@sedif.local',   'avazquez'),
    ('RICARDO', 'FLORES',    'MEDINA',  'ricardo.flores@sedif.local',    'rflores'),
    ('PAOLA',   'JIMENEZ',   'NAVA',    'paola.jimenez@sedif.local',     'pjimenez')
) AS v(nombre, paterno, materno, correo, usuario);

-- Empleados que levantan los tickets, repartidos entre las areas.
INSERT INTO public.usuario (
    s_nombre, s_apellido_paterno, s_apellido_materno, s_correo, s_username,
    s_password, b_activo, b_disponible_soporte, b_password_temporal,
    fn_rol_id, fn_area_id, s_creado_por, d_fecha_creacion)
SELECT v.nombre, v.paterno, v.materno, v.correo, v.usuario,
       '$2a$10$QDLjYG/9JTFWw5oEVg.ZVeBVEL7ERnJyC/B77yNt45xmVua2ggnkW',
       true, false, false,
       (SELECT pn_id FROM public.rol WHERE s_nombre = 'EMPLEADO'),
       (SELECT pn_id FROM public.area WHERE s_nombre = v.area),
       'demo', '2026-05-01'
FROM (VALUES
    ('MARTHA',   'GONZALEZ', 'PEREZ',   'martha.gonzalez@sedif.local',  'mgonzalez', 'Direccion General'),
    ('JAVIER',   'RAMOS',    'SILVA',   'javier.ramos@sedif.local',     'jramos',    'Recursos Humanos'),
    ('LUCIA',    'CASTILLO', 'ORTEGA',  'lucia.castillo@sedif.local',   'lcastillo', 'Finanzas'),
    ('FERNANDO', 'MORALES',  'REYES',   'fernando.morales@sedif.local', 'fmorales',  'Juridico'),
    ('GABRIELA', 'SANTOS',   'LUNA',    'gabriela.santos@sedif.local',  'gsantos',   'Comunicacion Social'),
    ('ARTURO',   'DELGADO',  'PINEDA',  'arturo.delgado@sedif.local',   'adelgado',  'Alimentaria'),
    ('VERONICA', 'ESTRADA',  'CAMPOS',  'veronica.estrada@sedif.local', 'vestrada',  'Delegacion Norte'),
    ('HECTOR',   'AGUILAR',  'ROMERO',  'hector.aguilar@sedif.local',   'haguilar',  'Finanzas')
) AS v(nombre, paterno, materno, correo, usuario, area);

-- ------------------------------------------------------------- 3. TICKETS
-- Un numero variable de tickets por dia: la mayoria de los dias rondan los
-- diez, con jornadas flojas de tres y picos de veinte. Se usa el propio dia
-- como semilla para que el reparto sea variable pero reproducible.
--
-- Los fines de semana bajan a una fraccion: el area no opera igual en sabado.

INSERT INTO public.ticket (
    s_titulo, s_descripcion, s_sede, s_solicitante_nombre, s_prioridad,
    s_estado, s_calificacion, s_comentario_encuesta, d_fecha_encuesta,
    plan_trabajo_clave, d_fecha_fin, fn_usuario_area_id, fn_usuario_soporte_id,
    s_creado_por, d_fecha_creacion, s_justificacion)
SELECT
    f.titulo,
    f.descripcion,
    f.sede,
    emp.s_nombre || ' ' || COALESCE(emp.s_apellido_paterno, ''),
    f.prioridad,
    f.estado,
    f.calificacion,
    CASE f.calificacion
        WHEN 'BUENO'   THEN 'Atencion rapida y resolvio el problema.'
        WHEN 'REGULAR' THEN 'Se resolvio, aunque tardo mas de lo esperado.'
        WHEN 'MALO'    THEN 'El problema volvio a presentarse al poco tiempo.'
    END,
    CASE WHEN f.calificacion IS NOT NULL THEN f.momento + INTERVAL '1 day' END,
    CASE WHEN f.estado = 'CERRADO' THEN f.meta END,
    CASE WHEN f.estado = 'CERRADO' THEN f.momento + (f.horas_cierre || ' hours')::INTERVAL END,
    emp.pn_id,
    tec.pn_id,
    'demo',
    f.momento,
    CASE WHEN f.estado = 'CERRADO' THEN f.solucion END
FROM (
    SELECT
        dia + (8 + (n % 9)) * INTERVAL '1 hour' + ((n * 7) % 60) * INTERVAL '1 minute' AS momento,
        n,
        -- Catalogo de fallas frecuentes del area.
        (ARRAY[
            'NO ENCIENDE EL EQUIPO',
            'LA IMPRESORA NO IMPRIME',
            'SIN ACCESO A INTERNET',
            'CORREO INSTITUCIONAL NO ABRE',
            'PANTALLA AZUL AL INICIAR',
            'EQUIPO MUY LENTO',
            'NO RECONOCE LA MEMORIA USB',
            'SOLICITUD DE INSTALACION DE SOFTWARE',
            'TELEFONO IP SIN TONO',
            'RESPALDO DE INFORMACION',
            'CAMBIO DE TONER',
            'CONFIGURACION DE IMPRESORA EN RED',
            'NO ENTRA AL SISTEMA INSTITUCIONAL',
            'TECLADO NO RESPONDE',
            'FALLA EN VIDEOCONFERENCIA'
        ])[1 + (n * 3 + EXTRACT(DAY FROM dia)::INT) % 15] AS titulo,
        (ARRAY[
            'EL USUARIO REPORTA QUE EL EQUIPO NO RESPONDE AL PRESIONAR EL BOTON DE ENCENDIDO.',
            'AL ENVIAR UN DOCUMENTO A LA IMPRESORA NO OCURRE NADA Y LA LUZ AMBAR PARPADEA.',
            'EL EQUIPO NO NAVEGA AUNQUE EL CABLE DE RED ESTA CONECTADO Y OTROS SI TIENEN ENLACE.',
            'AL ABRIR EL CORREO APARECE UN MENSAJE DE ERROR Y SE CIERRA LA SESION.',
            'EL EQUIPO SE REINICIA SOLO Y MUESTRA UNA PANTALLA AZUL ANTES DE APAGARSE.',
            'EL EQUIPO TARDA VARIOS MINUTOS EN ABRIR CUALQUIER PROGRAMA.',
            'AL CONECTAR LA MEMORIA NO APARECE NINGUNA UNIDAD NUEVA EN EL EXPLORADOR.',
            'SE REQUIERE LA INSTALACION DEL PAQUETE DE OFICINA PARA EL EQUIPO ASIGNADO.',
            'EL APARATO NO DA TONO Y LA PANTALLA MUESTRA SIN REGISTRO.',
            'SE SOLICITA RESPALDO DE LA CARPETA DE TRABAJO ANTES DEL CAMBIO DE EQUIPO.'
        ])[1 + (n * 5) % 10] AS descripcion,
        (ARRAY['OFICINAS CENTRALES','OFICINAS METROPOLITANAS','DELEGACION NORTE','CASA CARMEN SERDAN'])
            [1 + (n + EXTRACT(DAY FROM dia)::INT) % 4] AS sede,
        -- La mayoria son NORMAL; lo urgente es minoria, como en la realidad.
        (ARRAY['BAJA','NORMAL','NORMAL','NORMAL','NORMAL','ALTA','ALTA','URGENTE'])
            [1 + (n * 11) % 8] AS prioridad,
        -- Los tickets viejos estan cerrados; los de los ultimos dias siguen
        -- abiertos o en proceso, que es como se ve un sistema en uso.
        CASE
            WHEN dia < DATE '2026-07-28' THEN 'CERRADO'
            WHEN (n % 3) = 0 THEN 'ABIERTO'
            WHEN (n % 3) = 1 THEN 'EN_PROCESO'
            ELSE 'CERRADO'
        END AS estado,
        -- Solo una parte de los cerrados recibe encuesta: la mayoria de la
        -- gente no contesta, y fingir lo contrario falsearia la metrica.
        CASE
            WHEN dia >= DATE '2026-07-28' THEN NULL
            WHEN (n * 13) % 10 < 4 THEN NULL
            WHEN (n * 17) % 20 < 2 THEN 'MALO'
            WHEN (n * 17) % 20 < 7 THEN 'REGULAR'
            ELSE 'BUENO'
        END AS calificacion,
        (ARRAY[1,2,5,6,7,7,7,8,11,12])[1 + (n * 3) % 10] AS meta,
        2 + (n * 7) % 46 AS horas_cierre,
        (ARRAY[
            'SE REEMPLAZO LA FUENTE DE PODER Y EL EQUIPO OPERA CON NORMALIDAD.',
            'SE CAMBIO EL CARTUCHO DE TONER Y SE CALIBRO LA IMPRESORA.',
            'SE RECONFIGURO EL PUERTO DE RED Y SE RESTABLECIO EL ENLACE.',
            'SE RESTABLECIO LA CONTRASENA Y SE VERIFICO EL ACCESO AL BUZON.',
            'SE ACTUALIZARON LOS CONTROLADORES Y SE APLICARON LAS ACTUALIZACIONES PENDIENTES.',
            'SE LIBERO ESPACIO EN DISCO Y SE DESACTIVARON PROGRAMAS DE INICIO.',
            'SE INSTALO EL SOFTWARE SOLICITADO Y SE VERIFICO SU FUNCIONAMIENTO.',
            'SE REEMPLAZO EL CABLE DE RED DEL APARATO Y SE REGISTRO LA EXTENSION.'
        ])[1 + (n * 3) % 8] AS solucion
    FROM generate_series(DATE '2026-05-01', DATE '2026-08-04', INTERVAL '1 day') AS dia
    CROSS JOIN LATERAL generate_series(1,
        CASE
            -- Sabado y domingo: guardia minima.
            WHEN EXTRACT(DOW FROM dia) IN (0, 6) THEN 1 + (EXTRACT(DAY FROM dia)::INT % 3)
            -- Entre semana: de 3 a 20, con la mayoria de los dias cerca de 10.
            ELSE 3 + ((EXTRACT(DAY FROM dia)::INT * 7 + EXTRACT(MONTH FROM dia)::INT * 3) % 18)
        END) AS n
) AS f
-- Se reparte entre los empleados y los tecnicos dando la vuelta a las listas.
CROSS JOIN LATERAL (
    SELECT pn_id, s_nombre, s_apellido_paterno FROM public.usuario
    WHERE s_creado_por = 'demo' AND fn_rol_id = (SELECT pn_id FROM public.rol WHERE s_nombre = 'EMPLEADO')
    ORDER BY pn_id OFFSET (f.n + EXTRACT(DAY FROM f.momento)::INT) % 8 LIMIT 1
) AS emp
CROSS JOIN LATERAL (
    SELECT pn_id FROM public.usuario
    WHERE fn_rol_id = (SELECT pn_id FROM public.rol WHERE s_nombre = 'SOPORTE') AND b_activo
    ORDER BY pn_id OFFSET (f.n * 3 + EXTRACT(DAY FROM f.momento)::INT) % 5 LIMIT 1
) AS tec;

-- Bitacora de los tickets cerrados, para que el historial no quede vacio.
INSERT INTO public.bitacora (s_estatus_registrado, s_justificacion, fn_ticket_id,
                             s_creado_por, d_fecha_creacion)
SELECT 'CERRADO', t.s_justificacion, t.pn_id, 'demo', t.d_fecha_fin
FROM public.ticket t
WHERE t.s_creado_por = 'demo' AND t.s_estado = 'CERRADO';

-- --------------------------------------------------- 4. TALLER (DICTAMENES)
-- Alrededor de un ingreso al taller cada dos dias.

INSERT INTO public.equipo_reparacion (
    s_folio, s_equipo_tipo, s_marca, s_modelo, s_numero_serie, s_numero_inventario,
    s_accesorios, s_condicion_recepcion, s_falla_reportada, s_diagnostico, s_solucion,
    s_estado_taller, s_solicitante_nombre, s_solicitante_numero, s_departamento,
    fn_tecnico_id, s_creado_por, d_fecha_creacion)
SELECT
    'REP-2026-' || (1000 + ROW_NUMBER() OVER (ORDER BY f.momento)),
    f.tipo, f.marca, f.modelo,
    'SN-' || LPAD(((f.n * 137) % 90000 + 10000)::TEXT, 5, '0'),
    'INV-' || LPAD(((f.n * 53) % 9000 + 1000)::TEXT, 4, '0'),
    (ARRAY['CARGADOR','CARGADOR Y MALETIN','CABLE DE CORRIENTE','SIN ACCESORIOS','MOUSE Y TECLADO'])
        [1 + f.n % 5],
    (ARRAY['BUEN ESTADO FISICO','RAYONES EN LA CARCASA','CARCASA CON GOLPE EN LA ESQUINA','SIN DANOS VISIBLES'])
        [1 + f.n % 4],
    f.falla,
    CASE WHEN f.estado <> 'RECIBIDO' THEN f.diagnostico END,
    CASE WHEN f.estado IN ('REPARADO','ENTREGADO') THEN f.solucion END,
    f.estado,
    emp.s_nombre || ' ' || COALESCE(emp.s_apellido_paterno, ''),
    'E-' || LPAD(((f.n * 31) % 9000 + 1000)::TEXT, 4, '0'),
    (SELECT s_nombre FROM public.area WHERE pn_id = emp.fn_area_id),
    tec.pn_id, 'demo', f.momento
FROM (
    SELECT
        dia + (9 + (EXTRACT(DAY FROM dia)::INT % 7)) * INTERVAL '1 hour' AS momento,
        EXTRACT(DOY FROM dia)::INT AS n,
        (ARRAY['LAPTOP','PC DE ESCRITORIO','IMPRESORA','MONITOR','NO BREAK','ESCANER'])
            [1 + EXTRACT(DAY FROM dia)::INT % 6] AS tipo,
        (ARRAY['DELL','HP','LENOVO','ACER','EPSON','APC'])
            [1 + EXTRACT(DAY FROM dia)::INT % 6] AS marca,
        (ARRAY['LATITUDE 5420','PROBOOK 450','THINKPAD E14','VERITON X','L3250','BACK-UPS 700'])
            [1 + EXTRACT(DAY FROM dia)::INT % 6] AS modelo,
        (ARRAY[
            'EL EQUIPO NO ENCIENDE NI DA SENAL DE VIDEO.',
            'SE APAGA SOLO DESPUES DE UNOS MINUTOS DE USO.',
            'NO CARGA LA BATERIA AUNQUE ESTE CONECTADO.',
            'LA PANTALLA PARPADEA Y SE VE CON LINEAS.',
            'HACE UN RUIDO CONSTANTE Y SE CALIENTA MUCHO.',
            'ATASCA EL PAPEL EN CADA IMPRESION.'
        ])[1 + EXTRACT(DAY FROM dia)::INT % 6] AS falla,
        (ARRAY[
            'FUENTE DE PODER DANADA POR VARIACION DE VOLTAJE.',
            'DISIPADOR SATURADO DE POLVO Y PASTA TERMICA SECA.',
            'BATERIA AGOTADA POR FIN DE VIDA UTIL.',
            'CABLE FLEX DE LA PANTALLA CON FALSO CONTACTO.',
            'RODILLO DE ARRASTRE DESGASTADO.'
        ])[1 + EXTRACT(DAY FROM dia)::INT % 5] AS diagnostico,
        (ARRAY[
            'SE REEMPLAZO LA FUENTE DE PODER Y SE PROBO POR DOS HORAS.',
            'SE REALIZO LIMPIEZA PROFUNDA Y CAMBIO DE PASTA TERMICA.',
            'SE SUSTITUYO LA BATERIA Y SE CALIBRO LA CARGA.',
            'SE REEMPLAZO EL CABLE FLEX Y SE VERIFICO LA IMAGEN.',
            'SE CAMBIO EL RODILLO Y SE LIMPIO EL RECORRIDO DEL PAPEL.'
        ])[1 + EXTRACT(DAY FROM dia)::INT % 5] AS solucion,
        CASE
            WHEN dia < DATE '2026-07-20' THEN 'ENTREGADO'
            WHEN dia < DATE '2026-07-30' THEN (ARRAY['REPARADO','ENTREGADO','IRREPARABLE'])[1 + EXTRACT(DAY FROM dia)::INT % 3]
            ELSE (ARRAY['RECIBIDO','EN_DIAGNOSTICO','EN_REPARACION'])[1 + EXTRACT(DAY FROM dia)::INT % 3]
        END AS estado
    FROM generate_series(DATE '2026-05-01', DATE '2026-08-04', INTERVAL '2 days') AS dia
) AS f
CROSS JOIN LATERAL (
    SELECT pn_id, s_nombre, s_apellido_paterno, fn_area_id FROM public.usuario
    WHERE s_creado_por = 'demo' AND fn_rol_id = (SELECT pn_id FROM public.rol WHERE s_nombre = 'EMPLEADO')
    ORDER BY pn_id OFFSET f.n % 8 LIMIT 1
) AS emp
CROSS JOIN LATERAL (
    SELECT pn_id FROM public.usuario
    WHERE fn_rol_id = (SELECT pn_id FROM public.rol WHERE s_nombre = 'SOPORTE') AND b_activo
    ORDER BY pn_id OFFSET f.n % 5 LIMIT 1
) AS tec;

-- ----------------------------------------------------------- 5. RESGUARDOS
-- Un prestamo cada tres dias, con vencimientos escalonados: algunos ya
-- vencidos, otros por vencer y varios devueltos.

INSERT INTO public.resguardo (
    s_equipo_nombre, s_numero_serie, s_numero_inventario, s_accesorios, s_condiciones,
    s_solicitante_nombre, s_solicitante_numero, s_departamento, s_telefono,
    s_estado_resguardo, d_fecha_vencimiento, fn_usuario_creador_id,
    s_creado_por, d_fecha_creacion)
SELECT
    f.equipo,
    'SR-' || LPAD(((f.n * 211) % 90000 + 10000)::TEXT, 5, '0'),
    'INV-' || LPAD(((f.n * 79) % 9000 + 1000)::TEXT, 4, '0'),
    (ARRAY['CARGADOR Y MALETIN','CARGADOR','MOUSE INALAMBRICO','SIN ACCESORIOS'])[1 + f.n % 4],
    (ARRAY['BUENO','BUENO','BUENO','CON DETALLES ESTETICOS'])[1 + f.n % 4],
    emp.s_nombre || ' ' || COALESCE(emp.s_apellido_paterno, ''),
    'E-' || LPAD(((f.n * 31) % 9000 + 1000)::TEXT, 4, '0'),
    (SELECT s_nombre FROM public.area WHERE pn_id = emp.fn_area_id),
    'EXT. ' || (1000 + (f.n * 17) % 900),
    f.estado,
    f.vence,
    tec.pn_id, 'demo', f.momento
FROM (
    SELECT
        dia + INTERVAL '11 hours' AS momento,
        EXTRACT(DOY FROM dia)::INT AS n,
        (ARRAY[
            'LAPTOP DELL LATITUDE 5420','PROYECTOR EPSON POWERLITE','TABLETA SAMSUNG GALAXY TAB',
            'CAMARA WEB LOGITECH C920','NO BREAK APC 700VA','LAPTOP HP PROBOOK 450',
            'ANTENA DE RED TP-LINK','DISCO DURO EXTERNO 1TB'
        ])[1 + EXTRACT(DAY FROM dia)::INT % 8] AS equipo,
        dia + ((30 + (EXTRACT(DAY FROM dia)::INT % 90)) || ' days')::INTERVAL AS vence,
        CASE
            -- Devueltos: los prestamos mas antiguos ya se cerraron.
            WHEN dia < DATE '2026-06-15' AND EXTRACT(DAY FROM dia)::INT % 3 = 0 THEN 'DEVUELTO'
            -- Vencidos: entregados cuya fecha ya paso y siguen fuera.
            WHEN dia + ((30 + (EXTRACT(DAY FROM dia)::INT % 90)) || ' days')::INTERVAL < DATE '2026-08-04'
                THEN 'VENCIDO'
            ELSE 'ENTREGADO'
        END AS estado
    FROM generate_series(DATE '2026-05-01', DATE '2026-08-04', INTERVAL '3 days') AS dia
) AS f
CROSS JOIN LATERAL (
    SELECT pn_id, s_nombre, s_apellido_paterno, fn_area_id FROM public.usuario
    WHERE s_creado_por = 'demo' AND fn_rol_id = (SELECT pn_id FROM public.rol WHERE s_nombre = 'EMPLEADO')
    ORDER BY pn_id OFFSET f.n % 8 LIMIT 1
) AS emp
CROSS JOIN LATERAL (
    SELECT pn_id FROM public.usuario
    WHERE fn_rol_id = (SELECT pn_id FROM public.rol WHERE s_nombre = 'SOPORTE') AND b_activo
    ORDER BY pn_id OFFSET f.n % 5 LIMIT 1
) AS tec;

COMMIT;
