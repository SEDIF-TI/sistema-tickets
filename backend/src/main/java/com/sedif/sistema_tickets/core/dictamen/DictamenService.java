package com.sedif.sistema_tickets.core.dictamen;

import com.sedif.sistema_tickets.core.documento.DictamenRequest;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.exception.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@Slf4j
@RequiredArgsConstructor
public class DictamenService {

    private final DictamenRepository dictamenRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Guarda el dictamen que se acaba de emitir.
     *
     * <p>Se invoca desde la generacion del PDF: hasta la V10 el documento se
     * componia y se devolvia sin dejar rastro, de modo que no habia historial
     * posible. El guardado no debe impedir la entrega del PDF —el tecnico
     * necesita su documento aunque el registro falle—, asi que el llamador
     * decide como tratar el error.</p>
     */
    @Transactional
    public Dictamen registrar(DictamenRequest request, String correoTecnico) {
        Dictamen dictamen = new Dictamen();
        dictamen.setFolio(generarFolio());
        dictamen.setTicketId(request.folioTicket());

        dictamen.setCve(request.cve());
        dictamen.setDescripcionEquipo(request.descripcionEquipo());
        dictamen.setMarca(request.marca());
        dictamen.setModelo(request.modelo());
        dictamen.setSerie(request.serie());
        dictamen.setNoResguardo(request.noResguardo());

        dictamen.setNombreUsuario(request.nombreUsuario());
        dictamen.setTelefonoUsuario(request.telefonoUsuario());
        dictamen.setDireccionUsuario(request.direccionUsuario());
        dictamen.setDepartamentoUsuario(request.departamentoUsuario());
        dictamen.setTipoReporte(request.tipoReporte());

        dictamen.setFallaReportada(request.fallaReportada());
        dictamen.setDiagnostico(request.diagnostico());

        dictamen.setRealizadoPor(request.realizadoPor());
        dictamen.setRevisadoPor(request.revisadoPor());
        dictamen.setRecibidoPor(request.recibidoPor());

        // El tecnico se resuelve por el correo de la sesion. Si no se
        // encuentra, el dictamen se guarda igual: perder la referencia al
        // emisor es preferible a perder el documento.
        if (correoTecnico != null) {
            usuarioRepository.findByCorreo(correoTecnico)
                    .ifPresentOrElse(
                            dictamen::setTecnico,
                            () -> log.warn("No se encontro el tecnico {} al registrar el dictamen.", correoTecnico));
        }

        Dictamen guardado = dictamenRepository.save(dictamen);
        log.info("Dictamen tecnico registrado con folio {}", guardado.getFolio());
        return guardado;
    }

    /** Historial paginado con busqueda resuelta en la base. */
    @Transactional(readOnly = true)
    public PageResponse<DictamenResponse> listarPaginado(String busqueda, Pageable pageable) {
        return PageResponse.de(
                dictamenRepository.buscarPaginado(normalizarBusqueda(busqueda), pageable),
                DictamenResponse::desdeEntidad);
    }

    @Transactional(readOnly = true)
    public DictamenResponse obtener(Long id) {
        return dictamenRepository.findById(id)
                .map(DictamenResponse::desdeEntidad)
                .orElseThrow(() -> new IllegalArgumentException("El dictamen indicado no existe."));
    }

    /**
     * Genera el folio del ano en curso, con el mismo criterio que el taller.
     *
     * <p>Se toma el maximo consecutivo del ano en lugar de {@code count() + 1}:
     * eso ultimo duplicaria folios si dos tecnicos emiten a la vez y nunca
     * reiniciaria la numeracion al cambiar de ano. La restriccion UNIQUE de la
     * columna es la garantia final.</p>
     */
    private String generarFolio() {
        String prefijo = "DIC-" + Year.now().getValue() + "-";

        int siguiente = dictamenRepository.findMaxConsecutivoDelAnio(prefijo + "%")
                .map(ultimo -> ultimo + 1)
                .orElse(1);

        return prefijo + siguiente;
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
}
