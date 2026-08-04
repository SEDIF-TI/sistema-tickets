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

@Service
@Slf4j
@RequiredArgsConstructor
public class ResguardoService {

    private final ResguardoRepository resguardoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SedifTelegramBot telegramBot;

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
     * Calcula la fecha en que vence el prestamo.
     *
     * <p>Se extrajo a un metodo propio porque la version anterior solo
     * contemplaba "Dias" y "Semanas": cualquier otro valor caia en el
     * {@code else} y dejaba el resguardo <b>sin fecha de vencimiento</b>, de
     * modo que nunca aparecia como vencido y el equipo podia quedarse prestado
     * indefinidamente sin que nadie lo detectara.</p>
     *
     * @return la fecha de vencimiento, o {@code null} si el prestamo es
     *         indefinido de forma deliberada.
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

    /**
     * Historial paginado. Con 40 resguardos ya registrados y creciendo, traer
     * la tabla completa en cada consulta no se sostiene.
     */
    @Transactional(readOnly = true)
    public PageResponse<ResguardoResponse> listarPaginado(Pageable pageable) {
        return listarPaginado(null, null, pageable);
    }

    /**
     * Listado paginado con busqueda y filtro de estado resueltos en la base.
     *
     * <p>La pantalla filtraba sobre la pagina ya descargada, asi que buscar un
     * numero de serie solo miraba los diez resguardos visibles.</p>
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
     * Resguardos proximos a vencer, para poder avisar al usuario ANTES de que
     * el prestamo caduque en lugar de reclamarlo despues.
     *
     * @param dias ventana de anticipacion (por defecto 7).
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

    @Transactional
    public ResguardoResponse devolverEquipo(Long resguardoId) {
        Resguardo resguardo = resguardoRepository.findById(resguardoId)
                .orElseThrow(() -> new IllegalArgumentException("Resguardo no encontrado."));

        // Un resguardo ya devuelto no puede devolverse dos veces: sin esta
        // comprobacion, un doble clic reescribia la fecha de modificacion y
        // falseaba la trazabilidad.
        if (resguardo.getEstado() == EstadoResguardo.DEVUELTO) {
            throw new IllegalStateException("Este resguardo ya fue devuelto.");
        }

        resguardo.setEstado(EstadoResguardo.DEVUELTO);
        return ResguardoResponse.desdeEntidad(resguardoRepository.save(resguardo));
    }

    @Transactional
    @Scheduled(cron = "${app.resguardos.revision-vencimientos-cron:0 0 6 * * *}")
    public void verificarVencimientos() {
        LocalDateTime ahora = LocalDateTime.now();
        // 👈 CORREGIDO: Buscamos resguardos con estado ENTREGADO
        List<Resguardo> vencidos = resguardoRepository.findByFechaVencimientoBeforeAndEstado(ahora, EstadoResguardo.ENTREGADO);

        if (vencidos.isEmpty()) {
            log.debug("Revision de vencimientos: no hay resguardos vencidos hoy.");
            return;
        }

        for (Resguardo r : vencidos) {
            try {
                r.setEstado(EstadoResguardo.VENCIDO);
                resguardoRepository.save(r);

                if (r.getUsuarioCreador() != null && r.getUsuarioCreador().getTelegramChatId() != null) {
                    try {
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