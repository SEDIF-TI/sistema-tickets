package com.sedif.sistema_tickets.core.auth;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Clave secreta estática (En producción esto debería ir en el application.properties)
    // Es una llave de 256-bits codificada en Base64. NUNCA la comparta.
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    public String generarToken(Usuario usuario) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", usuario.getRol().getNombre());
        extraClaims.put("nombre", usuario.getNombre());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(usuario.getCorreo())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8)) // Expira en 8 horas
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // MÉTODOS PARA LEER Y VALIDAR EL TOKEN ---

    public String extraerIdentificador(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null; // Si el token es inválido, expirado o alterado, retorna null
        }
    }

    public boolean isTokenValido(String token) {
        return extraerIdentificador(token) != null;
    }

}