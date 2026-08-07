package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.core.ticket.bitacora.Bitacora;
import com.sedif.sistema_tickets.core.ticket.bitacora.BitacoraRepository;
import com.sedif.sistema_tickets.core.ticket.filtros.TicketFiltroStrategy;
import com.sedif.sistema_tickets.core.usuarios.Rol;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pertenencia del ticket y transiciones de estado.
 *
 * <p>Comprueba que un tecnico solo puede operar sobre los tickets que tiene
 * asignados y que un ticket cerrado no vuelve atras. Son limites que, cuando
 * se rompen, no producen ningun error: la operacion simplemente se ejecuta
 * sobre un ticket ajeno o sobre uno ya terminado.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TicketService: permisos y transiciones")
class TicketServicePermisosTest {

    private static final Long TICKET_ID = 500L;

    @Mock private TicketRepository ticketRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private BitacoraRepository bitacoraRepository;
    @Mock private SedifTelegramBot telegramBot;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private TicketService servicio;

    private Usuario tecnicoAsignado;
    private Usuario otroTecnico;
    private Usuario administrador;

    @BeforeEach
    void prepararServicio() {
        Map<String, TicketFiltroStrategy> estrategias = Map.of();
        servicio = new TicketService(
                telegramBot, ticketRepository, usuarioRepository,
                estrategias, bitacoraRepository, messagingTemplate);

        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        tecnicoAsignado = usuario(1L, "Tecnico Asignado", rol("SOPORTE", "PERSONAL"));
        otroTecnico = usuario(2L, "Otro Tecnico", rol("SOPORTE", "PERSONAL"));
        administrador = usuario(3L, "Administrador", rol("ADMINISTRADOR", "GLOBAL"));

        registrarUsuario(tecnicoAsignado);
        registrarUsuario(otroTecnico);
        registrarUsuario(administrador);
    }

    @Nested
    @DisplayName("atender")
    class Atender {

        @Test
        @DisplayName("el tecnico asignado pasa el ticket a EN_PROCESO")
        void elAsignadoPuedeAtender() {
            prepararTicket(EstadoTicket.ABIERTO, tecnicoAsignado);

            servicio.atenderTicket(TICKET_ID, tecnicoAsignado.getCorreo());

            assertThat(ticketGuardado().getEstado()).isEqualTo(EstadoTicket.EN_PROCESO);
        }

        @Test
        @DisplayName("otro tecnico no puede atender un ticket ajeno")
        void otroTecnicoNoPuedeAtender() {
            prepararTicket(EstadoTicket.ABIERTO, tecnicoAsignado);

            assertThatThrownBy(() -> servicio.atenderTicket(TICKET_ID, otroTecnico.getCorreo()))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("asignado a otro tecnico");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("el administrador puede atender cualquier ticket")
        void elAdministradorPuedeAtenderCualquiera() {
            prepararTicket(EstadoTicket.ABIERTO, tecnicoAsignado);

            servicio.atenderTicket(TICKET_ID, administrador.getCorreo());

            assertThat(ticketGuardado().getEstado()).isEqualTo(EstadoTicket.EN_PROCESO);
        }

        @Test
        @DisplayName("un ticket sin tecnico no lo puede atender un tecnico cualquiera")
        void ticketSinAsignarNoSeAtiendePorCualquiera() {
            prepararTicket(EstadoTicket.ABIERTO, null);

            assertThatThrownBy(() -> servicio.atenderTicket(TICKET_ID, otroTecnico.getCorreo()))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("un ticket cerrado no vuelve a EN_PROCESO")
        void noSeAtiendeUnTicketCerrado() {
            prepararTicket(EstadoTicket.CERRADO, tecnicoAsignado);

            assertThatThrownBy(() -> servicio.atenderTicket(TICKET_ID, tecnicoAsignado.getCorreo()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya esta cerrado");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("deja constancia en la bitacora con el nombre de quien atiende")
        void registraEnBitacora() {
            prepararTicket(EstadoTicket.ABIERTO, tecnicoAsignado);

            servicio.atenderTicket(TICKET_ID, tecnicoAsignado.getCorreo());

            ArgumentCaptor<Bitacora> captor = ArgumentCaptor.forClass(Bitacora.class);
            verify(bitacoraRepository).save(captor.capture());

            Bitacora registro = captor.getValue();
            assertThat(registro.getEstatusRegistrado()).isEqualTo(EstadoTicket.EN_PROCESO.name());
            assertThat(registro.getJustificacion()).contains(tecnicoAsignado.getNombre());
        }
    }

    @Nested
    @DisplayName("resolver")
    class Resolver {

        @Test
        @DisplayName("el tecnico asignado cierra el ticket y fija la fecha de fin")
        void elAsignadoPuedeResolver() {
            prepararTicket(EstadoTicket.EN_PROCESO, tecnicoAsignado);

            servicio.resolverTicket(TICKET_ID, "Se reemplazo la fuente.", 7,
                    tecnicoAsignado.getCorreo());

            Ticket guardado = ticketGuardado();
            assertThat(guardado.getEstado()).isEqualTo(EstadoTicket.CERRADO);
            assertThat(guardado.getFechaFin()).isNotNull();
            assertThat(guardado.getPlanTrabajoClave()).isEqualTo(7);
        }

        @Test
        @DisplayName("otro tecnico no puede cerrar un ticket ajeno")
        void otroTecnicoNoPuedeResolver() {
            prepararTicket(EstadoTicket.EN_PROCESO, tecnicoAsignado);

            assertThatThrownBy(() -> servicio.resolverTicket(
                    TICKET_ID, "Resuelto", 7, otroTecnico.getCorreo()))
                    .isInstanceOf(SecurityException.class);

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("una clave de plan de trabajo fuera del catalogo rechaza el cierre")
        void planDeTrabajoInvalido() {
            prepararTicket(EstadoTicket.EN_PROCESO, tecnicoAsignado);

            assertThatThrownBy(() -> servicio.resolverTicket(
                    TICKET_ID, "Resuelto", 9999, tecnicoAsignado.getCorreo()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("plan de trabajo");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("sin clave de plan de trabajo el cierre se acepta igual")
        void sinPlanDeTrabajoSeCierra() {
            prepararTicket(EstadoTicket.EN_PROCESO, tecnicoAsignado);

            servicio.resolverTicket(TICKET_ID, "Resuelto", null, tecnicoAsignado.getCorreo());

            assertThat(ticketGuardado().getEstado()).isEqualTo(EstadoTicket.CERRADO);
        }
    }

    // ------------------------------------------------------------ apoyos

    private void prepararTicket(EstadoTicket estado, Usuario asignado) {
        Ticket ticket = new Ticket();
        ticket.setId(TICKET_ID);
        ticket.setTitulo("Equipo sin video");
        ticket.setEstado(estado);
        ticket.setUsuarioSoporte(asignado);
        ticket.setUsuarioArea(usuario(9L, "Solicitante", rol("EMPLEADO", "AREA")));

        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
    }

    private Ticket ticketGuardado() {
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        return captor.getValue();
    }

    /** El servicio resuelve al usuario autenticado por correo o por username. */
    private void registrarUsuario(Usuario usuario) {
        when(usuarioRepository.findByCorreoOrUsername(usuario.getCorreo(), usuario.getCorreo()))
                .thenReturn(Optional.of(usuario));
    }

    private Usuario usuario(Long id, String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setCorreo(nombre.toLowerCase().replace(' ', '.') + "@sedif.local");
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private Rol rol(String nombre, String nivelVision) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setNivelVision(nivelVision);
        return rol;
    }
}
