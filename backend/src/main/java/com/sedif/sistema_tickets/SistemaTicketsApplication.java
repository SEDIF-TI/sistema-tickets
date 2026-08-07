package com.sedif.sistema_tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del sistema de tickets.
 *
 * <p>{@code @SpringBootApplication} activa la autoconfiguracion y el barrido de
 * componentes a partir de este paquete, de modo que todo lo que cuelga de
 * {@code com.sedif.sistema_tickets} queda dentro del alcance: controladores,
 * servicios, repositorios y las clases de configuracion que registran la cadena
 * de seguridad, el canal WebSocket, la cache y la auditoria.</p>
 */
@SpringBootApplication
public class SistemaTicketsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaTicketsApplication.class, args);
	}

}
