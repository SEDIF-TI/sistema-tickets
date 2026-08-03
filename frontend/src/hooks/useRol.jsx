import { useContext, useMemo } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';

/**
 * Rol del usuario de la sesión, normalizado.
 *
 * El backend envía el rol en `user.rol` como texto plano (ver `JwtResponse`),
 * pero cada pantalla lo deducía a su manera, encadenando alternativas que no
 * existen: `user?.role`, `user?.rolNombre`, `user?.rol === 'ADMIN'`… Ninguna de
 * esas variantes se ha enviado nunca, así que solo añadían ruido y ocultaban
 * el hecho de que la comprobación real es una sola.
 *
 * Devuelve además los indicadores por rol, para no repetir la comparación de
 * cadenas en cada componente.
 */
export function useRol() {
    const { user } = useContext(AuthContext);

    return useMemo(() => {
        // Se tolera el prefijo ROLE_ de Spring Security por si algún endpoint
        // lo devuelve con él.
        const rol = (user?.rol || '').replace('ROLE_', '').trim().toUpperCase();

        return {
            rol,
            esAdministrador: rol === 'ADMINISTRADOR',
            esSoporte: rol === 'SOPORTE',
            esEmpleado: rol === 'EMPLEADO',
            /** Puede atender tickets: soporte y administración. */
            esTecnico: rol === 'SOPORTE' || rol === 'ADMINISTRADOR',
            usuarioId: user?.usuarioId ?? null,
            areaId: user?.areaId ?? null,
            nombre: user?.nombre ?? '',
        };
    }, [user]);
}

export default useRol;
