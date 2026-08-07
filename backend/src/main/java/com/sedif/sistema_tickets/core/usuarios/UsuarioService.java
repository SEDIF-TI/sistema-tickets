package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.core.area.AreaRepository;
import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.exception.PageResponse;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
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
 * Gestion de usuarios: altas, edicion, disponibilidad y contrasenas.
 *
 * <p>El servidor es el unico que fabrica contrasenas. En el alta y en el
 * restablecimiento genera una temporal, la guarda cifrada y la devuelve una
 * sola vez para que el administrador pueda comunicarla; la marca
 * {@code passwordTemporal} obliga a cambiarla en el primer acceso.</p>
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
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Da de alta un usuario con una contrasena temporal generada en el
     * servidor.
     *
     * <p>La respuesta la incluye en claro por unica vez, ya que solo se
     * persiste su hash y no habra forma de recuperarla despues.</p>
     */
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con el correo " + request.correo() + ".");
        }

        // El username es opcional, pero si viene debe ser unico. Adelantar la
        // comprobacion de la restriccion UNIQUE permite decir que corregir, en
        // lugar del 500 generico que produce el error de la base.
        String username = request.username();
        if (username != null && !username.isBlank()
                && usuarioRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException(
                    "El nombre de usuario '" + username.trim() + "' ya esta en uso.");
        }

        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));

        String passwordTemporal = generarPasswordAleatoria();

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setApellidoPaterno(request.apellidoPaterno());
        nuevoUsuario.setApellidoMaterno(request.apellidoMaterno());
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setUsername(username != null && !username.isBlank() ? username.trim() : null);
        
        // Solo se persiste el hash; la bandera fuerza el cambio al entrar.
        nuevoUsuario.setPassword(passwordEncoder.encode(passwordTemporal));
        nuevoUsuario.setPasswordTemporal(true); 

        nuevoUsuario.setRol(rol);
        nuevoUsuario.setActivo(true);

        // La columna es NOT NULL, asi que un campo omitido se resuelve a false.
        Boolean disponible = request.disponibleSoporte();
        nuevoUsuario.setDisponibleSoporte(disponible != null ? disponible : false);

        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
            nuevoUsuario.setArea(area);
        }

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        return UsuarioResponse.desdeEntidadConPassword(usuarioGuardado, passwordTemporal);
    }

    /**
     * Genera una contrasena temporal criptograficamente segura.
     *
     * <p>Toma los caracteres de {@link SecureRandom} sobre un alfabeto amplio
     * que combina ambas cajas, digitos y simbolos. Quedan fuera los caracteres
     * ambiguos (I, l, 1, O, 0) porque estas claves se dictan o se copian a
     * mano.</p>
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
     * Catalogo completo de usuarios, sin paginar.
     *
     * <p>Lo consumen los selectores que necesitan todas las opciones de golpe.
     * El listado del panel usa {@link #listarUsuariosPaginado}.</p>
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
     * Listado paginado del panel de administracion, con la busqueda y los
     * filtros de rol y estado resueltos en la base de datos.
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
     * <p>El {@code Pageable} se arma con lo que llega en la URL. La lista
     * blanca impide ordenar por {@code password}, ya que el orden del
     * resultado dejaria deducir informacion sobre los hashes.</p>
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
     * Alterna si un tecnico entra en el reparto automatico de tickets.
     *
     * <p>Solo tiene sentido sobre el rol SOPORTE: es el unico que participa en
     * el balanceo de carga.</p>
     */
    @Transactional
    public UsuarioResponse actualizarDisponibilidad(Long id, DisponibilidadRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        if (!RolUsuario.SOPORTE.es(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("Solo los usuarios con rol de SOPORTE pueden modificar su disponibilidad.");
        }

        usuario.setDisponibleSoporte(request.disponible());
        Usuario usuarioActualizado = usuarioRepository.save(usuario);

        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    /**
     * Baja logica de un usuario.
     *
     * <p>Conserva la fila y su historial; el usuario deja de poder entrar. Si
     * era tecnico, se le retira ademas la disponibilidad para que el
     * balanceador no siga considerandolo.</p>
     */
    @Transactional
    public UsuarioResponse inactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        if (!usuario.getActivo()) {
            throw new IllegalArgumentException("El usuario ya se encuentra inactivo.");
        }

        usuario.setActivo(false);

        if (RolUsuario.SOPORTE.es(usuario.getRol().getNombre())) {
            usuario.setDisponibleSoporte(false);
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    /**
     * Actualiza los datos de un usuario desde el panel de administracion.
     *
     * <p>Rol y area solo se tocan si vienen informados, de modo que una
     * edicion parcial no desvincule al usuario de ninguno de los dos.</p>
     */
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        // El correo identifica al usuario en el acceso: si chocara con el de
        // otra persona, ambos quedarian sin poder entrar.
        if (usuarioRepository.existsByCorreoAndIdNot(request.correo(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe otro usuario con el correo " + request.correo() + ".");
        }

        usuario.setNombre(request.nombre());
        usuario.setApellidoPaterno(request.apellidoPaterno());
        usuario.setApellidoMaterno(request.apellidoMaterno());
        usuario.setCorreo(request.correo());

        if (request.rolId() != null) {
            Rol rol = rolRepository.findById(request.rolId())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
            usuario.setRol(rol);
        }

        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
            usuario.setArea(area);
        }

        return UsuarioResponse.desdeEntidad(usuarioRepository.save(usuario));
    }

    /** Catalogo completo de usuarios mapeado a DTO. */
    public List<UsuarioResponse> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }

    /**
     * Restablece la contrasena de un usuario que perdio la suya.
     *
     * <p>Genera una nueva clave temporal y vuelve a marcar la cuenta como tal,
     * de modo que se exija cambiarla en el siguiente acceso. La devuelve en
     * claro por unica vez para que el administrador pueda comunicarla.</p>
     */
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

    /** Invierte el alta o baja del usuario desde el conmutador del panel. */
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
     * <p>Exige la contrasena actual antes de aceptar la nueva: quien se hiciera
     * con un token valido no puede apoderarse de la cuenta de forma permanente
     * sin conocer la clave original.</p>
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

        // Repetir la clave dejaria la cuenta con la contrasena temporal, que ya
        // circulo por otros canales al comunicarsela al usuario.
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
        return usuarioRepository.findByRolNombre(RolUsuario.SOPORTE.nombreEnBd())
                .stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }
}