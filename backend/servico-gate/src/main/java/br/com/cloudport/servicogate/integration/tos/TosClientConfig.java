package br.com.cloudport.servicogate.integration.tos;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(TosProperties.class)
public class TosClientConfig {

    @Bean
    public WebClient tosWebClient(TosProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.getApi().getTimeout())
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getApi().getTimeout().toMillis())
                .doOnConnected(conn -> conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(
                        properties.getApi().getTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(
                                properties.getApi().getTimeout().toMillis(), TimeUnit.MILLISECONDS)));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getApi().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (StringUtils.hasText(properties.getApi().getUsername())) {
            builder.defaultHeaders(headers -> headers.setBasicAuth(
                    properties.getApi().getUsername(),
                    properties.getApi().getPassword()));
        }

        return builder.build();
    }

    @Bean
    public Retry tosRetry(TosProperties properties) {
        TosProperties.RetryProperties retryProperties = properties.getRetry();
        IntervalFunction interval = IntervalFunction.ofExponentialBackoff(
                retryProperties.getInitialInterval().toMillis(),
                retryProperties.getMultiplier());

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryProperties.getMaxAttempts())
                .intervalFunction(interval)
                .retryExceptions(Exception.class)
                .build();
        return Retry.of("tosClient", config);
    }

    @Bean
    public CircuitBreaker tosCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("tosApi");
    }

    @Bean(name = "tosCacheManager")
    public CacheManager cacheManager(TosProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                TosCacheNames.BOOKING,
                TosCacheNames.CONTAINER_STATUS,
                TosCacheNames.CUSTOMS_RELEASE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(properties.getCache().getMaxSize())
                .expireAfterWrite(properties.getCache().getTtl()));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean(name = "cacheManager")
    @ConditionalOnExpression("'${spring.application.name:servico-gate}' != 'cloudport-runtime'")
    public CacheManager cacheManagerStandalone(
            @Qualifier("tosCacheManager") CacheManager tosCacheManager) {
        return tosCacheManager;
    }
}
