package br.com.cloudport.runtime.configuracao;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public final class RedisEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "cloudportRedisNormalizacao";

    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final String DEFAULT_REDIS_PORT = "6379";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        Map<String, Object> propriedadesNormalizadas = new LinkedHashMap<>();
        propriedadesNormalizadas.put(
                "spring.redis.host",
                normalizar(
                        environment.getProperty("spring.redis.host"),
                        environment.getProperty("REDIS_HOST"),
                        DEFAULT_REDIS_HOST));
        propriedadesNormalizadas.put(
                "spring.redis.port",
                normalizar(
                        environment.getProperty("spring.redis.port"),
                        environment.getProperty("REDIS_PORT"),
                        DEFAULT_REDIS_PORT));

        environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, propriedadesNormalizadas));
    }

    private static String normalizar(
            String valorSpring,
            String valorLegado,
            String valorPadrao) {
        if (StringUtils.hasText(valorSpring)) {
            return valorSpring.trim();
        }
        if (StringUtils.hasText(valorLegado)) {
            return valorLegado.trim();
        }
        return valorPadrao;
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
