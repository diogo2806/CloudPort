package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class RedisEnvironmentPostProcessorTest {

    private final RedisEnvironmentPostProcessor postProcessor =
            new RedisEnvironmentPostProcessor();

    @Test
    void deveAplicarFallbackQuandoHostEPortaEstiveremVazios() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.redis.host", "   ")
                .withProperty("REDIS_HOST", "")
                .withProperty("spring.redis.port", " ")
                .withProperty("REDIS_PORT", "");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.redis.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("spring.redis.port")).isEqualTo("6379");
    }

    @Test
    void deveUsarVariaveisLegadasQuandoPropriedadesSpringEstiveremVazias() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.redis.host", "")
                .withProperty("REDIS_HOST", " redis-interno ")
                .withProperty("spring.redis.port", "")
                .withProperty("REDIS_PORT", " 6380 ");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.redis.host")).isEqualTo("redis-interno");
        assertThat(environment.getProperty("spring.redis.port")).isEqualTo("6380");
    }

    @Test
    void devePreservarPropriedadesSpringValidasRemovendoEspacos() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.redis.host", " redis-principal ")
                .withProperty("REDIS_HOST", "redis-legado")
                .withProperty("spring.redis.port", " 6381 ")
                .withProperty("REDIS_PORT", "6380");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.redis.host")).isEqualTo("redis-principal");
        assertThat(environment.getProperty("spring.redis.port")).isEqualTo("6381");
    }
}
