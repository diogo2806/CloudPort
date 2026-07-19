package br.com.cloudport.runtime.configuracao;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public final class RedisEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String PROPERTY_SOURCE_NAME = "cloudportRedisNormalizacao";

    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final String DEFAULT_REDIS_PORT = "6379";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
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
}
