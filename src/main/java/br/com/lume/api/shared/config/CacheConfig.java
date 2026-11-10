package br.com.lume.api.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String CACHE_TIPOS_VIOLENCIA = "catalogoTiposViolencia";
    public static final String CACHE_MARCADORES_VULNERABILIDADE = "catalogoMarcadores";
    public static final String CACHE_CATEGORIAS_ESTABELECIMENTOS = "catalogoCategorias";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_TIPOS_VIOLENCIA,
                CACHE_MARCADORES_VULNERABILIDADE,
                CACHE_CATEGORIAS_ESTABELECIMENTOS
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(1)));
        return cacheManager;
    }
}