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

    @Test
    void deveConverterUrlComUnderscoreNoHostParaCamposDiscretos() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUrl("redis://pessoal_redis-guardiao:6379");

        RedisPropertiesConfiguracaoRuntime.normalizar(redisProperties);

        assertThat(redisProperties.getUrl()).isNull();
        assertThat(redisProperties.getHost()).isEqualTo("pessoal_redis-guardiao");
        assertThat(redisProperties.getPort()).isEqualTo(6379);
        assertThat(redisProperties.isSsl()).isFalse();
    }

    @Test
    void deveConverterUrlComCredenciaisESslQuandoHostTiverUnderscore() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUrl("rediss://usuario:senha@cache_redis:6380");

        RedisPropertiesConfiguracaoRuntime.normalizar(redisProperties);

        assertThat(redisProperties.getUrl()).isNull();
        assertThat(redisProperties.getHost()).isEqualTo("cache_redis");
        assertThat(redisProperties.getPort()).isEqualTo(6380);
        assertThat(redisProperties.getUsername()).isEqualTo("usuario");
        assertThat(redisProperties.getPassword()).isEqualTo("senha");
        assertThat(redisProperties.isSsl()).isTrue();
    }

    @Test
    void deveConverterUrlSomenteComSenhaQuandoHostTiverUnderscore() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUrl("redis://:senha@cache_redis");

        RedisPropertiesConfiguracaoRuntime.normalizar(redisProperties);

        assertThat(redisProperties.getUrl()).isNull();
        assertThat(redisProperties.getHost()).isEqualTo("cache_redis");
        assertThat(redisProperties.getPort()).isEqualTo(6379);
        assertThat(redisProperties.getUsername()).isNull();
        assertThat(redisProperties.getPassword()).isEqualTo("senha");
    }

    @Test
    void devePreservarUrlValidaQuandoHostForInterpretadoPelaUri() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUrl(" redis://redis-interno:6380 ");

        RedisPropertiesConfiguracaoRuntime.normalizar(redisProperties);

        assertThat(redisProperties.getUrl()).isEqualTo("redis://redis-interno:6380");
    }

    @Test
    void deveDescartarUrlEmBrancoOuInvalida() {
        RedisProperties urlEmBranco = new RedisProperties();
        urlEmBranco.setUrl("   ");
        RedisPropertiesConfiguracaoRuntime.normalizar(urlEmBranco);
        assertThat(urlEmBranco.getUrl()).isNull();
        assertThat(urlEmBranco.getHost()).isEqualTo("localhost");

        RedisProperties urlInvalida = new RedisProperties();
        urlInvalida.setHost("redis-interno");
        urlInvalida.setUrl("redis://:6379");
        RedisPropertiesConfiguracaoRuntime.normalizar(urlInvalida);
        assertThat(urlInvalida.getUrl()).isNull();
        assertThat(urlInvalida.getHost()).isEqualTo("redis-interno");
        assertThat(urlInvalida.getPort()).isEqualTo(6379);
    }
}
