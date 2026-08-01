package com.sedif.sistema_tickets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de arranque del contexto de Spring.
 *
 * <p>Desactivada porque levanta la aplicacion completa y necesita una base de
 * datos PostgreSQL accesible, ademas de las variables de entorno del archivo
 * .env. En una maquina sin base de datos falla por causas ajenas al codigo.</p>
 *
 * <p>Para ejecutarla: arranca la base de datos, exporta las variables y quita
 * la anotacion {@code @Disabled}.</p>
 *
 * <pre>
 *   docker compose -f docker-compose.yml -f docker-compose.local.yml up -d postgres-db
 *   set -a &amp;&amp; . ./.env &amp;&amp; set +a
 *   ./mvnw test
 * </pre>
 *
 * <p>Las pruebas que no requieren infraestructura (validacion del secreto JWT
 * y emision de tokens) viven en {@code security/} y se ejecutan siempre.</p>
 */
@SpringBootTest
@Disabled("Requiere PostgreSQL y las variables de entorno del .env")
class SistemaTicketsApplicationTests {

	@Test
	void contextLoads() {
	}

}
