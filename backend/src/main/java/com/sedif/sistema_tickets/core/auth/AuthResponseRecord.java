package com.sedif.sistema_tickets.core.auth;

import com.sedif.sistema_tickets.core.usuarios.VistaDTO;
import java.util.List;

public record AuthResponseRecord(
        Long usuarioId,
        String nombre,
        String rol,
        String token,
        String mensaje,
        List<VistaDTO> vistas, // <-- Nueva propiedad para enviar el menú
        Long areaId
) {}