package br.com.expresscart.config;

import br.com.expresscart.controller.response.ProductResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Cache do catálogo de produtos no Redis. Os valores são gravados em JSON, cada cache com o seu
 * tipo e o seu tempo de vida (expresscart.cache.* no application.yml).
 * <p>
 * Ao mudar o tipo ou o formato do valor de um cache, mude também o sufixo de versão do nome:
 * assim as entradas gravadas pela versão anterior nunca são lidas e expiram sozinhas.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCT_CACHE = "catalog-product-v2";
    public static final String PRODUCT_LIST_CACHE = "catalog-products-v2";

    @Bean
    public RedisCacheManagerBuilderCustomizer catalogCaches(
            ObjectMapper objectMapper,
            @Value("${expresscart.cache.product-ttl}") Duration productTtl,
            @Value("${expresscart.cache.product-list-ttl}") Duration productListTtl
    ) {
        JavaType productList = objectMapper.getTypeFactory().constructCollectionType(List.class, ProductResponse.class);

        return builder -> builder
                .withCacheConfiguration(PRODUCT_CACHE, jsonCache(
                        new Jackson2JsonRedisSerializer<>(objectMapper, ProductResponse.class), productTtl))
                .withCacheConfiguration(PRODUCT_LIST_CACHE, jsonCache(
                        new Jackson2JsonRedisSerializer<>(objectMapper, productList), productListTtl));
    }

    private static RedisCacheConfiguration jsonCache(RedisSerializer<?> serializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));
    }
}
