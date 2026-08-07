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

/**
 * Registro y consulta de dictamenes tecnicos.
 *
 * <p>El alta la dispara la generacion del PDF, no un endpoint propio: emitir el
 * documento y dejar constancia de el son la misma operacion. La consulta del
 * historial vive en {@link DictamenResource}.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DictamenService {

    private final DictamenRepository dictamenRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Guarda el dictamen que se acaba de emitir y le asigna folio.
     *
     * <p>Se invoca desde la generacion del PDF, que es lo que hace posible el
     * historial y la reimpresion. El registro no debe impedir la entrega del
     * documento —el tecnico lo necesita aunque el guardado falle—, asi que las
     * excepciones suben y es el llamador quien decide como tratarlas.</p>
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
     * Genera el folio del ano en curso con el formato {@code DIC-<ano>-<n>},
     * mismo criterio que el taller.
     *
     * <p>El consecutivo sale del maximo ya emitido ese ano, de modo que la
     * numeracion arranca de nuevo en cada ejercicio y no depende del total de
     * filas. La restriccion UNIQUE de la columna es la garantia final: si dos
     * tecnicos emiten a la vez y leen el mismo maximo, la base rechaza el
     * segundo folio en lugar de admitirlo repetido.</p>
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
