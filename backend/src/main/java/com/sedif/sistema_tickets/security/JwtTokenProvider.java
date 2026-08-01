package com.sedif.sistema_tickets.security;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Emite y valida los JSON Web Tokens de la aplicacion.
 *
 * <p>Sustituye a la antigua clase {@code core.auth.JwtService}, que tenia la
 * clave de firma incrustada en el codigo. Aqui la clave llega desde
 * {@code app.jwt.secret} (variable de entorno) y {@link JwtSecretValidator}
 * garantiza su solidez antes de que la aplicacion acepte peticiones.</p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Claim con el rol del usuario; lo consume el filtro para autorizar. */
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_NOMBRE = "nombre";
    public static final String CLAIM_PASSWORD_TEMPORAL = "passwordTemporal";

    private final SecretKey claveFirma;
    private final long vigenciaMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration:3600000}") long vigenciaMs) {
        this.claveFirma = construirClave(secret);
        this.vigenciaMs = vigenciaMs;
    }

    /**
     * Genera el access token del usuario.
     *
     * <p>Solo se incluyen los datos que el frontend necesita para pintar la
     * interfaz. Nunca se incluye la contrasena ni su hash.</p>
     */
    public String generarToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROL, usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        claims.put(CLAIM_NOMBRE, usuario.getNombre());
        claims.put(CLAIM_PASSWORD_TEMPORAL,
                usuario.getPasswordTemporal() != null && usuario.getPasswordTemporal());

        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + vigenciaMs);

        return Jwts.builder()
                .claims(claims)
                .subject(usuario.getCorreo())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(claveFirma)
                .compact();
    }

    /**
     * Extrae el identificador (subject) del token, verificando antes la firma
     * y la expiracion.
     *
     * @return el identificador, o {@code null} si el token no es valido.
     */
    public String extraerIdentificador(String token) {
        Claims claims = parsear(token);
        return claims != null ? claims.getSubject() : null;
    }

    /** Indica si el token esta correctamente firmado y no ha expirado. */
    public boolean isTokenValido(String token) {
        return parsear(token) != null;
    }

    /**
     * Verifica la firma y devuelve los claims.
     *
     * <p>A diferencia de la implementacion anterior, cada motivo de fallo se
     * registra por separado. Un token con firma invalida indica un intento de
     * falsificacion y no debe confundirse con una sesion simplemente
     * caducada.</p>
     *
     * @return los claims, o {@code null} si el token no supera la validacion.
     */
    private Claims parsear(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(claveFirma)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            // Caso normal: la sesion caduco. Nivel DEBUG para no llenar los logs.
            log.debug("Token expirado.");
        } catch (SignatureException e) {
            // Firma que no cuadra con nuestra clave: posible intento de falsificacion.
            log.warn("Token con firma invalida: posible intento de falsificacion.");
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Token mal formado o de un tipo no soportado.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token rechazado durante la validacion.");
        }
        return null;
    }

    /**
     * Construye la clave HMAC. Acepta la clave en Base64 (lo recomendado) y,
     * como alternativa, texto plano suficientemente largo.
     */
    private SecretKey construirClave(String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException noEsBase64) {
            bytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
