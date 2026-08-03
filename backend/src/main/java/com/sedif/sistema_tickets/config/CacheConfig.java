package com.sedif.sistema_tickets.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache en memoria para los catalogos que casi nunca cambian.
 *
 * <p>Roles, areas, estatus y vistas se consultan en practicamente cada
 * peticion (el filtro JWT resuelve el rol, el menu lee las vistas) pero se
 * modifican muy de vez en cuando. Cachearlos evita ir a la base de datos una
 * y otra vez por los mismos registros.</p>
 *
 * <p>Se usa {@link ConcurrentMapCacheManager} por ser suficiente para un
 * despliegue de una sola instancia y no anadir dependencias. Si en el futuro
 * la aplicacion se replica en varias instancias, conviene sustituirlo por una
 * cache distribuida (Redis) para que todas compartan la misma copia.</p>
 *
 * <p><b>Nota:</b> las entradas se invalidan con {@code @CacheEvict} en los
 * servicios que modifican estos catalogos. Ningun dato de ticket ni de usuario
 * se cachea, para no servir informacion obsoleta ni retener datos personales
 * en memoria mas alla de la peticion.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ROLES = "roles";
    public static final String CACHE_AREAS = "areas";
    public static final String CACHE_VISTAS = "vistas";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_ROLES,
                CACHE_AREAS,
                CACHE_VISTAS
        );
    }
}
