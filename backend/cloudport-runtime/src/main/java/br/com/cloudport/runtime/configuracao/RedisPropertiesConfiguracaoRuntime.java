package br.com.cloudport.runtime.configuracao;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedisPropertiesConfiguracaoRuntime {

    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final int DEFAULT_REDIS_PORT = 6379;

    @Bean
    public static BeanPostProcessor redisPropertiesNormalizador() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RedisProperties) {
                    normalizar((RedisProperties) bean);
                }
                return bean;
            }
        };
    }

    static void normalizar(RedisProperties redisProperties) {
        String host = redisProperties.getHost();
        redisProperties.setHost(
                StringUtils.hasText(host) ? host.trim() : DEFAULT_REDIS_HOST);

        if (redisProperties.getPort() <= 0) {
            redisProperties.setPort(DEFAULT_REDIS_PORT);
        }
    }
}
