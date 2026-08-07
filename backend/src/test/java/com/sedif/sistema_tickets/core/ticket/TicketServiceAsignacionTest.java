package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.core.ticket.bitacora.BitacoraRepository;
import com.sedif.sistema_tickets.core.ticket.filtros.TicketFiltroStrategy;
import com.sedif.sistema_tickets.core.usuarios.Rol;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import com.sedif.sistema_tickets.util.enums.Prioridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Asignacion de tecnico al levantar un ticket.
 *
 * <p>Cubre las tres vias de {@code crearTicket} y el reparto automatico, que
 * decide quien atiende cada reporte. Es la regla de negocio con mas ramas del
 * sistema y la que no produce ningun error visible cuando falla: un ticket mal
 * asignado simplemente no aparece en la bandeja de quien deberia atenderlo.</p>
 *
 * <p>Los repositorios se simulan, de modo que las pruebas no necesitan base de
 * datos y comprueban unicamente la decision, no su persistencia.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TicketService: asignacion de tecnico")
class TicketServiceAsignacionTest {

    private static final String CORREO_SOLICITANTE = "empleado@sedif.local";

    @Mock private TicketRepository ticketRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private BitacoraRepository bitacoraRepository;
    @Mock private SedifTelegramBot telegramBot;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private TicketService servicio;

    @BeforeEach
    void prepararServicio() {
        Map<String, TicketFiltroStrategy> estrategias = Map.of();
        servicio = new TicketService(
                telegramBot, ticketRepository, usuarioRepository,
                estrategias, bitacoraRepository, messagingTemplate);

        // El ticket se devuelve tal cual se guarda: lo que se comprueba es la
        // decision de asignacion, no lo que hace la base con la entidad.
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    // ------------------------------------------------------------- via 1

    @Test
    @DisplayName("con usuarioSoporteId asigna a ese tecnico, sin consultar el reparto")
    void asignacionDirecta() {
        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));
        Usuario elegido = usuario(77L, "Tecnico Elegido", rol("SOPORTE", "PERSONAL"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(elegido));

        servicio.crearTicket(peticion(77L), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getUsuarioSoporte()).isEqualTo(elegido);

        // La via directa no debe pasar por el reparto por carga.
        verify(usuarioRepository, never())
                .buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any());
    }

    @Test
    @DisplayName("con un usuarioSoporteId inexistente rechaza el alta")
    void asignacionDirectaConTecnicoInexistente() {
        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crearTicket(peticion(999L), CORREO_SOLICITANTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("soporte no encontrado");

        verify(ticketRepository, never()).save(any());
    }

    // ------------------------------------------------------------- via 2

    @Test
    @DisplayName("un tecnico que levanta el ticket se queda con el")
    void autoAsignacionDelTecnico() {
        Usuario tecnico = usuario(20L, "Tecnico Autor", rol("SOPORTE", "PERSONAL"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(tecnico));

        servicio.crearTicket(peticion(null), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getUsuarioSoporte()).isEqualTo(tecnico);
        verify(usuarioRepository, never())
                .buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any());
    }

    // ------------------------------------------------------------- via 3

    @Test
    @DisplayName("el soporte fijo del area tiene preferencia sobre el reparto por carga")
    void repartoPrefiereElSoporteFijo() {
        Usuario fijo = usuario(30L, "Soporte Fijo", rol("SOPORTE", "PERSONAL"));
        Usuario menosCargado = usuario(31L, "Otro Tecnico", rol("SOPORTE", "PERSONAL"));

        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));
        solicitante.setArea(areaCon(fijo));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of(menosCargado));

        servicio.crearTicket(peticion(null), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getUsuarioSoporte()).isEqualTo(fijo);
    }

    @Test
    @DisplayName("un soporte fijo inactivo cede al reparto por carga")
    void soporteFijoInactivoNoSeAsigna() {
        Usuario fijo = usuario(30L, "Soporte Fijo", rol("SOPORTE", "PERSONAL"));
        fijo.setActivo(false);

        Usuario menosCargado = usuario(31L, "Otro Tecnico", rol("SOPORTE", "PERSONAL"));

        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));
        solicitante.setArea(areaCon(fijo));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of(menosCargado));

        servicio.crearTicket(peticion(null), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getUsuarioSoporte()).isEqualTo(menosCargado);
    }

    @Test
    @DisplayName("un soporte fijo no disponible cede al reparto por carga")
    void soporteFijoNoDisponibleNoSeAsigna() {
        Usuario fijo = usuario(30L, "Soporte Fijo", rol("SOPORTE", "PERSONAL"));
        fijo.setDisponibleSoporte(false);

        Usuario menosCargado = usuario(31L, "Otro Tecnico", rol("SOPORTE", "PERSONAL"));

        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));
        solicitante.setArea(areaCon(fijo));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of(menosCargado));

        servicio.crearTicket(peticion(null), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getUsuarioSoporte()).isEqualTo(menosCargado);
    }

    @Test
    @DisplayName("sin soporte fijo toma el primero de la lista ordenada por carga")
    void repartoTomaElPrimeroPorCarga() {
        Usuario menosCargado = usuario(40L, "Menos Cargado", rol("SOPORTE", "PERSONAL"));
        Usuario masCargado = usuario(41L, "Mas Cargado", rol("SOPORTE", "PERSONAL"));

        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of(menosCargado, masCargado));

        servicio.crearTicket(peticion(null), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getUsuarioSoporte()).isEqualTo(menosCargado);
    }

    @Test
    @DisplayName("sin tecnicos disponibles el ticket se guarda sin asignar")
    void sinTecnicosElTicketSeGuardaIgual() {
        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of());

        servicio.crearTicket(peticion(null), CORREO_SOLICITANTE);

        // Perder el reporte seria peor que dejarlo pendiente de asignar.
        Ticket guardado = ticketGuardado();
        assertThat(guardado.getUsuarioSoporte()).isNull();
        assertThat(guardado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
    }

    // ------------------------------------------------- estado y prioridad

    @Test
    @DisplayName("el ticket nace ABIERTO y con la prioridad recibida")
    void estadoInicialYPrioridad() {
        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of());

        servicio.crearTicket(new TicketRequestRecord(
                "Titulo", "Sede", "Descripcion", "URGENTE", null, null), CORREO_SOLICITANTE);

        Ticket guardado = ticketGuardado();
        assertThat(guardado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(guardado.getPrioridad()).isEqualTo(Prioridad.URGENTE);
    }

    @Test
    @DisplayName("una prioridad no reconocida cae en la de por defecto sin rechazar el alta")
    void prioridadDesconocidaCaeEnLaPorDefecto() {
        Usuario solicitante = usuario(10L, "Empleado", rol("EMPLEADO", "AREA"));

        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.buscarTecnicosDisponiblesOrdenadosPorCarga(anyString(), any()))
                .thenReturn(List.of());

        servicio.crearTicket(new TicketRequestRecord(
                "Titulo", "Sede", "Descripcion", "INVENTADA", null, null), CORREO_SOLICITANTE);

        assertThat(ticketGuardado().getPrioridad()).isEqualTo(Prioridad.porDefecto());
    }

    @Test
    @DisplayName("un correo que no corresponde a ningun usuario rechaza el alta")
    void solicitanteInexistente() {
        when(usuarioRepository.findByCorreo(CORREO_SOLICITANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crearTicket(peticion(null), CORREO_SOLICITANTE))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ------------------------------------------------------------ apoyos

    /** Recupera la entidad tal como llego al repositorio. */
    private Ticket ticketGuardado() {
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        return captor.getValue();
    }

    private TicketRequestRecord peticion(Long usuarioSoporteId) {
        return new TicketRequestRecord(
                "No enciende el equipo", "Oficinas centrales",
                "El equipo no da senal de video.", "NORMAL", null, usuarioSoporteId);
    }

    private Usuario usuario(Long id, String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setCorreo(nombre.toLowerCase().replace(' ', '.') + "@sedif.local");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setDisponibleSoporte(true);
        return usuario;
    }

    private Rol rol(String nombre, String nivelVision) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setNivelVision(nivelVision);
        return rol;
    }

    private Area areaCon(Usuario soporteFijo) {
        Area area = new Area();
        area.setId(1L);
        area.setNombre("Area de prueba");
        area.setSoporteFijo(soporteFijo);
        return area;
    }
}
