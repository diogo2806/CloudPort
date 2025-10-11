package br.com.cloudport.servicogate.integration.tos;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(TosProperties.class)
public class TosClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TosClientConfig.class);

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
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse());

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

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            LOGGER.info("event=tos.request method={} url={}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            LOGGER.info("event=tos.response status={} headers={}", clientResponse.statusCode(), clientResponse.headers().asHttpHeaders());
            return Mono.just(clientResponse);
        });
    }
}
