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
 * <p>Los tokens se firman con HMAC a partir de {@code app.jwt.secret}, y su
 * vigencia la fija {@code app.jwt.expiration} (una hora por defecto). Ambas
 * propiedades se resuelven en el constructor, de modo que la clave se deriva
 * una sola vez y se reutiliza en cada firma y verificacion.
 * {@link JwtSecretValidator} comprueba la solidez de esa clave durante el
 * arranque.</p>
 *
 * <p>La validacion es autocontenida: no hay almacen de sesiones ni lista de
 * tokens emitidos, asi que un token vale hasta que expira.</p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Claim con el nombre del rol, tal como figura en la tabla {@code rol}. */
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
     * <p>El subject es el correo, que es el identificador con el que el filtro
     * localiza despues la cuenta. Los claims llevan solo lo que el frontend
     * necesita para pintar la interfaz (rol, nombre y si la contrasena es
     * temporal), nunca la contrasena ni su hash: el contenido de un JWT viaja
     * firmado, no cifrado, y cualquiera puede leerlo.</p>
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
     * Verifica firma y vigencia del token y devuelve sus claims.
     *
     * <p>Cada motivo de rechazo se registra por separado y con distinto nivel:
     * una sesion caducada es un hecho rutinario, mientras que una firma que no
     * cuadra con la clave del servidor apunta a un intento de falsificacion y
     * merece atencion en los logs.</p>
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
            // Caso rutinario: la sesion caduco. Nivel DEBUG para no llenar los logs.
            log.debug("Token expirado.");
        } catch (SignatureException e) {
            // Firma que no cuadra con la clave del servidor: token ajeno o alterado.
            log.warn("Token con firma invalida: posible intento de falsificacion.");
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Token mal formado o de un tipo no soportado.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token rechazado durante la validacion.");
        }
        return null;
    }

    /**
     * Deriva la clave HMAC del secreto configurado. Se intenta primero decodificar
     * como Base64, que es el formato recomendado por aportar mas entropia por
     * caracter; si el valor no lo es, se toman sus bytes UTF-8 en texto plano.
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
