package com.sedif.sistema_tickets.security;

import com.sedif.sistema_tickets.core.usuarios.Rol;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la emision y validacion de los JWT.
 *
 * <p>El caso mas importante es {@code rechazaTokenFirmadoConOtraClave}: si un
 * token firmado con otra clave se aceptara, cualquiera podria fabricarse uno
 * con rol de administrador.</p>
 */
class JwtTokenProviderTest {

    private static final String CLAVE = Base64.getEncoder()
            .encodeToString("9f3Kx7Qw2ZpL5vNb8RtYuE1sA4dG6hJ0".getBytes());

    private static final String OTRA_CLAVE = Base64.getEncoder()
            .encodeToString("0Jh6Gd4As1Eu YtR8bNv5Lp Z2wQ7xK3f9".replace(" ", "").getBytes());

    private JwtTokenProvider tokenProvider;
    private Usuario usuario;

    @BeforeEach
    void prepararEscenario() {
        // 1 hora de vigencia, como en produccion.
        tokenProvider = new JwtTokenProvider(CLAVE, 3_600_000L);

        Rol rol = new Rol();
        rol.setNombre("SOPORTE");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Persona de prueba");
        usuario.setCorreo("prueba@sedif.gob.mx");
        usuario.setRol(rol);
        usuario.setPasswordTemporal(false);
    }

    @Test
    @DisplayName("Genera un token valido del que se puede recuperar el identificador")
    void generaYValidaToken() {
        String token = tokenProvider.generarToken(usuario);

        assertNotNull(token);
        assertTrue(tokenProvider.isTokenValido(token));
        assertEquals("prueba@sedif.gob.mx", tokenProvider.extraerIdentificador(token));
    }

    @Test
    @DisplayName("Rechaza un token firmado con una clave distinta")
    void rechazaTokenFirmadoConOtraClave() {
        // Un atacante firma su propio token con una clave que no es la nuestra.
        JwtTokenProvider atacante = new JwtTokenProvider(OTRA_CLAVE, 3_600_000L);
        String tokenFalsificado = atacante.generarToken(usuario);

        assertFalse(tokenProvider.isTokenValido(tokenFalsificado));
        assertNull(tokenProvider.extraerIdentificador(tokenFalsificado));
    }

    @Test
    @DisplayName("Rechaza un token ya expirado")
    void rechazaTokenExpirado() {
        // Vigencia negativa: nace caducado.
        JwtTokenProvider caducado = new JwtTokenProvider(CLAVE, -1000L);
        String token = caducado.generarToken(usuario);

        assertFalse(tokenProvider.isTokenValido(token));
    }

    @Test
    @DisplayName("Rechaza cadenas que no son un token")
    void rechazaTokenMalFormado() {
        assertFalse(tokenProvider.isTokenValido("esto-no-es-un-jwt"));
        assertFalse(tokenProvider.isTokenValido(""));
        assertFalse(tokenProvider.isTokenValido("a.b.c"));
    }

    @Test
    @DisplayName("Rechaza un token manipulado en su carga util")
    void rechazaTokenAlterado() {
        String token = tokenProvider.generarToken(usuario);

        // Se altera un caracter del payload: la firma deja de cuadrar.
        String[] partes = token.split("\\.");
        char primero = partes[1].charAt(0);
        partes[1] = (primero == 'A' ? 'B' : 'A') + partes[1].substring(1);
        String alterado = String.join(".", partes);

        assertFalse(tokenProvider.isTokenValido(alterado));
    }

    @Test
    @DisplayName("El token no contiene la contrasena del usuario")
    void tokenNoFiltraLaPassword() {
        usuario.setPassword("$2a$10$hashBcryptDePrueba");
        String token = tokenProvider.generarToken(usuario);

        // El payload va en Base64 sin cifrar: cualquiera puede leerlo.
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));

        assertFalse(payload.contains("$2a$"), "El token nunca debe incluir el hash de la contrasena.");
        assertFalse(payload.toLowerCase().contains("password\":\"" ),
                "El token no debe incluir un campo password con valor.");
    }
}
