package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import com.sedif.sistema_tickets.core.estatusticket.EstatusRepository;
import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.core.ticket.bitacora.Bitacora;
import com.sedif.sistema_tickets.core.ticket.bitacora.BitacoraRepository;
import com.sedif.sistema_tickets.core.ticket.filtros.TicketFiltroStrategy;
import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.exception.PageResponse;
import com.sedif.sistema_tickets.util.enums.PlanTrabajo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// ---> IMPORTACIÓN NECESARIA PARA WEBSOCKETS
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final SedifTelegramBot telegramBot;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstatusRepository estatusRepository;
    private final Map<String, TicketFiltroStrategy> estrategiasFiltro;
    private final BitacoraRepository bitacoraRepository;
    
    // ---> INYECTAMOS LA HERRAMIENTA DE EMISIÓN DE WEBSOCKETS
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public TicketResponse crearTicket(TicketRequestRecord request, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Estatus estatusAbierto = estatusRepository.findByNombre("ABIERTO")
                .orElseThrow(() -> new IllegalStateException("Estatus ABIERTO no configurado en la base de datos"));

        Ticket nuevoTicket = new Ticket();
        nuevoTicket.setTitulo(request.titulo());
        nuevoTicket.setDescripcion(request.descripcion());
        nuevoTicket.setSede(request.sede());
        
        // Guardamos el nombre de quien reporta / tiene el resguardo
        if (request.solicitante() != null && !request.solicitante().isBlank()) {
            nuevoTicket.setSolicitanteNombre(request.solicitante());
        }

        nuevoTicket.setUsuarioArea(usuario); 
        nuevoTicket.setEstatus(estatusAbierto); 
        nuevoTicket.setFechaCreacion(LocalDateTime.now());
        nuevoTicket.setCreadoPor(usuario.getCorreo());
        
        String prioridad = (request.prioridad() != null && !request.prioridad().isBlank()) 
                           ? request.prioridad() 
                           : "NORMAL"; 
        nuevoTicket.setPrioridad(prioridad);

        // ---> NUEVA LÓGICA DE ASIGNACIÓN <---
        if (request.usuarioSoporteId() != null) {
            // 1. Asignación directa (Manual por Administrador)
            Usuario soporteElegido = usuarioRepository.findById(request.usuarioSoporteId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario de soporte no encontrado"));
            nuevoTicket.setUsuarioSoporte(soporteElegido);
            log.debug("Ticket asignado manualmente al tecnico id={}", soporteElegido.getId());
            
        } else if (usuario.getRol() != null && "SOPORTE".equals(usuario.getRol().getNombre())) {
            // 2. Auto-asignación (Si un soporte crea el ticket)
            nuevoTicket.setUsuarioSoporte(usuario);
            log.debug("Ticket auto-asignado al tecnico que lo creo.");
            
        } else {
            // 3. Balanceador automático (Si un usuario normal lo crea)
            Usuario soporteAsignado = resolverAsignacion(usuario);
            if (soporteAsignado != null) {
                nuevoTicket.setUsuarioSoporte(soporteAsignado);
                log.debug("Ticket asignado por el balanceador al tecnico id={}", soporteAsignado.getId());
            } else {
                log.warn("Ticket creado sin tecnico asignado: no hay soporte disponible.");
            }
        }

        Ticket ticketGuardado = ticketRepository.save(nuevoTicket);

        // Notificación automática por Telegram
        if (ticketGuardado.getUsuarioSoporte() != null && 
            ticketGuardado.getUsuarioSoporte().getTelegramChatId() != null) {
            
            // Extraemos el nombre de quien reporta (Dependiendo de si tu front lo manda en "sede" o "solicitanteNombre")
            String afectado = ticketGuardado.getSede(); 
            if (afectado == null || afectado.isBlank()) {
                afectado = ticketGuardado.getUsuarioArea().getNombre(); // Fallback al usuario de la sesión
            }

            // Armamos la plantilla completa con formato Markdown
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

        // ---> NUEVO: EMITIR EL EVENTO WEBSOCKET HACIA REACT
        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
    }

    private Usuario resolverAsignacion(Usuario usuarioArea) {
        if (usuarioArea.getArea() != null && usuarioArea.getArea().getSoporteFijo() != null) {
            Usuario fijo = usuarioArea.getArea().getSoporteFijo();
            if (Boolean.TRUE.equals(fijo.getActivo()) && Boolean.TRUE.equals(fijo.getDisponibleSoporte())) {
                return fijo;
            }
        }

        // Balanceo por carga resuelto en una sola consulta.
        //
        // La version anterior hacia findAll() de TODOS los usuarios y despues,
        // dentro del comparador, una consulta COUNT por cada tecnico y en cada
        // comparacion: un problema N+1 en el camino critico de creacion de
        // tickets. La consulta del repositorio ya devuelve a los tecnicos
        // disponibles ordenados por carga ascendente.
        List<Usuario> disponibles =
                usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga("SOPORTE", "ABIERTO");

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
                t.getEstatus().getNombre(),
                t.getUsuarioArea()!= null ? t.getUsuarioArea().getId() : null,
                t.getUsuarioSoporte() != null ? t.getUsuarioSoporte().getId() : null,
                justificacion
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
    
        ticket.setEstatus(estatusRepository.findByNombre("CERRADO").orElseThrow());
        ticket.setFechaFin(LocalDateTime.now());
        Ticket ticketGuardado = ticketRepository.save(ticket);

        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
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
     * Marca que el tecnico va en camino a atender el reporte.
     *
     * <p><b>Correccion de control de acceso.</b> La version anterior recibia
     * solo el id del ticket y no comprobaba nada mas: cualquier usuario con rol
     * SOPORTE podia mover el estado de un ticket asignado a otro tecnico
     * cambiando el numero de la URL, y la bitacora registraba el cambio sin
     * dejar constancia de quien lo habia hecho. Ahora se exige que el ticket
     * sea suyo, salvo que quien actue sea ADMINISTRADOR.</p>
     */
    @Transactional
    public TicketResponse atenderTicket(Long ticketId, String correoUsuario) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Usuario tecnico = obtenerUsuarioAutenticado(correoUsuario);
        verificarPuedeOperarElTicket(ticket, tecnico);

        if (ticket.getEstatus() != null
                && "CERRADO".equalsIgnoreCase(ticket.getEstatus().getNombre())) {
            throw new IllegalStateException(
                    "El ticket ya esta cerrado: no se puede volver a marcar en atencion.");
        }

        Estatus estatusEnCamino = estatusRepository.findByNombre("EN PROCESO")
                .orElseThrow(() -> new IllegalStateException("El estatus EN PROCESO no existe en la BD."));

        ticket.setEstatus(estatusEnCamino);
        Ticket ticketGuardado = ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticketGuardado);
        bitacora.setEstatusRegistrado("EN PROCESO");
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
                && "GLOBAL".equalsIgnoreCase(usuario.getRol().getNivelVision());

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
     * Cierra el ticket con la constancia del trabajo realizado.
     *
     * <p>Comparte con {@link #atenderTicket} la comprobacion de pertenencia:
     * antes cualquier tecnico podia cerrar el ticket de otro, firmando ademas
     * la bitacora en su nombre.</p>
     *
     * <p>La clave del plan de trabajo se valida contra el catalogo: hasta ahora
     * se guardaba cualquier entero que llegase, de modo que un valor fuera de
     * catalogo corrompia silenciosamente el conteo de metas del ano.</p>
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

        Estatus estatusResuelto = estatusRepository.findByNombre("CERRADO")
                .orElseThrow(() -> new IllegalStateException("El estatus CERRADO no existe en la BD."));

        ticket.setEstatus(estatusResuelto);
        ticket.setFechaFin(LocalDateTime.now());

        if (justificacion != null) ticket.setJustificacion(justificacion);
        if (planTrabajoClave != null) ticket.setPlanTrabajoClave(planTrabajoClave);

        Ticket ticketGuardado = ticketRepository.save(ticket);

        Bitacora bitacora = new Bitacora();
        bitacora.setTicket(ticketGuardado);
        bitacora.setEstatusRegistrado("CERRADO");
        bitacora.setJustificacion(justificacion);
        bitacoraRepository.save(bitacora);

        emitirEventoTicket(ticketGuardado);

        return mapearATicketResponse(ticketGuardado);
    }

    /**
     * Bandeja de tickets del usuario, paginada y filtrada segun su rol.
     *
     * <p><b>Correccion de seguridad.</b> La version anterior solo distinguia
     * entre SOPORTE y "todo lo demas": cualquier EMPLEADO caia en
     * {@code obtenerTodosLosTickets()} y recibia titulos, descripciones,
     * solicitantes y areas de TODA la institucion. Fuga de datos entre areas
     * (control de acceso a nivel de objeto, OWASP A01).</p>
     *
     * <p>Ahora la visibilidad se decide por {@code Rol.nivelVision}:</p>
     * <ul>
     *   <li>{@code GLOBAL} (ADMINISTRADOR): todos los tickets.</li>
     *   <li>{@code PERSONAL} (SOPORTE): los asignados a el y los que creo.</li>
     *   <li>{@code AREA} (EMPLEADO): solo los de su area.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> obtenerTicketsParaBandeja(
            String correoUsuario, String busqueda, String estatus, Pageable pageable) {

        Usuario usuario = usuarioRepository.findByCorreoOrUsername(correoUsuario, correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        if (usuario.getRol() == null) {
            throw new IllegalStateException("El usuario no tiene un rol asignado.");
        }

        String textoBusqueda = normalizarFiltro(busqueda);
        String filtroEstatus = normalizarFiltro(estatus);

        String nivelVision = usuario.getRol().getNivelVision();
        Page<Ticket> pagina = switch (nivelVision == null ? "" : nivelVision.toUpperCase()) {
            case "GLOBAL" -> ticketRepository.buscarTodosPaginado(textoBusqueda, filtroEstatus, pageable);

            case "PERSONAL" -> ticketRepository.buscarPorSoporteOCreadorPaginado(
                    usuario.getId(), textoBusqueda, filtroEstatus, pageable);

            case "AREA" -> {
                if (usuario.getArea() == null) {
                    // Sin area no hay nada que mostrar. Devolver todo seria
                    // repetir exactamente el fallo que se esta corrigiendo.
                    log.warn("Usuario id={} con vision AREA pero sin area asignada.", usuario.getId());
                    yield Page.empty(pageable);
                }
                yield ticketRepository.buscarPorAreaPaginado(
                        usuario.getArea().getId(), textoBusqueda, filtroEstatus, pageable);
            }

            // Nivel desconocido: se niega el acceso en lugar de conceder todo.
            default -> {
                log.error("Nivel de vision no reconocido: '{}' (usuario id={})", nivelVision, usuario.getId());
                throw new IllegalStateException(
                        "El rol del usuario no tiene un nivel de visibilidad valido. Contacte al administrador.");
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

        String textoBusqueda = normalizarFiltro(busqueda);
        String filtroEstatus = normalizarFiltro(estatus);

        boolean esAdministrador = usuario.getRol() != null
                && "GLOBAL".equalsIgnoreCase(usuario.getRol().getNivelVision());

        if (esAdministrador) {
            return PageResponse.de(
                    ticketRepository.buscarTodosPaginado(textoBusqueda, filtroEstatus, pageable),
                    this::mapearATicketResponse);
        }

        if (usuario.getArea() == null) {
            throw new IllegalStateException("Su cuenta no tiene un area asignada. Contacte al administrador.");
        }

        return PageResponse.de(
                ticketRepository.buscarPorAreaPaginado(
                        usuario.getArea().getId(), textoBusqueda, filtroEstatus, pageable),
                this::mapearATicketResponse);
    }

    /**
     * Catalogo de metas del plan anual de trabajo.
     *
     * <p>Lo consume el desplegable de resolucion del panel de soporte, que
     * antes llevaba las doce metas escritas a mano en el JSX. Al servirlo desde
     * el enum, el catalogo deja de estar duplicado y una meta nueva no obliga a
     * recompilar el frontend.</p>
     */
    public List<PlanTrabajoResponse> obtenerCatalogoPlanTrabajo() {
        return Arrays.stream(PlanTrabajo.values())
                .map(meta -> new PlanTrabajoResponse(meta.getClave(), meta.getDescripcion()))
                .toList();
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
     * Publica el estado del ticket en el canal de soporte.
     *
     * <p>Antes cada emision estaba envuelta en un {@code catch (Exception e) {}}
     * vacio: si el canal fallaba, el panel de soporte dejaba de actualizarse en
     * tiempo real y no quedaba ni rastro del problema. Ahora el fallo se
     * registra con su causa y se avisa por el canal de errores.</p>
     *
     * <p>No se propaga la excepcion a proposito: la notificacion es accesoria y
     * el ticket ya se guardo correctamente. Perder el aviso en vivo no debe
     * deshacer la operacion de negocio.</p>
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