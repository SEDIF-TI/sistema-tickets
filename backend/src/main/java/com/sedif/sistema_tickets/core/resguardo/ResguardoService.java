package com.sedif.sistema_tickets.core.resguardo;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.exception.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ciclo de vida del resguardo: entrega, consulta, devolucion y caducidad.
 *
 * <p>El plazo se fija en el alta a partir de la duracion solicitada. Una tarea
 * programada revisa a diario los prestamos vencidos, los pasa a
 * {@code VENCIDO} y avisa a quien los registro por Telegram y por WebSocket.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResguardoService {

    private final ResguardoRepository resguardoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SedifTelegramBot telegramBot;

    /**
     * Registra la entrega de un equipo.
     *
     * <p>El resguardo nace en estado ENTREGADO y queda vinculado a quien lo
     * captura, que es el destinatario de los avisos de vencimiento.</p>
     */
    @Transactional
    public ResguardoResponse crearResguardo(ResguardoRequest request, String correoCreador) {
        Usuario usuario = usuarioRepository.findByCorreo(correoCreador)
                .orElseThrow(() -> new IllegalArgumentException("Usuario creador no encontrado"));

        Resguardo resguardo = new Resguardo();
        resguardo.setSolicitanteNombre(request.solicitanteNombre());
        resguardo.setSolicitanteNumero(request.solicitanteNumero());
        resguardo.setEquipoNombre(request.equipoNombre());
        resguardo.setNumeroSerie(request.numeroSerie());
        resguardo.setAccesorios(request.accesorios());
        resguardo.setEstado(EstadoResguardo.ENTREGADO);
        
        resguardo.setTelefono(request.telefono());
        resguardo.setDepartamento(request.departamento());
        resguardo.setNumeroInventario(request.numeroInventario());
        resguardo.setCondiciones(request.condiciones());

        resguardo.setFechaVencimiento(calcularVencimiento(
                request.duracionTipo(), request.duracionCantidad()));

        resguardo.setUsuarioCreador(usuario);
        resguardoRepository.save(resguardo);
        
        return ResguardoResponse.desdeEntidad(resguardo);
    }

    /**
     * Calcula la fecha en que vence el prestamo sumando la cantidad indicada
     * en la unidad correspondiente (dias, semanas o meses).
     *
     * <p>Un resguardo sin fecha de vencimiento nunca lo detecta la revision
     * programada, de modo que el equipo puede quedarse prestado indefinidamente
     * sin que nadie lo advierta. Por eso el unico caso que devuelve
     * {@code null} sin dejar rastro es el prestamo declarado INDEFINIDO;
     * cualquier unidad no reconocida se registra en el log.</p>
     *
     * @return la fecha de vencimiento, o {@code null} si el prestamo no tiene
     *         plazo.
     */
    private LocalDateTime calcularVencimiento(String tipo, Integer cantidad) {
        if (tipo == null || cantidad == null || cantidad <= 0) {
            return null;
        }

        LocalDateTime ahora = LocalDateTime.now();
        return switch (tipo.trim().toUpperCase()) {
            case "DIAS" -> ahora.plusDays(cantidad);
            case "SEMANAS" -> ahora.plusWeeks(cantidad);
            case "MESES" -> ahora.plusMonths(cantidad);
            case "INDEFINIDO" -> null;
            default -> {
                // Un tipo no reconocido es un error de integracion, no una
                // peticion de prestamo indefinido: se deja constancia.
                log.warn("Tipo de duracion no reconocido: '{}'. El resguardo queda sin vencimiento.", tipo);
                yield null;
            }
        };
    }

    @Transactional(readOnly = true)
    public List<ResguardoResponse> listarTodos() {
        return resguardoRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(ResguardoResponse::desdeEntidad)
                .toList();
    }

    /** Historial paginado sin filtros, para cuando solo se navega por paginas. */
    @Transactional(readOnly = true)
    public PageResponse<ResguardoResponse> listarPaginado(Pageable pageable) {
        return listarPaginado(null, null, pageable);
    }

    /**
     * Listado paginado con busqueda y filtro de estado resueltos en la base.
     *
     * <p>Filtrar en la consulta y no sobre la pagina ya descargada es lo que
     * permite que buscar un numero de serie recorra todo el historial y no solo
     * los resguardos visibles.</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<ResguardoResponse> listarPaginado(
            String busqueda, String estado, Pageable pageable) {

        return PageResponse.de(
                resguardoRepository.buscarPaginado(
                        normalizarBusqueda(busqueda), convertirEstado(estado), pageable),
                ResguardoResponse::desdeEntidad);
    }

    /** Prepara el texto para el LIKE: minusculas, comodines y escape. */
    private String normalizarBusqueda(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String escapado = valor.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }

    /**
     * Traduce el filtro de estado al enum. Un valor desconocido se ignora en
     * lugar de romper la consulta: es una comodidad de la interfaz, no un dato
     * de negocio.
     */
    private EstadoResguardo convertirEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return EstadoResguardo.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Filtro de estado de resguardo no reconocido: '{}'. Se ignora.", valor);
            return null;
        }
    }

    /**
     * Resguardos que venceran dentro de los proximos dias, para avisar al
     * usuario antes de que el prestamo caduque en lugar de reclamarlo despues.
     *
     * <p>Solo considera los que siguen ENTREGADO: los devueltos y los ya
     * vencidos quedan fuera de la ventana.</p>
     *
     * @param dias ventana de anticipacion, contada desde el momento actual.
     */
    @Transactional(readOnly = true)
    public List<ResguardoResponse> listarPorVencer(int dias) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusDays(dias);

        return resguardoRepository
                .findByEstadoAndFechaVencimientoBetween(EstadoResguardo.ENTREGADO, ahora, limite)
                .stream()
                .map(ResguardoResponse::desdeEntidad)
                .toList();
    }

    /**
     * Cierra el resguardo al regresar el equipo.
     *
     * <p>Un resguardo VENCIDO tambien puede devolverse: el vencimiento indica
     * que el plazo expiro, no que el prestamo este cerrado.</p>
     */
    @Transactional
    public ResguardoResponse devolverEquipo(Long resguardoId) {
        Resguardo resguardo = resguardoRepository.findById(resguardoId)
                .orElseThrow(() -> new IllegalArgumentException("Resguardo no encontrado."));

        // La devolucion es una operacion unica: repetirla reescribiria la fecha
        // de modificacion del registro y falsearia la trazabilidad de cuando
        // regreso realmente el equipo. Un doble clic queda cubierto por aqui.
        if (resguardo.getEstado() == EstadoResguardo.DEVUELTO) {
            throw new IllegalStateException("Este resguardo ya fue devuelto.");
        }

        resguardo.setEstado(EstadoResguardo.DEVUELTO);
        return ResguardoResponse.desdeEntidad(resguardoRepository.save(resguardo));
    }

    /**
     * Revision programada de vencimientos: marca como VENCIDO todo prestamo
     * cuyo plazo expiro y sigue sin devolverse, y avisa a quien lo registro.
     *
     * <p>La hora de ejecucion se toma de
     * {@code app.resguardos.revision-vencimientos-cron}, con las seis de la
     * manana por defecto.</p>
     *
     * <p>Cada resguardo se procesa dentro de su propio {@code try}: un fallo al
     * enviar el aviso de Telegram o la notificacion por WebSocket se registra en
     * el log pero no interrumpe el recorrido, de modo que un destinatario
     * inaccesible no impide marcar los demas vencimientos.</p>
     */
    @Transactional
    @Scheduled(cron = "${app.resguardos.revision-vencimientos-cron:0 0 6 * * *}")
    public void verificarVencimientos() {
        LocalDateTime ahora = LocalDateTime.now();
        // Solo los ENTREGADO: los devueltos ya no interesan y los que ya estan
        // en VENCIDO no deben volver a notificarse cada dia.
        List<Resguardo> vencidos = resguardoRepository.findByFechaVencimientoBeforeAndEstado(ahora, EstadoResguardo.ENTREGADO);

        if (vencidos.isEmpty()) {
            log.debug("Revision de vencimientos: no hay resguardos vencidos hoy.");
            return;
        }

        for (Resguardo r : vencidos) {
            try {
                r.setEstado(EstadoResguardo.VENCIDO);
                resguardoRepository.save(r);

                // El aviso por Telegram requiere que quien registro el
                // resguardo haya vinculado su cuenta desde el perfil: sin chat
                // id no hay destinatario al que enviar.
                if (r.getUsuarioCreador() != null && r.getUsuarioCreador().getTelegramChatId() != null) {
                    try {
                        // Los datos del prestamo pueden venir incompletos, asi
                        // que cada uno lleva su valor de relleno: el mensaje
                        // debe salir aunque falte algun campo opcional.
                        String solicitante = r.getSolicitanteNombre() != null ? r.getSolicitanteNombre() : "SIN NOMBRE";
                        String numero = r.getSolicitanteNumero() != null ? r.getSolicitanteNumero() : "N/A";
                        String equipo = r.getEquipoNombre() != null ? r.getEquipoNombre() : "EQUIPO";
                        String serie = r.getNumeroSerie() != null ? r.getNumeroSerie() : "S/N";

                        String mensaje = "⚠️ ALERTA DE RESGUARDO VENCIDO ⚠️\n\n" +
                                        "El resguardo de " + solicitante + " ha vencido hoy.\n" +
                                        "👤 Solicitante: " + solicitante + " (No. " + numero + ")\n" +
                                        "💻 Equipo: " + equipo + " - SN: " + serie + "\n" +
                                        "Por favor, contacta al usuario para la devolución.";

                        telegramBot.enviarMensaje(r.getUsuarioCreador().getTelegramChatId(), mensaje);
                    } catch (Exception e) {
                        log.warn("No se pudo enviar el aviso de Telegram del resguardo id={}", r.getId(), e);
                    }
                }

                // Difusion por STOMP: las pantallas suscritas al canal de
                // alertas muestran el vencimiento sin esperar a recargar.
                try {
                    ResguardoResponse payload = ResguardoResponse.desdeEntidad(r);
                    messagingTemplate.convertAndSend("/topic/alertas-resguardos", payload);
                } catch (Exception e) {
                    log.warn("No se pudo emitir la alerta WebSocket del resguardo id={}", r.getId(), e);
                }

            } catch (Exception e) {
                log.error("Error al procesar el vencimiento del resguardo id={}", r.getId(), e);
            }
        }

        log.info("Revision de vencimientos: {} resguardos marcados como VENCIDO.", vencidos.size());
    }
}