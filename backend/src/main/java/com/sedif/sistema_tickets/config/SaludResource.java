package com.sedif.sistema_tickets.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprobacion de que el servidor responde.
 *
 * <p>Lo consume el sondeo de {@code useNetworkStatus} del frontend, que
 * necesita distinguir "no hay internet" de "el servidor no contesta".</p>
 *
 * <p>Antes ese sondeo apuntaba a {@code /api} a secas, que no corresponde a
 * ningun controlador: Spring Security lo rechazaba con 403 <b>antes</b> de que
 * el filtro de CORS anadiera sus cabeceras, de modo que el navegador lo
 * reportaba como un error de CORS. La consola se llenaba de
 * "blocked by CORS policy" cada treinta segundos y la aplicacion mostraba
 * "No hay comunicacion con el servidor" aunque el backend estuviera
 * perfectamente en pie.</p>
 *
 * <p>Responde a GET y a HEAD, y no toca la base de datos: solo confirma que el
 * proceso acepta peticiones. Es publico a proposito —lo llama tambien la
 * pantalla de acceso, antes de que exista sesion— y no revela nada: ni version,
 * ni estado de dependencias, ni datos.</p>
 */
@RestController
@RequestMapping("/api/salud")
public class SaludResource {

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<Void> comprobar() {
        // Cuerpo vacio: al sondeo le basta el codigo de respuesta, y asi la
        // comprobacion es lo mas barata posible aunque corra cada 30 segundos.
        return ResponseEntity.noContent().build();
    }
}
