package com.sedif.sistema_tickets.core.resguardo;

import com.sedif.sistema_tickets.core.telegram.SedifTelegramBot;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Calculo de la fecha de vencimiento de un resguardo.
 *
 * <p>De esa fecha depende que la revision programada detecte el prestamo: un
 * resguardo sin vencimiento nunca pasa a VENCIDO, de modo que el equipo puede
 * quedarse prestado indefinidamente sin que nadie lo advierta. Solo el plazo
 * declarado indefinido debe producir ese resultado.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResguardoService: calculo del vencimiento")
class ResguardoServiceVencimientoTest {

    private static final String CORREO = "soporte@sedif.local";

    @Mock private ResguardoRepository resguardoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SedifTelegramBot telegramBot;

    private ResguardoService servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new ResguardoService(
                resguardoRepository, usuarioRepository, messagingTemplate, telegramBot);

        Usuario creador = new Usuario();
        creador.setId(1L);
        creador.setNombre("Soporte");
        creador.setCorreo(CORREO);

        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(creador));
        when(resguardoRepository.save(any(Resguardo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("en dias suma esa cantidad de dias")
    void plazoEnDias() {
        LocalDateTime antes = LocalDateTime.now();

        servicio.crearResguardo(peticion("dias", 15), CORREO);

        LocalDateTime vencimiento = resguardoGuardado().getFechaVencimiento();
        assertThat(vencimiento).isNotNull();
        assertThat(ChronoUnit.DAYS.between(antes, vencimiento)).isEqualTo(15);
    }

    @Test
    @DisplayName("en semanas suma siete dias por semana")
    void plazoEnSemanas() {
        LocalDateTime antes = LocalDateTime.now();

        servicio.crearResguardo(peticion("semanas", 2), CORREO);

        LocalDateTime vencimiento = resguardoGuardado().getFechaVencimiento();
        assertThat(ChronoUnit.DAYS.between(antes, vencimiento)).isEqualTo(14);
    }

    @Test
    @DisplayName("en meses avanza al mismo dia del mes correspondiente")
    void plazoEnMeses() {
        LocalDateTime antes = LocalDateTime.now();

        servicio.crearResguardo(peticion("meses", 6), CORREO);

        LocalDateTime vencimiento = resguardoGuardado().getFechaVencimiento();
        assertThat(vencimiento).isAfter(antes);
        assertThat(ChronoUnit.MONTHS.between(antes, vencimiento)).isEqualTo(6);
    }

    @Test
    @DisplayName("indefinido deja el resguardo sin fecha de vencimiento")
    void plazoIndefinido() {
        servicio.crearResguardo(peticion("indefinido", 30), CORREO);

        assertThat(resguardoGuardado().getFechaVencimiento()).isNull();
    }

    @ParameterizedTest(name = "\"{0}\" se interpreta igual que en minusculas")
    @ValueSource(strings = {"DIAS", "Dias", "  dias  "})
    @DisplayName("la unidad tolera mayusculas y espacios")
    void unidadNormalizada(String tipo) {
        LocalDateTime antes = LocalDateTime.now();

        servicio.crearResguardo(peticion(tipo, 3), CORREO);

        LocalDateTime vencimiento = resguardoGuardado().getFechaVencimiento();
        assertThat(vencimiento).isNotNull();
        assertThat(ChronoUnit.DAYS.between(antes, vencimiento)).isEqualTo(3);
    }

    @Test
    @DisplayName("una unidad desconocida no produce vencimiento")
    void unidadDesconocida() {
        servicio.crearResguardo(peticion("quincenas", 2), CORREO);

        assertThat(resguardoGuardado().getFechaVencimiento()).isNull();
    }

    @Test
    @DisplayName("sin cantidad no hay plazo que calcular")
    void sinCantidad() {
        servicio.crearResguardo(peticion("dias", null), CORREO);

        assertThat(resguardoGuardado().getFechaVencimiento()).isNull();
    }

    @Test
    @DisplayName("una cantidad de cero o negativa no produce vencimiento")
    void cantidadNoPositiva() {
        servicio.crearResguardo(peticion("dias", 0), CORREO);

        assertThat(resguardoGuardado().getFechaVencimiento()).isNull();
    }

    @Test
    @DisplayName("el resguardo nace ENTREGADO y vinculado a quien lo captura")
    void estadoInicial() {
        servicio.crearResguardo(peticion("dias", 10), CORREO);

        Resguardo guardado = resguardoGuardado();
        assertThat(guardado.getEstado()).isEqualTo(EstadoResguardo.ENTREGADO);
        assertThat(guardado.getUsuarioCreador().getCorreo()).isEqualTo(CORREO);
    }

    // ------------------------------------------------------------ apoyos

    private Resguardo resguardoGuardado() {
        ArgumentCaptor<Resguardo> captor = ArgumentCaptor.forClass(Resguardo.class);
        verify(resguardoRepository).save(captor.capture());
        return captor.getValue();
    }

    private ResguardoRequest peticion(String duracionTipo, Integer duracionCantidad) {
        return new ResguardoRequest(
                "JUAN PEREZ", "E-123", "LAPTOP DELL", "SN-0001", "CARGADOR",
                duracionCantidad, duracionTipo,
                "2221234567", "SISTEMAS", "INV-9", "BUENO", null);
    }
}
