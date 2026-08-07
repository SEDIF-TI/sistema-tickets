package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.core.ticket.bitacora.Bitacora;
import com.sedif.sistema_tickets.core.ticket.bitacora.BitacoraRepository;
import com.sedif.sistema_tickets.core.ticket.filtros.TicketFiltroStrategy;
import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.exception.PageResponse;
import com.sedif.sistema_tickets.util.enums.Calificacion;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import com.sedif.sistema_tickets.util.enums.PlanTrabajo;
import com.sedif.sistema_tickets.util.enums.NivelVision;
import com.sedif.sistema_tickets.util.enums.Prioridad;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclo de vida del ticket: alta, asignacion, atencion, resolucion, cierre y
 * encuesta de satisfaccion.
 *
 * <p>La visibilidad del listado no se resuelve aqui sino en las estrategias de
 * {@code core.ticket.filtros}, inyectadas en {@code estrategiasFiltro} y
 * seleccionadas por el nivel de vision del rol: GLOBAL, AREA o PERSONAL.</p>
 *
 * <p>Cada cambio de estado deja constancia en la bitacora y se difunde por
 * WebSocket, de modo que las pantallas abiertas se actualizan sin recargar.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final SedifTelegramBot telegramBot;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;

    /** Estrategias de visibilidad indexadas por nivel de vision del rol. */
    private final Map<String, TicketFiltroStrategy> estrategiasFiltro;

    private final BitacoraRepository bitacoraRepository;

    /** Canal STOMP hacia los clientes suscritos. */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Da de alta un ticket y le asigna tecnico.
     *
     * <p>La asignacion sigue tres vias excluyentes, en este orden:</p>
     * <ol>
     *   <li><b>Directa</b>: el alta trae {@code usuarioSoporteId}, que es lo que
     *       envia el administrador cuando elige tecnico a mano.</li>
     *   <li><b>Auto-asignacion</b>: quien crea el ticket tiene rol SOPORTE, de
     *       modo que se queda con el suyo.</li>
     *   <li><b>Reparto automatico</b>: el resto de casos pasan por
     *       {@link #resolverAsignacion(Usuario)}.</li>
     * </ol>
     *
     * <p>Si ninguna via da resultado el ticket se guarda sin tecnico: queda
     * visible en la bandeja para que soporte lo tome, en lugar de rechazar el
     * alta y perder el reporte.</p>
     */
    @Transactional
    public TicketResponse crearTicket(TicketRequestRecord request, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(request.titulo());
        nuevoTicket.setDescripcion(request.descripcion());
        nuevoTicket.setSede(request.sede());

        // Persona afectada por la falla, que no siempre coincide con quien
        // levanta el ticket: soporte puede reportar en nombre de un tercero.
        if (request.solicitante() != null && !request.solicitante().isBlank()) {
            nuevoTicket.setSolicitanteNombre(request.solicitante());
        }

        nuevoTicket.setUsuarioArea(usuario);
        nuevoTicket.setEstado(EstadoTicket.ABIERTO);
        nuevoTicket.setFechaCreacion(LocalDateTime.now());
        nuevoTicket.setCreadoPor(usuario.getCorreo());

        // Una prioridad no reconocida cae en NORMAL en vez de rechazar el alta:
        // registrar el ticket importa mas que el matiz de prioridad, que puede
        // corregirse despues desde la bandeja.
        nuevoTicket.setPrioridad(
                Prioridad.desde(request.prioridad()).orElseGet(Prioridad::porDefecto));

        if (request.usuarioSoporteId() != null) {
            // Via 1: el administrador indico tecnico en el alta.
            Usuario soporteElegido = usuarioRepository.findById(request.usuarioSoporteId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario de soporte no encontrado"));
            nuevoTicket.setUsuarioSoporte(soporteElegido);
            log.debug("Ticket asignado manualmente al tecnico id={}", soporteElegido.getId());

        } else if (usuario.getRol() != null && RolUsuario.SOPORTE.es(usuario.getRol().getNombre())) {
            // Via 2: lo levanta un tecnico, que se queda con el ticket.
            nuevoTicket.setUsuarioSoporte(usuario);
            log.debug("Ticket auto-asignado al tecnico que lo creo.");

        } else {
            // Via 3: reparto automatico por soporte fijo del area o por carga.
            Usuario soporteAsignado = resolverAsignacion(usuario);
            if (soporteAsignado != null) {
                nuevoTicket.setUsuarioSoporte(soporteAsignado);
                log.debug("Ticket asignado por el balanceador al tecnico id={}", soporteAsignado.getId());
            } else {
                log.warn("Ticket creado sin tecnico asignado: no hay soporte disponible.");
            }
        }

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        // Aviso por Telegram al tecnico asignado. Requiere que haya vinculado su
        // cuenta desde el perfil: sin chat id no hay destinatario al que enviar.
        if (ticketGuardado.getUsuarioSoporte() != null &&
            ticketGuardado.getUsuarioSoporte().getTelegramChatId() != null) {

            // La sede identifica al afectado cuando el ticket se levanta en
            // nombre de otra persona; si viene vacia se usa quien lo creo.
            String afectado = ticketGuardado.getSede();
            if (afectado == null || afectado.isBlank()) {
                afectado = ticketGuardado.getUsuarioArea().getNombre();
            }

            // El bot envia con parse mode Markdown: los asteriscos aplican
            // negrita y el guion bajo cursiva.
            String mensaje = "🚨 *NUEVO TICKET ASIGNADO* 🚨\n\n" +
                             "🆔 *Folio:* #" + ticketGuardado.getId() + "\n" +
                             "👤 *Usuario afectado:* " + afectado + "\n" +
                             "📌 *Falla Principal:* " + ticketGuardado.getTitulo() + "\n\n" +
                             "📝 *Descripción detallada:*\n_" + ticketGuardado.getDescripcion() + "_";

            try {
                telegramBot.enviarMensaje(ticketGuardado.getUsuarioSoporte().getTelegramChatId(), mensaje);
                log.debug("Notificacion de Telegram enviada para el ticket id={}", ticketGuardado.getId());
            } catch (Exception e) {
                log.warn("No se pudo enviar la notificacion de Telegram del ticket id={}", ticketGuardado.getId(), e);
            }
        } else {
            log.debug("Telegram omitido: el tecnico asignado no tiene ChatID vinculado.");
        }

        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
    }

    /**
     * Elige tecnico para un ticket que no trae asignacion explicita.
     *
     * <p>Primero manda el soporte fijo del area, si esa area tiene uno y sigue
     * activo y disponible: es la persona que ya conoce el equipo y a la gente
     * de ese departamento.</p>
     *
     * <p>Cuando no hay soporte fijo aplicable, se reparte por carga de trabajo.
     * La consulta del repositorio devuelve a los tecnicos disponibles ya
     * ordenados por numero de tickets abiertos ascendente, de modo que basta
     * tomar el primero; resolverlo en la base evita recorrer usuarios y contar
     * tickets uno a uno en el camino critico del alta.</p>
     *
     * @return el tecnico elegido, o {@code null} si no hay ninguno disponible.
     */
    private Usuario resolverAsignacion(Usuario usuarioArea) {
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            if (Boolean.TRUE.equals(fijo.getActivo()) && Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        List<Usuario> disponibles = usuarioRepository
                .buscarTecnicosDisponiblesOrdenadosPorCarga(
                        RolUsuario.SOPORTE.nombreEnBd(), EstadoTicket.ABIERTO);

        return disponibles.isEmpty() ? null : disponibles.get(0);
    }

    public List<TicketResponse> obtenerTodosLosTickets() {
        return ticketRepository.findAll().stream()
                .map(this::mapearATicketResponse)
                .toList();
    }

    public List<TicketResponse> obtenerTicketsSegunRol(Long usuarioSolicitanteId) {
        Usuario usuario = usuarioRepository.findById(usuarioSolicitanteId)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        String nivel = usuario.getRol().getNivelVision(); 
        TicketFiltroStrategy estrategia = estrategiasFiltro.get(nivel);

        if (estrategia == null) {
            throw new IllegalStateException(
                    "El rol del usuario no tiene un nivel de visibilidad valido. Contacte al administrador.");
        }

        return estrategia.obtenerTickets(usuario).stream()
                .map(this::mapearATicketResponse)
                .toList();
    }

    private TicketResponse mapearATicketResponse(Ticket t) {
        String nombreSolicitante = "Desconocido";
        String nombreDepartamento = "Sin área";

        if (t.getUsuarioArea() != null) {
            nombreSolicitante = t.getUsuarioArea().getNombre();
            if (t.getUsuarioArea().getArea() != null) {
                nombreDepartamento = t.getUsuarioArea().getArea().getNombre();
            }
        }

        String justificacion = null;
        var historial = bitacoraRepository.findByTicketIdOrderByFechaCreacionDesc(t.getId());
        if (!historial.isEmpty()) {
            justificacion = historial.get(0).getJustificacion();
        }
        return new TicketResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getSede(),
                t.getFechaCreacion(),
                t.getFechaFin(),
                nombreSolicitante,
                nombreDepartamento,
                // Codigo estable para la logica de la interfaz...
                t.getEstado() != null ? t.getEstado().name() : null,
                // ...y etiqueta legible para mostrarla en pantalla.
                t.getEstado() != null ? t.getEstado().getEtiqueta() : null,
                t.getPrioridad() != null ? t.getPrioridad().name() : null,
                t.getPrioridad() != null ? t.getPrioridad().getEtiqueta() : null,
                t.getUsuarioArea()!= null ? t.getUsuarioArea().getId() : null,
                t.getUsuarioSoporte() != null ? t.getUsuarioSoporte().getId() : null,
                t.getUsuarioSoporte() != null ? t.getUsuarioSoporte().getNombre() : null,
                justificacion,
                t.getCalificacion() != null ? t.getCalificacion().name() : null,
                t.getCalificacion() != null ? t.getCalificacion().getEtiqueta() : null,
                t.getComentarioEncuesta()
        );
    }

    @Transactional
    public TicketResponse finalizarTicketPorEmpleado(Long ticketId, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        if (!ticket.getUsuarioArea().getArea().getId().equals(usuario.getArea().getId())) {
            throw new SecurityException("No tienes permiso: El ticket pertenece a otra área.");
        }
    
        ticket.setEstado(EstadoTicket.CERRADO);
        ticket.setFechaFin(LocalDateTime.now());
        Ticket ticketGuardado = ticketRepository.save(ticket);

        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
    }

    /**
     * Registra la encuesta de satisfaccion del solicitante.
     *
     * <p>Solo puede calificar quien levanto el ticket, y solo una vez: la
     * calificacion alimenta la metrica de cada tecnico, de modo que permitir
     * que la cambiara cualquiera —o varias veces— la volveria inservible.</p>
     *
     * <p>Se exige ademas que el ticket este cerrado: calificar un servicio que
     * todavia no termina no mide nada.</p>
     */
    @Transactional
    public TicketResponse calificarTicket(Long ticketId, String calificacion,
                                          String comentario, String correoUsuario) {

        Usuario usuario = usuarioRepository.findByCorreoOrUsername(correoUsuario, correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        if (ticket.getUsuarioArea() == null
                || !ticket.getUsuarioArea().getId().equals(usuario.getId())) {
            throw new SecurityException(
                    "Solo quien levanto el ticket puede calificar el servicio recibido.");
        }

        if (ticket.getEstado() == null || !ticket.getEstado().esFinal()) {
            throw new IllegalStateException(
                    "El ticket todavia no esta cerrado: aun no hay servicio que calificar.");
        }

        if (ticket.getCalificacion() != null) {
            throw new IllegalStateException("Este ticket ya fue calificado.");
        }

        Calificacion valor = Calificacion.desde(calificacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La calificacion debe ser MALO, REGULAR o BUENO."));

        ticket.setCalificacion(valor);
        ticket.setComentarioEncuesta(
                comentario != null && !comentario.isBlank() ? comentario.trim() : null);
        ticket.setFechaEncuesta(LocalDateTime.now());

        return mapearATicketResponse(ticketRepository.save(ticket));
    }

    /** Catalogo de calificaciones, para los botones de la encuesta. */
    public List<CatalogoResponse> obtenerCatalogoCalificaciones() {
        return Arrays.stream(Calificacion.values())
                .map(c -> new CatalogoResponse(c.name(), c.getEtiqueta()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> obtenerTicketsDeMiArea(String correoUsuario) {
        Usuario empleado = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (empleado.getArea() == null) throw new IllegalStateException("Usuario sin área.");

        return ticketRepository.findByUsuarioAreaAreaIdOrderByFechaCreacionDesc(empleado.getArea().getId())
                .stream()
                .map(this::mapearATicketResponse)
                .toList();
    }

    /**
     * Marca que el tecnico va en camino a atender el reporte y pasa el ticket a
     * EN_PROCESO.
     *
     * <p>Solo puede hacerlo el tecnico al que esta asignado, o un
     * ADMINISTRADOR: la pertenencia se comprueba con el correo de la sesion, no
     * con lo que llegue en la peticion. Sobre un ticket ya cerrado la operacion
     * se rechaza, porque su estado es final.</p>
     */
    @Transactional
    public TicketResponse atenderTicket(Long ticketId, String correoUsuario) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Usuario tecnico = obtenerUsuarioAutenticado(correoUsuario);
        verificarPuedeOperarElTicket(ticket, tecnico);

        if (ticket.getEstado() != null && ticket.getEstado().esFinal()) {
            throw new IllegalStateException(
                    "El ticket ya esta cerrado: no se puede volver a marcar en atencion.");
        }

        ticket.setEstado(EstadoTicket.EN_PROCESO);
        Ticket ticketGuardado = ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticketGuardado);
        bitacora.setEstatusRegistrado(EstadoTicket.EN_PROCESO.name());
        // Se deja constancia de quien avisa: la entrada generica anterior no
        // permitia saber que tecnico se habia puesto en camino.
        bitacora.setJustificacion(
                "El tecnico " + tecnico.getNombre() + " va en camino para atender el reporte.");
        bitacoraRepository.save(bitacora);

        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
    }

    /** Carga al usuario autenticado, que puede identificarse por correo o usuario. */
    private Usuario obtenerUsuarioAutenticado(String correoUsuario) {
        return usuarioRepository.findByCorreoOrUsername(correoUsuario, correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));
    }

    /**
     * Comprueba que el tecnico puede operar sobre el ticket.
     *
     * <p>El ADMINISTRADOR (vision GLOBAL) puede intervenir en cualquiera, para
     * poder desatascar un reporte cuando el tecnico asignado esta ausente. El
     * resto solo opera sobre los suyos.</p>
     */
    private void verificarPuedeOperarElTicket(Ticket ticket, Usuario usuario) {
        boolean esAdministrador = usuario.getRol() != null
                && NivelVision.desde(usuario.getRol().getNivelVision())
                        .filter(NivelVision.GLOBAL::equals)
                        .isPresent();

        if (esAdministrador) {
            return;
        }

        Long idAsignado = ticket.getUsuarioSoporte() != null
                ? ticket.getUsuarioSoporte().getId()
                : null;

        if (idAsignado == null || !idAsignado.equals(usuario.getId())) {
            log.warn("El usuario id={} intento operar el ticket id={}, asignado a id={}.",
                    usuario.getId(), ticket.getId(), idAsignado);
            throw new SecurityException(
                    "No puedes modificar este ticket: esta asignado a otro tecnico.");
        }
    }

    /**
     * Cierra el ticket dejando constancia del trabajo realizado.
     *
     * <p>Comparte con {@link #atenderTicket} la comprobacion de pertenencia: el
     * cierre lo firma en la bitacora quien lo ejecuta, de modo que solo puede
     * hacerlo el tecnico asignado o un ADMINISTRADOR.</p>
     *
     * <p>La clave del plan de trabajo se valida contra el catalogo antes de
     * guardarla. Es la que agrupa los tickets por meta anual, y un valor fuera
     * de catalogo falsearia ese conteo sin producir ningun error visible.</p>
     */
    @Transactional
    public TicketResponse resolverTicket(Long ticketId, String justificacion,
                                         Integer planTrabajoClave, String correoUsuario) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Usuario tecnico = obtenerUsuarioAutenticado(correoUsuario);
        verificarPuedeOperarElTicket(ticket, tecnico);

        if (planTrabajoClave != null && !PlanTrabajo.esClaveValida(planTrabajoClave)) {
            throw new IllegalArgumentException(
                    "La meta del plan de trabajo indicada no existe en el catalogo.");
        }

        ticket.setEstado(EstadoTicket.CERRADO);
        ticket.setFechaFin(LocalDateTime.now());

        if (justificacion != null) ticket.setJustificacion(justificacion);
        if (planTrabajoClave != null) ticket.setPlanTrabajoClave(planTrabajoClave);

        Ticket ticketGuardado = ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticketGuardado);
        bitacora.setEstatusRegistrado(EstadoTicket.CERRADO.name());
        bitacora.setJustificacion(justificacion);
        bitacoraRepository.save(bitacora);

        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
    }

    /**
     * Bandeja de tickets del usuario, paginada y acotada segun su rol.
     *
     * <p>El alcance lo determina {@code Rol.nivelVision}, y el recorte se aplica
     * en la consulta a la base, no sobre la pagina ya recuperada:</p>
     * <ul>
     *   <li>{@code GLOBAL} (ADMINISTRADOR): todos los tickets.</li>
     *   <li>{@code PERSONAL} (SOPORTE): los asignados a el y los que creo.</li>
     *   <li>{@code AREA} (EMPLEADO): solo los de su area.</li>
     * </ul>
     *
     * <p>Al ser un limite de visibilidad entre areas, se resuelve a partir del
     * usuario autenticado y no de ningun parametro de la peticion.</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> obtenerTicketsParaBandeja(
            String correoUsuario, String busqueda, String estatus, Pageable pageable) {

        Usuario usuario = usuarioRepository.findByCorreoOrUsername(correoUsuario, correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        if (usuario.getRol() == null) {
            throw new IllegalStateException("El usuario no tiene un rol asignado.");
        }

        String textoBusqueda = normalizarBusqueda(busqueda);
        EstadoTicket filtroEstado = normalizarEstado(estatus);
        Pageable paginaSegura = sanearOrden(pageable);

        String nivelVision = usuario.getRol().getNivelVision();

        // Un nivel que no figure en el enum niega el acceso en lugar de
        // conceder todo: ante una configuracion invalida, la opcion segura es
        // no mostrar nada.
        NivelVision alcance = NivelVision.desde(nivelVision).orElseThrow(() -> {
            log.error("Nivel de vision no reconocido: '{}' (usuario id={})", nivelVision, usuario.getId());
            return new IllegalStateException(
                    "El rol del usuario no tiene un nivel de visibilidad valido. Contacte al administrador.");
        });

        Page<Ticket> pagina = switch (alcance) {
            case GLOBAL -> ticketRepository.buscarTodosPaginado(textoBusqueda, filtroEstado, paginaSegura);

            case PERSONAL -> ticketRepository.buscarPorSoporteOCreadorPaginado(
                    usuario.getId(), textoBusqueda, filtroEstado, paginaSegura);

            case AREA -> {
                if (usuario.getArea() == null) {
                    // Sin area no hay conjunto que mostrar, y devolver todo
                    // saltaria precisamente el limite que este nivel impone.
                    log.warn("Usuario id={} con vision AREA pero sin area asignada.", usuario.getId());
                    yield Page.empty(paginaSegura);
                }
                yield ticketRepository.buscarPorAreaPaginado(
                        usuario.getArea().getId(), textoBusqueda, filtroEstado, paginaSegura);
            }
        };

        return PageResponse.de(pagina, this::mapearATicketResponse);
    }

    /**
     * Historial del area del usuario, paginado.
     *
     * <p>Un ADMINISTRADOR no tiene area propia, asi que recibe la vision
     * global que le corresponde en lugar de una lista vacia.</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> obtenerTicketsDeMiAreaPaginado(
            String correoUsuario, String busqueda, String estatus, Pageable pageable) {

        Usuario usuario = usuarioRepository.findByCorreoOrUsername(correoUsuario, correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        String textoBusqueda = normalizarBusqueda(busqueda);
        EstadoTicket filtroEstado = normalizarEstado(estatus);
        Pageable paginaSegura = sanearOrden(pageable);

        boolean esAdministrador = usuario.getRol() != null
                && NivelVision.desde(usuario.getRol().getNivelVision())
                        .filter(NivelVision.GLOBAL::equals)
                        .isPresent();

        if (esAdministrador) {
            return PageResponse.de(
                    ticketRepository.buscarTodosPaginado(textoBusqueda, filtroEstado, paginaSegura),
                    this::mapearATicketResponse);
        }

        if (usuario.getArea() == null) {
            throw new IllegalStateException("Su cuenta no tiene un area asignada. Contacte al administrador.");
        }

        return PageResponse.de(
                ticketRepository.buscarPorAreaPaginado(
                        usuario.getArea().getId(), textoBusqueda, filtroEstado, paginaSegura),
                this::mapearATicketResponse);
    }

    /**
     * Catalogo de metas del plan anual de trabajo.
     *
     * <p>Lo consume el desplegable de resolucion del panel de soporte. Se sirve
     * desde el enum {@link PlanTrabajo}, que es la misma fuente contra la que se
     * valida la clave al cerrar un ticket: asi el catalogo existe una sola vez y
     * anadir una meta no obliga a tocar el frontend.</p>
     */
    public List<PlanTrabajoResponse> obtenerCatalogoPlanTrabajo() {
        return Arrays.stream(PlanTrabajo.values())
                .map(meta -> new PlanTrabajoResponse(meta.getClave(), meta.getDescripcion()))
                .toList();
    }

    /**
     * Catalogo de prioridades, para el desplegable del formulario de alta.
     *
     * <p>Se sirve desde el enum en lugar de escribirlo en el JSX, para que la
     * pantalla y la validacion del servidor no puedan discrepar.</p>
     */
    public List<CatalogoResponse> obtenerCatalogoPrioridades() {
        return Arrays.stream(Prioridad.values())
                .map(p -> new CatalogoResponse(p.name(), p.getEtiqueta()))
                .toList();
    }

    /**
     * Campos por los que se admite ordenar la bandeja.
     *
     * <p>El {@code Pageable} se construye con lo que llega en la URL, asi que
     * sin esta lista un cliente podria ordenar por cualquier atributo de la
     * entidad —incluidos los de las relaciones, como la contrasena del usuario
     * asignado— y deducir informacion a partir del orden del resultado.</p>
     */
    private static final java.util.Set<String> CAMPOS_ORDENABLES = java.util.Set.of(
            "id", "titulo", "fechaCreacion", "fechaFin", "estado", "prioridad");

    /**
     * Depura la ordenacion recibida del cliente.
     *
     * <p>Descarta los campos que no estan en {@link #CAMPOS_ORDENABLES}.</p>
     *
     * <p><b>Limitacion conocida.</b> La prioridad esta mapeada como
     * {@code EnumType.STRING}, asi que ordenar por ella ordena por el texto
     * guardado: sale "ALTA, BAJA, NORMAL, URGENTE" en orden alfabetico y lo
     * urgente no queda arriba. Para el uso previsto —agrupar los tickets de la
     * misma prioridad— es suficiente, y la bandeja destaca lo urgente por
     * color, no por posicion. Resolverlo correctamente exige una columna con el
     * peso numerico; se deja anotado por si llega a molestar.</p>
     */
    private Pageable sanearOrden(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> permitidos = pageable.getSort().stream()
                .filter(orden -> {
                    boolean valido = CAMPOS_ORDENABLES.contains(orden.getProperty());
                    if (!valido) {
                        log.warn("Orden por campo no permitido: '{}'. Se ignora.", orden.getProperty());
                    }
                    return valido;
                })
                .toList();

        if (permitidos.isEmpty()) {
            // Sin criterio valido se usa el de siempre, para no devolver
            // paginas en orden arbitrario.
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(permitidos));
    }

    /**
     * Deja en {@code null} los filtros vacios.
     *
     * <p>Las consultas tratan {@code null} como "sin filtrar". Una cadena vacia
     * llegada del formulario no es lo mismo: comparada con LIKE '%%' colaria,
     * pero comparada con el estatus no encontraria ningun registro y la tabla
     * apareceria vacia sin motivo.</p>
     */
    private String normalizarFiltro(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /**
     * Prepara el texto de busqueda para el LIKE de las consultas.
     *
     * <p>Devuelve el texto en minusculas y rodeado de comodines, o
     * {@code null} si no hay nada que buscar. La transformacion se hace aqui y
     * no en la consulta porque PostgreSQL no puede inferir el tipo de un
     * parametro nulo dentro de {@code LOWER(...)}: lo trata como {@code bytea}
     * y la consulta falla en cuanto se filtra por estado sin texto de
     * busqueda.</p>
     *
     * <p>Se escapan los comodines propios de LIKE para que un usuario que
     * escriba "100%" busque ese texto literal y no cualquier cosa que empiece
     * por "100".</p>
     */
    private String normalizarBusqueda(String valor) {
        String texto = normalizarFiltro(valor);
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
     * Traduce el filtro de estado recibido del cliente al enum.
     *
     * <p>Un valor desconocido se trata como "sin filtro" en lugar de provocar
     * un error: el filtro es una comodidad de la interfaz, y un parametro mal
     * escrito en la URL no debe impedir ver la bandeja. Se deja constancia en
     * el log para poder detectar un desajuste entre pantalla y backend.</p>
     */
    private EstadoTicket normalizarEstado(String valor) {
        String texto = normalizarFiltro(valor);
        if (texto == null) {
            return null;
        }

        return EstadoTicket.desde(texto).orElseGet(() -> {
            log.warn("Filtro de estado no reconocido: '{}'. Se ignora.", texto);
            return null;
        });
    }

    /**
     * Publica el estado del ticket en {@code /topic/tickets-soporte}, que es el
     * canal al que se suscribe el panel para refrescarse sin recargar.
     *
     * <p>La excepcion no se propaga: la difusion es accesoria y el ticket ya
     * quedo guardado, de modo que un fallo del canal no debe deshacer la
     * operacion. Si falla, se registra con su causa y se avisa por el canal de
     * errores, para que la perdida del tiempo real no pase inadvertida.</p>
     */
    private void emitirEventoTicket(Ticket ticket) {
        try {
            messagingTemplate.convertAndSend("/topic/tickets-soporte", mapearATicketResponse(ticket));
        } catch (Exception e) {
            log.error("Fallo al emitir el evento WebSocket del ticket id={}. "
                    + "El panel de soporte no se actualizara en tiempo real.", ticket.getId(), e);
            notificarFalloTiempoReal(ticket.getId(), e);
        }
    }

    /**
     * Avisa a los clientes conectados de que la sincronizacion en vivo fallo,
     * para que recarguen manualmente en lugar de quedarse con datos obsoletos
     * sin saberlo.
     */
    private void notificarFalloTiempoReal(Long ticketId, Exception causa) {
        try {
            // Se tipa el payload para desambiguar la sobrecarga de
            // convertAndSend(String, Object) frente a (String, Map headers).
            Map<String, String> aviso = new HashMap<>();
            aviso.put("tipo", "SINCRONIZACION_FALLIDA");
            aviso.put("ticketId", String.valueOf(ticketId));
            aviso.put("mensaje", "No se pudo sincronizar el ticket en tiempo real. Actualice la vista.");
            aviso.put("causa", causa.getClass().getSimpleName());

            messagingTemplate.convertAndSend("/topic/errores", (Object) aviso);
        } catch (Exception e) {
            // Si tambien falla el canal de errores, el broker esta caido por
            // completo: solo queda dejar constancia en el log del servidor.
            log.error("El canal de errores WebSocket tampoco responde.", e);
        }
    }
}