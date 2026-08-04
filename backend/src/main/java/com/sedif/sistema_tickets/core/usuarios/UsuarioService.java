package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.core.area.AreaRepository;
import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.exception.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * Servicio que contiene la lógica de negocio para la gestión de usuarios.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UsuarioService {

    /**
     * Generador criptografico para las contrasenas temporales. Es thread-safe,
     * por lo que puede compartirse como constante de clase.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final RolRepository rolRepository; // Inyectamos el nuevo repositorio de Rol
    private final PasswordEncoder passwordEncoder; // Inyectamos el PasswordEncoder para encriptar contraseñas
    /**
     * Crea un nuevo usuario en el sistema aplicando reglas de validación.
     */
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con el correo " + request.correo() + ".");
        }

        // El username es opcional, pero si viene debe ser unico. Sin esta
        // comprobacion el choque lo detectaba la restriccion UNIQUE de la base
        // y la respuesta era un 500 generico que no decia que corregir.
        String username = request.username();
        if (username != null && !username.isBlank()
                && usuarioRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException(
                    "El nombre de usuario '" + username.trim() + "' ya esta en uso.");
        }

        // 1. Buscamos el Rol
        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));

        // 2. Generar contraseña temporal segura
        String passwordTemporal = generarPasswordAleatoria();
        
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.nombre());
        // Los apellidos se guardaban vacios: el formulario los capturaba pero
        // el DTO no los aceptaba, asi que se descartaban en silencio y los
        // documentos oficiales salian sin ellos.
        nuevoUsuario.setApellidoPaterno(request.apellidoPaterno());
        nuevoUsuario.setApellidoMaterno(request.apellidoMaterno());
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setUsername(username != null && !username.isBlank() ? username.trim() : null);
        
        // 3. Encriptar contraseña y activar bandera de cambio forzoso
        nuevoUsuario.setPassword(passwordEncoder.encode(passwordTemporal));
        nuevoUsuario.setPasswordTemporal(true); 
        
        nuevoUsuario.setRol(rol);
        nuevoUsuario.setActivo(true);

        // ¡AQUÍ ESTÁ LA SOLUCIÓN!
        // Si el request trae el campo nulo, le asignamos false automáticamente.
        Boolean disponible = request.disponibleSoporte();
        nuevoUsuario.setDisponibleSoporte(disponible != null ? disponible : false);

        // Si el usuario pertenece a una área, asignarla
        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
            nuevoUsuario.setArea(area);
        }

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        
        // 4. Retornamos la respuesta incluyendo la contraseña temporal para informarla al administrador
        return UsuarioResponse.desdeEntidadConPassword(usuarioGuardado, passwordTemporal);
    }

    /**
     * Genera una contrasena temporal criptograficamente segura.
     *
     * <p>La version anterior usaba {@code UUID.randomUUID().substring(0, 8)}:
     * ocho caracteres del primer bloque de un UUID, es decir solo digitos
     * hexadecimales (0-9, a-f). Eso son unos 32 bits de entropia, sin
     * mayusculas ni simbolos, y resulta atacable por fuerza bruta.</p>
     *
     * <p>Ahora se usa {@link SecureRandom} sobre un alfabeto amplio. Se
     * excluyen los caracteres ambiguos (I, l, 1, O, 0) porque estas claves se
     * dictan o se copian a mano.</p>
     */
    private String generarPasswordAleatoria() {
        final String alfabeto =
                "ABCDEFGHJKLMNPQRSTUVWXYZ" + "abcdefghijkmnopqrstuvwxyz" + "23456789" + "@#$%&*";
        final int longitud = 14;

        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(alfabeto.charAt(RANDOM.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }

    /**
     * Obtiene la lista de todos los usuarios registrados.
     *
     * <p>Se conserva sin paginar para los selectores que necesitan el catalogo
     * completo. El listado del panel usa {@link #listarUsuariosPaginado}.</p>
     */
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }

    /** Campos por los que se admite ordenar el listado de usuarios. */
    private static final Set<String> CAMPOS_ORDENABLES_USUARIO =
            Set.of("id", "nombre", "apellidoPaterno", "correo", "username", "activo");

    /**
     * Listado paginado del panel de administracion.
     *
     * <p>La busqueda y los filtros de rol y estado se resuelven en la base de
     * datos. Antes la pantalla descargaba la tabla completa y filtraba en el
     * navegador, de modo que buscar solo miraba lo ya cargado.</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listarUsuariosPaginado(
            String busqueda, String rol, Boolean activo, Pageable pageable) {

        Pageable paginaSegura = sanearOrdenUsuario(pageable);

        return PageResponse.de(
                usuarioRepository.buscarPaginado(
                        normalizarBusqueda(busqueda), normalizarTexto(rol), activo, paginaSegura),
                UsuarioResponse::desdeEntidad);
    }

    /** Deja en {@code null} los filtros vacios, que las consultas leen como "sin filtrar". */
    private String normalizarTexto(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /**
     * Prepara el texto para el LIKE: minusculas, comodines y escape de los
     * caracteres especiales, para que buscar "100%" busque ese literal.
     */
    private String normalizarBusqueda(String valor) {
        String texto = normalizarTexto(valor);
        if (texto == null) {
            return null;
        }
        String escapado = texto.toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }

    /**
     * Descarta los campos de ordenacion no permitidos.
     *
     * <p>El {@code Pageable} se arma con lo que llega en la URL: sin esta
     * lista se podria ordenar por {@code password} y deducir informacion a
     * partir del orden del resultado.</p>
     */
    private Pageable sanearOrdenUsuario(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> permitidos = pageable.getSort().stream()
                .filter(orden -> {
                    boolean valido = CAMPOS_ORDENABLES_USUARIO.contains(orden.getProperty());
                    if (!valido) {
                        log.warn("Orden por campo no permitido: '{}'. Se ignora.", orden.getProperty());
                    }
                    return valido;
                })
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                permitidos.isEmpty() ? Sort.by(Sort.Direction.ASC, "nombre") : Sort.by(permitidos));
    }

    /**
     * Cambia la disponibilidad de un usuario de soporte.
     */
    @Transactional
    public UsuarioResponse actualizarDisponibilidad(Long id, DisponibilidadRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        // Validamos comparando el nombre del rol en la entidad
        if (!"SOPORTE".equals(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("Solo los usuarios con rol de SOPORTE pueden modificar su disponibilidad.");
        }

        usuario.setDisponibleSoporte(request.disponible());
        Usuario usuarioActualizado = usuarioRepository.save(usuario);

        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    /**
     * Realiza la baja lógica de un usuario.
     */
    @Transactional
    public UsuarioResponse inactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        if (!usuario.getActivo()) {
            throw new IllegalArgumentException("El usuario ya se encuentra inactivo.");
        }

        usuario.setActivo(false);

        // Validamos comparando el nombre del rol
        if ("SOPORTE".equals(usuario.getRol().getNombre())) {
            usuario.setDisponibleSoporte(false);
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    // Asegúrate de que en UsuarioService.java tengas exactamente esto:
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        // El correo identifica al usuario en el login: no puede chocar con el
        // de otra persona. Antes solo se comprobaba al crear, asi que una
        // edicion podia duplicarlo y romper el acceso de ambos.
        if (usuarioRepository.existsByCorreoAndIdNot(request.correo(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe otro usuario con el correo " + request.correo() + ".");
        }

        // 2. Actualizar campos básicos
        usuario.setNombre(request.nombre());
        usuario.setApellidoPaterno(request.apellidoPaterno());
        usuario.setApellidoMaterno(request.apellidoMaterno());
        usuario.setCorreo(request.correo());

        // 3. Actualizar Rol (buscando la entidad)
        if (request.rolId() != null) {
            Rol rol = rolRepository.findById(request.rolId())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
            usuario.setRol(rol);
        }

        // 4. Actualizar Área
        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
            usuario.setArea(area);
        }

        return UsuarioResponse.desdeEntidad(usuarioRepository.save(usuario));
    }

    // Listar todos (necesitas mapear la lista de entidades a lista de responses)
    public List<UsuarioResponse> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }

    // Resetear contraseña
    @Transactional
    public UsuarioResponse resetearPassword(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        String nuevaTemporal = generarPasswordAleatoria();
        usuario.setPassword(passwordEncoder.encode(nuevaTemporal));
        usuario.setPasswordTemporal(true);
        
        Usuario guardado = usuarioRepository.save(usuario);
        return UsuarioResponse.desdeEntidadConPassword(guardado, nuevaTemporal);
    }

    // Alternar estado
    @Transactional
    public UsuarioResponse alternarEstadoUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        usuario.setActivo(!usuario.getActivo());
        return UsuarioResponse.desdeEntidad(usuarioRepository.save(usuario));
    }

    /**
     * Cambia la contrasena del usuario autenticado.
     *
     * <p>Exige la contrasena actual antes de aceptar la nueva. Sin esa
     * comprobacion, quien se hiciera con un token valido podia apoderarse de
     * la cuenta de forma permanente sin conocer la clave original.</p>
     *
     * <p>Se busca por correo o por username porque no todos los usuarios
     * tienen correo: el principal del token puede ser cualquiera de los dos.</p>
     *
     * @param identificador correo o username del usuario autenticado.
     * @param request       contrasena actual y nueva, ya validadas de formato.
     */
    @Transactional
    public void actualizarPassword(String identificador, CambioPasswordRequest request) {
        Usuario usuario = usuarioRepository
                .findByCorreoOrUsername(identificador, identificador)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        // Prueba de identidad: sin la contrasena actual no se permite el cambio.
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException(MessageConstants.PASSWORD_ACTUAL_INCORRECTA);
        }

        // Reutilizar la misma clave dejaria la cuenta con la contrasena
        // temporal que ya circulo por otros canales.
        if (passwordEncoder.matches(request.nuevaPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException(MessageConstants.PASSWORD_IGUAL_ANTERIOR);
        }

        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        // Con una clave elegida por el usuario, deja de ser temporal.
        usuario.setPasswordTemporal(false);

        usuarioRepository.save(usuario);
    }

    /**
     * Personal de soporte, para los selectores de asignacion manual de tickets
     * y de soporte fijo de un area.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarSoporte() {
        return usuarioRepository.findByRolNombre("SOPORTE")
                .stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }
}