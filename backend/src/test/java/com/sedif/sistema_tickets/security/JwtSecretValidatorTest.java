package com.sedif.sistema_tickets.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que el validador rechaza las claves inseguras al arranque.
 *
 * <p>Es la salvaguarda que habria evitado que el proyecto llegara a
 * produccion con la clave de ejemplo incrustada en el codigo, asi que
 * conviene tenerla cubierta.</p>
 */
class JwtSecretValidatorTest {

    /** Clave valida de 256 bits generada aleatoriamente. */
    private static final String CLAVE_VALIDA =
            Base64.getEncoder().encodeToString(
                    "9f3Kx7Qw2ZpL5vNb8RtYuE1sA4dG6hJ0".getBytes());

    @Test
    @DisplayName("Acepta una clave de 256 bits con suficiente entropia")
    void aceptaClaveValida() {
        JwtSecretValidator validator = new JwtSecretValidator(CLAVE_VALIDA);
        assertDoesNotThrow(validator::validar);
    }

    @Test
    @DisplayName("Rechaza la clave de ejemplo que estaba hardcodeada en el proyecto")
    void rechazaClaveDeEjemploConocida() {
        JwtSecretValidator validator = new JwtSecretValidator(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validar);
        assertTrue(ex.getMessage().contains("EJEMPLO"),
                "El mensaje debe advertir de que es una clave de ejemplo publica.");
    }

    @Test
    @DisplayName("Rechaza una clave demasiado corta para HMAC-SHA256")
    void rechazaClaveCorta() {
        JwtSecretValidator validator = new JwtSecretValidator("clave123");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validar);
        assertTrue(ex.getMessage().contains("256 bits"),
                "El mensaje debe indicar el minimo exigido por el algoritmo.");
    }

    @Test
    @DisplayName("Rechaza una clave larga pero repetitiva, sin entropia real")
    void rechazaClaveSinEntropia() {
        // 64 caracteres, longitud de sobra, pero un solo simbolo distinto.
        JwtSecretValidator validator = new JwtSecretValidator("a".repeat(64));

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validar);
        assertTrue(ex.getMessage().contains("caracteres distintos"));
    }

    @Test
    @DisplayName("Rechaza una clave vacia o sin definir")
    void rechazaClaveVacia() {
        assertThrows(IllegalStateException.class,
                () -> new JwtSecretValidator("").validar());
        assertThrows(IllegalStateException.class,
                () -> new JwtSecretValidator("   ").validar());
        assertThrows(IllegalStateException.class,
                () -> new JwtSecretValidator(null).validar());
    }

    @Test
    @DisplayName("Rechaza el valor de ejemplo que trae el .env.example")
    void rechazaPlaceholderDelEnvExample() {
        JwtSecretValidator validator =
                new JwtSecretValidator("GENERA_LA_TUYA_CON_openssl_rand_base64_32");

        assertThrows(IllegalStateException.class, validator::validar);
    }
}
