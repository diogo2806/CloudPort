package br.com.cloudport.runtime.configuracao;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguracaoRuntime {

    @Bean(name = "cacheManager")
    public CacheManager cacheManager(
            @Qualifier("tosCacheManager") CacheManager tosCacheManager,
            @Qualifier("visibilidadeCacheManager") CacheManager visibilidadeCacheManager) {
        return new CompositeCacheManager(tosCacheManager, visibilidadeCacheManager);
    }
}
