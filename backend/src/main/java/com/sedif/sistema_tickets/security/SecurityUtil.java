package com.sedif.sistema_tickets.security;

import com.sedif.sistema_tickets.util.enums.RolUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utilidades de solo lectura sobre el usuario autenticado en la peticion.
 *
 * <p>Centraliza el acceso al {@link SecurityContextHolder} para que los
 * servicios no lo consulten directamente y no se repita la comprobacion de
 * nulos por todo el codigo. Todos los metodos devuelven {@link Optional} vacio
 * o {@code false} en una peticion anonima, de forma que la ausencia de
 * autenticacion nunca provoca una excepcion.</p>
 *
 * <p>Los datos proceden del contexto que dejo {@link JwtAuthFilter}: el
 * identificador es el correo del usuario y la autoridad, un unico valor con
 * formato {@code ROLE_<ROL>}.</p>
 */
public final class SecurityUtil {

    /** Prefijo con el que Spring Security nombra las autoridades de rol. */
    private static final String PREFIJO_ROL = "ROLE_";

    private SecurityUtil() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Devuelve el identificador (correo) del usuario autenticado.
     *
     * @return el identificador, o vacio si la peticion es anonima.
     */
    public static Optional<String> obtenerIdentificadorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        // Spring marca como autenticado el token anonimo, cuyo principal es
        // "anonymousUser": hay que descartarlo aparte del caso nulo.
        if ("anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName()).filter(n -> !n.isBlank());
    }

    /**
     * Devuelve el rol del usuario autenticado, sin el prefijo {@code ROLE_}.
     * Por ejemplo {@code ADMINISTRADOR}.
     */
    public static Optional<String> obtenerRolActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return Optional.empty();
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .map(a -> a.startsWith(PREFIJO_ROL) ? a.substring(PREFIJO_ROL.length()) : a)
                .findFirst();
    }

    /** Indica si el usuario autenticado tiene el rol indicado (sin prefijo). */
    public static boolean tieneRol(String rol) {
        return obtenerRolActual()
                .map(actual -> actual.equalsIgnoreCase(rol))
                .orElse(false);
    }

    /** Indica si el usuario autenticado tiene el rol indicado. */
    public static boolean tieneRol(RolUsuario rol) {
        return tieneRol(rol.nombreEnBd());
    }

    /** Atajo para la comprobacion de rol mas frecuente del sistema. */
    public static boolean esAdministrador() {
        return tieneRol(RolUsuario.ADMINISTRADOR);
    }
}
