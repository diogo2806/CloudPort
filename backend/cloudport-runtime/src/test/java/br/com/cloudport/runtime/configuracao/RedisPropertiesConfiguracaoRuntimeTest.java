package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

class RedisPropertiesConfiguracaoRuntimeTest {

    @Test
    void deveAplicarFallbackQuandoHostEPortaForemInvalidos() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setHost("   ");
        redisProperties.setPort(0);

        RedisPropertiesConfiguracaoRuntime.normalizar(redisProperties);

        assertThat(redisProperties.getHost()).isEqualTo("localhost");
        assertThat(redisProperties.getPort()).isEqualTo(6379);
    }

    @Test
    void devePreservarConfiguracaoValidaRemovendoEspacosDoHost() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setHost(" redis-interno ");
        redisProperties.setPort(6380);

        RedisPropertiesConfiguracaoRuntime.normalizar(redisProperties);

        assertThat(redisProperties.getHost()).isEqualTo("redis-interno");
        assertThat(redisProperties.getPort()).isEqualTo(6380);
    }
}
