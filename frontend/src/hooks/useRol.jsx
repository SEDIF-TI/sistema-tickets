import { useContext, useMemo } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';

/**
 * Rol del usuario de la sesión, normalizado.
 *
 * El backend lo envía en `user.rol` como texto plano (ver `JwtResponse`). Este
 * hook es el único punto donde se lee ese campo, de modo que la comprobación
 * vive en un solo sitio.
 *
 * Devuelve además los indicadores por rol —`esAdministrador`, `esSoporte`,
 * `esEmpleado`—, para no repetir comparaciones de cadenas en cada componente.
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
